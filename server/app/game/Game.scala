package game

import akka.actor.PoisonPill
import shared.Pieces._
import game.PiecesWithPosition.{GamePiece, NextPiece}
import play.api.libs.json.Json.JsValueWrapper
import shared.GameRules.nGameCols
import play.api.libs.json.{JsBoolean, JsObject, Json}
import shared.Actions._
import shared.{GameAPIKeys, GameRules}

import scala.concurrent.duration._


class Game(p1: Player, p2: Player) {
  private val player1: PlayerWithState = PlayerWithState(p1)
  private val player2: PlayerWithState = PlayerWithState(p2)

  private val players: Map[String, PlayerWithState] = Map(
    player1.id -> player1,
    player2.id -> player2
  )

  //Use the system's dispatcher as ExecutionContext
  private val system = akka.actor.ActorSystem("system")
  import system.dispatcher

  private def opponent(player: PlayerWithState): PlayerWithState = {
    if (player  == player1) player2
    else player1
  }

  def gameState(id: String): GameState = {
    players(id).state
  }

  def movePiece(id: String, action: Action): Unit = this.synchronized {
    val player = players(id)
    val gs = player.state

    if (!gs.ready) {
      return
    }

    val moved: Boolean = action match {
      case Left => gs.curPiece.moveLeft()
      case Right => gs.curPiece.moveRight()
      case Fall => gs.curPiece.fall()
      case Rotate => gs.curPiece.rotate()
    }

    if (moved) {
      if (action == Fall) handlePieceBottom(player)
      else broadcastPiecePositions(player)
    }
  }

  def setReady(id: String): Unit = {
    val player = players(id)
    player.state.ready = true
    println(id + " is ready")
    broadcast(player, Json.obj(GameAPIKeys.ready -> true))

    if (opponent(player).state.ready) {
      initGame()
    }
  }

  def initGame(): Unit = {
    broadcast(player1, Json.obj(
      GameAPIKeys.gameGrid -> player1.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> player1.state.nextPieceGrid,
      piecePositionsToKeyVal(player1)
    ))
    broadcast(player2, Json.obj(
      GameAPIKeys.gameGrid -> player2.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> player2.state.nextPieceGrid,
      piecePositionsToKeyVal(player2)
    ))

    gameTick(player1)
    gameTick(player2)
  }

  private def handlePieceBottom(player: PlayerWithState): Unit = {
    val opp = opponent(player)
    val state = player.state
    val nBlocksAbove = state.curPiece.getPositions.minBy(_._1)._1

    if (nBlocksAbove <= 1) {
      lose(player)
    }

    val removed = removeCompletedLines(player.state)

    val linesBeforeCompleted: Array[Array[Boolean]] = removed.map(p => {
      val row: Array[Boolean] = p._1
      val i = p._2
      row.indices.map(col => !state.curPiece.getPositions.contains((i, col))).toArray
    })

    state.piecesPlaced += 1
    state.points += GameRules.pointsForPieceDown(nBlocksAbove, linesBeforeCompleted.length, state.gameSpeed)

    val oppLost = sendLinesToOpponent(linesBeforeCompleted, opp)
    if (oppLost) {
      lose(opp)
    }

    state.curPiece = new GamePiece(state.nextPiece.piece, state.gameGrid)
    state.curPiece.addToGrid()

    state.nextPiece.removeFromGrid()
    state.nextPiece = new NextPiece(randomPiece(), state.nextPieceGrid)
    state.nextPiece.addToGrid()

    state.gameSpeed = GameRules.nextSpeed(state.gameSpeed)

    broadcastPiecePositions(player)

    broadcast(player, Json.obj(
      GameAPIKeys.gameGrid -> player.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> player.state.nextPieceGrid,
      GameAPIKeys.piecesPlaced -> state.piecesPlaced,
      GameAPIKeys.points -> state.points
    ))

    broadcastPiecePositions(opp)
    broadcast(opp, Json.obj(GameAPIKeys.gameGrid -> opp.state.gameGrid))
  }

  private def gameTick(player: PlayerWithState): Unit = this.synchronized {
    system.scheduler.scheduleOnce(player.state.gameSpeed.milliseconds) {
      if (!player.state.curPiece.moveDown()) handlePieceBottom(player)
      else broadcastPiecePositions(player)

      gameTick(player)
    }
  }

  def broadcast(player: PlayerWithState, jsonObj: JsObject): Unit = {
    player.out ! Json.toJson(jsonObj + (GameAPIKeys.opponent -> JsBoolean(false))).toString()
    opponent(player).out ! Json.toJson(jsonObj + (GameAPIKeys.opponent -> JsBoolean(true))).toString()
  }

  def broadcastPiecePositions(player: PlayerWithState): Unit = {
    broadcast(player, Json.obj(piecePositionsToKeyVal(player)))
  }

  def piecePositionsToKeyVal(player: PlayerWithState): (String, JsValueWrapper) = {
    GameAPIKeys.piecePositions -> player.state.curPiece.getPositions.map(p => Array(p._1, p._2))
  }


  def lose(player: PlayerWithState): Unit = {
    player.out ! Json.obj(GameAPIKeys.won -> JsBoolean(false)).toString()
    opponent(player).out ! Json.obj(GameAPIKeys.won -> JsBoolean(true)).toString()

    player1.out ! PoisonPill
    player2.out ! PoisonPill
  }

  def removeCompletedLines(state: GameState): Array[(Array[Boolean], Int)] = {
    val (removed, kept) = state.gameGrid
      .zipWithIndex
      .partition(p => p._1.count(x => x) == nGameCols)

    println("kept")
    kept.foreach(x => {
      x._1.foreach(y => {
        print(if (y) "X" else ".")
        print(" ")
      })
      println()
    })

    println("removed")
    removed.foreach(x => {
      x._1.foreach(y => {
        print(if (y) "X" else ".")
        print(" ")
      })
      println()
    })

    val newValues = Array.ofDim[Boolean](removed.length, nGameCols) ++ kept.map(_._1)

    println("newValues")
    newValues.foreach(x => {
      x.foreach(y => {
        print(if (y) "X" else ".")
        print(" ")
      })
      println()
    })

    state.updateGameGrid(newValues)

    println("newValues")
    newValues.foreach(x => {
      x.foreach(y => {
        print(if (y) "X" else ".")
        print(" ")
      })
      println()
    })

    println("Updated grid")
    state.gameGrid.foreach(x => {
      x.foreach(y => {
        print(if (y) "X" else ".")
        print(" ")
      })
      println()
    })

    removed
  }

  def sendLinesToOpponent(lines: Array[Array[Boolean]], opp: PlayerWithState): Boolean = {
    val toSend = lines.length match {
      case 1 | 2 | 3 => lines.take(lines.length - 1)
      case _ => lines
    }

    if (toSend.nonEmpty) {
      val oppPiece = opp.state.curPiece

      opp.state.gameGrid.foreach(x => {
        x.foreach(y => {
          print(if (y) "X" else "."); print(" ")
        }); println()
      })

      oppPiece.removeFromGrid()
      opp.state.updateGameGrid(opp.state.gameGrid.drop(toSend.length) ++ toSend)

      while (oppPiece.wouldCollideIfAddedToGrid()) {
        if (!oppPiece.moveUp(updateGridOnMove = false)) {
          return true
        }
      }

      oppPiece.addToGrid()

      println("Opp grid at the end")
      opp.state.gameGrid.foreach(x => {
        x.foreach(y => {
          print(if (y) "X" else "."); print(" ")
        }); println()
      })
    }
    false
  }
}

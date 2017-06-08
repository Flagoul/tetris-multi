package game

import akka.actor.{ActorRef, PoisonPill}
import shared.Pieces._
import game.PiecesWithPosition.{GamePiece, NextPiece}
import models.Result
import play.api.libs.json.Json.JsValueWrapper
import shared.GameRules.nGameCols
import play.api.libs.json.{JsBoolean, JsObject, Json}
import shared.Actions._
import shared.{GameAPIKeys, GameRules}

import scala.concurrent.duration._


class Game(p1: Player, p2: Player, gameManager: GameManager) {
  private val player1: PlayerWithState = PlayerWithState(p1)
  private val player2: PlayerWithState = PlayerWithState(p2)

  private var gameFinished: Boolean = false
  private var gameBeganAt: Long = _

  private val players: Map[Long, PlayerWithState] = Map(
    player1.user.id.get -> player1,
    player2.user.id.get -> player2
  )

  //Use the system's dispatcher as ExecutionContext
  private val system = akka.actor.ActorSystem("system")
  import system.dispatcher

  def setReady(implicit id: Long): Unit = {
    val player = players(id)
    player.state.ready = true
    println(id + " is ready")
    broadcast(player, Json.obj(GameAPIKeys.ready -> true))

    if (opponent(player).state.ready) {
      initGame()
    }
  }

  def everyoneReady(): Boolean = player1.state.ready && player2.state.ready

  private def initGame(): Unit = {
    player1.state.curPiece.addToGrid()
    player2.state.curPiece.addToGrid()

    player1.state.nextPiece.addToGrid()
    player2.state.nextPiece.addToGrid()

    broadcastState(player1)
    broadcastState(player2)

    gameBeganAt = System.currentTimeMillis()

    gameTick(player1)
    gameTick(player2)
  }

  def movePiece(action: Action)(implicit id: Long): Unit = this.synchronized {
    if (!everyoneReady()) {
      return
    }

    val player = players(id)
    val gs = player.state

    val moved: Boolean = action match {
      case Left => gs.curPiece.moveLeft()
      case Right => gs.curPiece.moveRight()
      case Rotate => gs.curPiece.rotate()
      case Down => gs.curPiece.moveDown()
      case Fall => gs.curPiece.fall()
    }

    if (moved) {
      if (action == Fall) handlePieceBottom(player)
      else broadcastPiecePositions(player)
    }
  }

  def putBackPlayerInGame(out: ActorRef)(implicit id: Long): Unit = this.synchronized {
    val player = players(id)
    val opp = opponent(player)

    player.changeActorRef(out)

    player.out ! Json.obj(GameAPIKeys.opponentUsername -> opp.user.username).toString()
    sendState(player, player)
    sendState(player, opp)
  }

  private def opponent(player: PlayerWithState): PlayerWithState = {
    if (player == player1) player2
    else player1
  }

  private def buildState(player: PlayerWithState): JsObject = {
    Json.obj(
      GameAPIKeys.gameGrid -> player.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> player.state.nextPieceGrid,
      GameAPIKeys.piecesPlaced -> player.state.piecesPlaced,
      GameAPIKeys.points -> player.state.points,
      piecePositionsToKeyVal(player)
    )
  }

  private def broadcastState(player: PlayerWithState): Unit = {
    broadcast(player, buildState(player))
  }

  private def sendState(dest: PlayerWithState, playerWithStateToSend: PlayerWithState): Unit = {
    dest.out ! (
      buildState(playerWithStateToSend) + (GameAPIKeys.opponent -> JsBoolean(dest != playerWithStateToSend))
    ).toString()
  }

  private def handlePieceBottom(player: PlayerWithState): Unit = this.synchronized {
    val opp = opponent(player)
    val state = player.state
    val nBlocksAbove = state.curPiece.getPositions.minBy(_._1)._1

    if (nBlocksAbove <= 1) {
      lose(player)
    }

    val removed = removeCompletedLines(player.state)

    state.piecesPlaced += 1
    state.points += GameRules.pointsForPieceDown(nBlocksAbove, removed.length, state.gameSpeed)

    val newSpeed = GameRules.nextSpeed(state.gameSpeed, removed.length)
    state.gameSpeed = newSpeed
    opp.state.gameSpeed = newSpeed

    val oppLost = sendLinesToOpponent(removed, state, opp)
    if (oppLost) {
      lose(opp)
      return
    }

    generateNewPiece(player)
  }

  private def generateNewPiece(player: PlayerWithState): Unit = {
    val state = player.state
    state.curPiece = new GamePiece(state.nextPiece.piece, state.gameGrid)

    if (state.curPiece.wouldCollideIfAddedToGrid()) {
      lose(player)
      return
    }

    state.curPiece.addToGrid()

    state.nextPiece.removeFromGrid()
    state.nextPiece = new NextPiece(randomPiece(), state.nextPieceGrid)
    state.nextPiece.addToGrid()

    broadcastPiecePositions(player)

    broadcast(player, Json.obj(
      GameAPIKeys.gameGrid -> player.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> player.state.nextPieceGrid,
      GameAPIKeys.piecesPlaced -> state.piecesPlaced,
      GameAPIKeys.points -> state.points
    ))
  }

  private def gameTick(player: PlayerWithState): Unit = {
    if (gameFinished) {
      return
    }

    system.scheduler.scheduleOnce(player.state.gameSpeed.milliseconds) {
      if (!player.state.curPiece.moveDown()) handlePieceBottom(player)
      else broadcastPiecePositions(player)

      gameTick(player)
    }
  }

  private def broadcast(player: PlayerWithState, jsonObj: JsObject): Unit = {
    player.out ! (jsonObj + (GameAPIKeys.opponent -> JsBoolean(false))).toString()
    opponent(player).out ! (jsonObj + (GameAPIKeys.opponent -> JsBoolean(true))).toString()
  }

  private def broadcastPiecePositions(player: PlayerWithState): Unit = {
    broadcast(player, Json.obj(piecePositionsToKeyVal(player)))
  }

  private def piecePositionsToKeyVal(player: PlayerWithState): (String, JsValueWrapper) = {
    GameAPIKeys.piecePositions -> player.state.curPiece.getPositions.map(p => Array(p._1, p._2))
  }

  private def lose(loser: PlayerWithState): Unit = this.synchronized {
    if (!gameFinished) {
      gameFinished = true

      val winner = opponent(loser)

      loser.out ! Json.obj(GameAPIKeys.won -> JsBoolean(false)).toString()
      winner.out ! Json.obj(GameAPIKeys.won -> JsBoolean(true)).toString()

      loser.out ! PoisonPill
      winner.out ! PoisonPill

      // If the player loses by leaving the game when in the lobby, the game time should be 0.
      val timeSpent = if (everyoneReady()) (System.currentTimeMillis() - gameBeganAt) / 1000 else 0

      gameManager.endGame(Result(
        None,
        winner.user.id.get, winner.state.points, winner.state.piecesPlaced,
        loser.user.id.get, loser.state.points, loser.state.piecesPlaced,
        timeSpent, None
      ))
    }
  }

  def lose(implicit id: Long): Unit = {
    lose(players(id))
  }

  def removeCompletedLines(state: GameState): Array[(Array[Boolean], Int)] = {
    val (removed, kept) = state.gameGrid
      .zipWithIndex
      .partition(p => p._1.count(x => x) == nGameCols)

    val newValues = Array.ofDim[Boolean](removed.length, nGameCols) ++ kept.map(_._1.clone)
    state.updateGameGrid(newValues)

    removed
  }

  def sendLinesToOpponent(removed: Array[(Array[Boolean], Int)], state: GameState, opp: PlayerWithState): Boolean = {
    val linesBeforeCompleted: Array[Array[Boolean]] = removed.map(p => {
      val row: Array[Boolean] = p._1
      val i = p._2
      row.indices.map(col => !state.curPiece.getPositions.contains((i, col))).toArray
    })

    val toSend = linesBeforeCompleted.length match {
      case 1 | 2 | 3 => linesBeforeCompleted.take(linesBeforeCompleted.length - 1)
      case _ => linesBeforeCompleted
    }

    if (pushLinesToGrid(toSend, opp.state)) {
      return true
    }

    broadcastPiecePositions(opp)
    broadcast(opp, Json.obj(GameAPIKeys.gameGrid -> opp.state.gameGrid))
    false
  }

  def pushLinesToGrid(toPush: Array[Array[Boolean]], state: GameState): Boolean = {
    if (toPush.nonEmpty) {
      val piece = state.curPiece

      piece.removeFromGrid()

      state.updateGameGrid(state.gameGrid.drop(toPush.length).map(_.clone) ++ toPush)

      while (piece.wouldCollideIfAddedToGrid()) {
        if (!piece.moveUpWithOnlyGridCheck(updateGridOnMove = false)) {
          return true
        }
      }

      piece.addToGrid()
    }
    false
  }
}

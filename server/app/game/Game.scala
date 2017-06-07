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

  private def opponent(player: PlayerWithState): PlayerWithState = {
    if (player  == player1) player2
    else player1
  }

  def everyoneReady(): Boolean = player1.state.ready && player2.state.ready

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

  def setReady(implicit id: Long): Unit = {
    val player = players(id)
    player.state.ready = true
    println(id + " is ready")
    broadcast(player, Json.obj(GameAPIKeys.ready -> true))

    if (opponent(player).state.ready) {
      initGame()
    }
  }

  def initGame(): Unit = {
    broadcastState(player1)
    broadcastState(player2)

    gameBeganAt = System.currentTimeMillis()

    gameTick(player1)
    gameTick(player2)
  }

  def buildState(player: PlayerWithState): JsObject = {
    Json.obj(
      GameAPIKeys.gameGrid -> player.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> player.state.nextPieceGrid,
      GameAPIKeys.piecesPlaced -> player.state.piecesPlaced,
      GameAPIKeys.points -> player.state.points,
      piecePositionsToKeyVal(player)
    )
  }

  def broadcastState(player: PlayerWithState): Unit = {
    broadcast(player, buildState(player))
  }

  def sendState(dest: PlayerWithState, playerWithStateToSend: PlayerWithState): Unit = {
    dest.out ! (
      buildState(playerWithStateToSend) + (GameAPIKeys.opponent -> JsBoolean(dest != playerWithStateToSend))
    ).toString()
  }

  def putBackPlayerInGame(out: ActorRef)(implicit id: Long): Unit = this.synchronized {
    val player = players(id)
    val opp = opponent(player)

    player.changeActorRef(out)

    player.out ! Json.obj(GameAPIKeys.opponentUsername -> opp.user.username).toString()
    sendState(player, player)
    sendState(player, opp)
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
    }

    generateNewPiece(player)
  }

  private def generateNewPiece(player: PlayerWithState) = {
    val state = player.state
    state.curPiece = new GamePiece(state.nextPiece.piece, state.gameGrid)
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
    if (gameFinished) return

    system.scheduler.scheduleOnce(player.state.gameSpeed.milliseconds) {
      if (!player.state.curPiece.moveDown()) handlePieceBottom(player)
      else broadcastPiecePositions(player)

      gameTick(player)
    }
  }

  def broadcast(player: PlayerWithState, jsonObj: JsObject): Unit = {
    player.out ! (jsonObj + (GameAPIKeys.opponent -> JsBoolean(false))).toString()
    opponent(player).out ! (jsonObj + (GameAPIKeys.opponent -> JsBoolean(true))).toString()
  }

  def broadcastPiecePositions(player: PlayerWithState): Unit = {
    broadcast(player, Json.obj(piecePositionsToKeyVal(player)))
  }

  def piecePositionsToKeyVal(player: PlayerWithState): (String, JsValueWrapper) = {
    GameAPIKeys.piecePositions -> player.state.curPiece.getPositions.map(p => Array(p._1, p._2))
  }

  private def lose(player: PlayerWithState): Unit = {
    gameFinished = true

    player.out ! Json.obj(GameAPIKeys.won -> JsBoolean(false)).toString()
    opponent(player).out ! Json.obj(GameAPIKeys.won -> JsBoolean(true)).toString()

    player1.out ! PoisonPill
    player2.out ! PoisonPill

    gameManager.endGame(Result(
      None,
      player1.user.id.get, player1.state.points, player1.state.piecesPlaced,
      player2.user.id.get, player2.state.points, player2.state.piecesPlaced,
      (System.currentTimeMillis() - gameBeganAt) / 1000
    ))

    println(s"Game took ${(System.currentTimeMillis() - gameBeganAt) / 1000} seconds")
  }

  def lose(implicit id: Long): Unit = {
    lose(players(id))
  }

  def removeCompletedLines(state: GameState): Array[(Array[Boolean], Int)] = {
    val (removed, kept) = state.gameGrid
      .zipWithIndex
      .partition(p => p._1.count(x => x) == nGameCols)

    val newValues = Array.ofDim[Boolean](removed.length, nGameCols) ++ kept.map(_._1.map(identity))
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

    if (toSend.nonEmpty) {
      val oppPiece = opp.state.curPiece

      oppPiece.removeFromGrid()
      opp.state.updateGameGrid(opp.state.gameGrid.drop(toSend.length) ++ toSend)

      while (oppPiece.wouldCollideIfAddedToGrid()) {
        if (!oppPiece.moveUp(updateGridOnMove = false)) {
          return true
        }
      }

      oppPiece.addToGrid()
    }

    broadcastPiecePositions(opp)
    broadcast(opp, Json.obj(GameAPIKeys.gameGrid -> opp.state.gameGrid))
    false
  }
}

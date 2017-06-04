package game

import akka.actor.{ActorRef, PoisonPill}
import game.Pieces._
import game.PiecesWithPosition.{GamePiece, NextPiece}
import play.api.libs.json.Json.JsValueWrapper
import shared.GameRules.nGameCols
import play.api.libs.json.{JsBoolean, JsObject, Json}
import shared.Actions._
import shared.{GameAPIKeys, GameRules}

import scala.util.Random
import scala.concurrent.duration._


class Game(p1: Player, p2: Player) {

  case class PlayerWithState(player: Player) {
    val id: String = player.id
    val out: ActorRef = player.ref
    val state: GameState = new GameState(randomPiece(), randomPiece())
  }

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
    val state = player.state
    val nBlocksAbove = state.curPiece.getPositions.minBy(_._1)._1

    if (nBlocksAbove <= 1) {
      lose(player)
    }

    val nDeleted = removeCompletedLines(player)

    state.piecesPlaced += 1
    state.points += GameRules.pointsForPieceDown(nBlocksAbove, nDeleted, state.gameSpeed)

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

    val opp = opponent(player)
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

  def randomPiece(): Piece = Random.shuffle(List(BarPiece, InvLPiece, LPiece, SPiece, SquarePiece, TPiece, ZPiece)).head

  def removeCompletedLines(player: PlayerWithState): Int = {
    val state = player.state
    val (removed, kept) = state.gameGrid
      .zipWithIndex
      .partition(p => p._1.count(x => x) == nGameCols)

    val linesBeforeCompleted: Array[Array[Boolean]] = removed.map(p => {
      val row: Array[Boolean] = p._1
      val i = p._2
      row.indices.map(col => !state.curPiece.getPositions.contains((i, col))).toArray
    })

    player.state.gameGrid = Array.ofDim[Boolean](removed.length, nGameCols) ++ kept.map(_._1)

    val toAdd = removed.length match {
      case 1 | 2 | 3 => linesBeforeCompleted.take(removed.length - 1)
      case _ => linesBeforeCompleted
    }

    opponent(player).state.gameGrid = opponent(player).state.gameGrid.drop(toAdd.length) ++ toAdd

    removed.length
  }
}

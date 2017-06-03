package game

import akka.actor.{ActorRef, PoisonPill}
import game.Pieces._
import game.PiecesWithPosition.{GamePiece, NextPiece}
import shared.GameRules.{nGameCols, nGameRows}
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

  def movePiece(id: String, action: Action): Unit = {
    val user = players(id)
    val gs = user.state

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
      if (action == Fall) handlePieceBottom(user)
      else broadcast(user, Json.obj("gameGrid" -> gs.gameGrid))
    }
  }

  def setReady(id: String): Unit = {
    val user = players(id)
    user.state.ready = true
    println(id + " is ready")

    if (opponent(user).state.ready) {
      initGame()
    }
  }

  def initGame(): Unit = {
    broadcast(player1, Json.obj(
      GameAPIKeys.gameGrid -> player1.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> player1.state.nextPieceGrid
    ))
    broadcast(player2, Json.obj(
      GameAPIKeys.gameGrid -> player2.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> player2.state.nextPieceGrid
    ))

    gameTick(player1)
    gameTick(player2)
  }

  private def handlePieceBottom(player: PlayerWithState): Unit = {
    val state = player.state
    val nBlocksAbove = state.curPiece.getPositions.minBy(_._1)._1

    if (nBlocksAbove <= 1) {
      stopGame()
    }

    val nDeleted = deleteCompletedLines(state)

    state.piecesPlaced += 1
    state.points += GameRules.pointsForPieceDown(nBlocksAbove, nDeleted, state.gameSpeed)

    state.curPiece = new GamePiece(state.nextPiece.piece, state.gameGrid)
    state.curPiece.addToGrid()

    state.nextPiece.removeFromGrid()
    state.nextPiece = new NextPiece(randomPiece(), state.nextPieceGrid)
    state.nextPiece.addToGrid()

    state.gameSpeed = GameRules.nextSpeed(state.gameSpeed)

    broadcast(player, Json.obj(
      GameAPIKeys.gameGrid -> player.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> player.state.nextPieceGrid,
      GameAPIKeys.piecesPlaced -> state.piecesPlaced,
      GameAPIKeys.points -> state.points
    ))
  }

  private def gameTick(user: PlayerWithState): Unit = {
    system.scheduler.scheduleOnce(user.state.gameSpeed.milliseconds) {
      if (!user.state.curPiece.moveDown()) {
        handlePieceBottom(user)
      }
      else {
        broadcast(user, Json.obj(GameAPIKeys.gameGrid -> user.state.gameGrid))
      }
      gameTick(user)
    }
  }

  def broadcast(user: PlayerWithState, jsonObj: JsObject): Unit = {
    user.out ! Json.toJson(jsonObj + (GameAPIKeys.opponent -> JsBoolean(false))).toString()
    opponent(user).out ! Json.toJson(jsonObj + (GameAPIKeys.opponent -> JsBoolean(true))).toString()
  }

  def stopGame(): Unit = {
    val p1Points = player1.state.points
    val p2Points = player2.state.points

    if (p1Points == p2Points) {
      val toSend = Json.obj(GameAPIKeys.draw -> JsBoolean(true)).toString()
      player1.out ! toSend
      player2.out ! toSend
    } else {
      val winner = if (p1Points > p2Points) player1 else player2
      winner.out ! Json.obj(GameAPIKeys.won -> JsBoolean(true)).toString()
      opponent(winner).out ! Json.obj(GameAPIKeys.won -> JsBoolean(false)).toString()
    }

    player1.out ! PoisonPill
    player2.out ! PoisonPill
  }

  def randomPiece(): Piece = Random.shuffle(List(BarPiece, InvLPiece, LPiece, SPiece, SquarePiece, TPiece, ZPiece)).head

  def deleteCompletedLines(state: GameState): Int = {
    val res = state.gameGrid.filterNot(row => row.count(x => x) == nGameCols)
    val nDeleted = nGameRows - res.length
    state.gameGrid = Array.ofDim[Boolean](nDeleted, nGameCols) ++ res
    nDeleted
  }
}

package game

import akka.actor.ActorRef
import game.Pieces._
import game.PiecesWithPosition.{GamePiece, NextPiece}
import shared.GameSettings.{nGameCols, nGameRows}
import play.api.libs.json.{JsBoolean, JsObject, Json}
import shared.Actions._
import shared.GameAPIKeys

import scala.util.Random
import scala.concurrent.duration._


class Game(val gameUser1: GameUser, val gameUser2: GameUser) {

  case class GameUserWithState(user: GameUser) {
    val id: String = user.id
    val out: ActorRef = user.ref
    val state: GameState = new GameState(randomPiece(), randomPiece())
  }

  private val user1: GameUserWithState = GameUserWithState(gameUser1)
  private val user2: GameUserWithState = GameUserWithState(gameUser2)

  private val users: Map[String, GameUserWithState] = Map(
    user1.id -> user1,
    user2.id -> user2
  )

  //Use the system's dispatcher as ExecutionContext
  private val system = akka.actor.ActorSystem("system")
  import system.dispatcher

  def opponent(id: String): GameUserWithState = {
    if (id == user1.id) user2
    else user1
  }

  def gameState(userId: String): GameState = {
    users(userId).state
  }

  def movePiece(userId: String, action: Action): Unit = {
    val user = users(userId)
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
    users(id).state.ready = true
    println(id + " is ready")
    if (opponent(id).state.ready) {
      initGame()
    }
  }

  def initGame(): Unit = {
    broadcast(user1, Json.obj(
      GameAPIKeys.gameGrid -> user1.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> user1.state.nextPieceGrid
    ))
    broadcast(user2, Json.obj(
      GameAPIKeys.gameGrid -> user2.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> user2.state.nextPieceGrid
    ))

    gameTick(user1)
    gameTick(user2)
  }

  private def handlePieceBottom(user: GameUserWithState): Unit = {
    val state = user.state

    // TODO check game lost

    state.piecesPlaced += 1

    // amount of spaces piece has above itself
    state.points += state.curPiece.getPositions.minBy(_._1)._1

    deleteCompletedLines(state)

    state.nextPiece.removeFromGrid()

    state.curPiece = new GamePiece(state.nextPiece.piece, state.gameGrid)
    state.curPiece.addToGrid()

    state.nextPiece = new NextPiece(randomPiece(), state.nextPieceGrid)
    state.nextPiece.addToGrid()

    broadcast(user, Json.obj(
      GameAPIKeys.gameGrid -> user.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> user.state.nextPieceGrid,
      GameAPIKeys.piecesPlaced -> state.piecesPlaced,
      GameAPIKeys.points -> state.points
    ))
  }

  private def gameTick(user: GameUserWithState): Unit = {
    system.scheduler.scheduleOnce(user.state.gameSpeed.milliseconds) {
      if (!user.state.curPiece.moveDown()) handlePieceBottom(user)
      else broadcast(user, Json.obj(GameAPIKeys.gameGrid -> user.state.gameGrid))
      gameTick(user)
    }
  }

  def broadcast(user: GameUserWithState, jsonObj: JsObject): Unit = {
    user.out ! Json.toJson(jsonObj + (GameAPIKeys.opponent -> JsBoolean(false))).toString()
    opponent(user.id).out ! Json.toJson(jsonObj + (GameAPIKeys.opponent -> JsBoolean(true))).toString()
  }

  def randomPiece(): Piece = Random.shuffle(List(BarPiece, InvLPiece, LPiece, SPiece, SquarePiece, TPiece, ZPiece)).head

  def deleteCompletedLines(state: GameState): Unit = {
    val res = state.gameGrid.filterNot(row => row.count(x => x) == nGameCols)
    val nDeleted = nGameRows - res.length
    state.gameGrid = Array.ofDim[Boolean](nDeleted, nGameCols) ++ res
    state.points += (50 * Math.pow(nDeleted, 2)).toInt
  }
}

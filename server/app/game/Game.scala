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
      broadcast(user, Json.obj("gameGrid" -> gs.gameGrid))
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
    broadcast(user1, Json.obj(GameAPIKeys.gameGrid -> user1.state.gameGrid))
    broadcast(user1, Json.obj(GameAPIKeys.nextPieceGrid -> user1.state.nextPieceGrid))
    broadcast(user2, Json.obj(GameAPIKeys.gameGrid -> user2.state.gameGrid))
    broadcast(user2, Json.obj(GameAPIKeys.nextPieceGrid -> user2.state.nextPieceGrid))

    gameTick(user1)
    gameTick(user2)
  }

  private def gameTick(user: GameUserWithState): Unit = {
    //Use the system's dispatcher as ExecutionContext
    val system = akka.actor.ActorSystem("system")
    import system.dispatcher

    system.scheduler.scheduleOnce(user.state.gameSpeed.milliseconds) {
      val state = user.state
      if (!state.curPiece.moveDown()) {
        state.gameGrid = deleteCompletedLines(state.gameGrid)
        state.nextPiece.removeFromGrid()

        state.curPiece = new GamePiece(state.nextPiece.piece, state.gameGrid)
        state.curPiece.addToGrid()

        state.nextPiece = new NextPiece(randomPiece(), state.nextPieceGrid)
        state.nextPiece.addToGrid()

        broadcast(user, Json.obj(GameAPIKeys.nextPieceGrid -> user.state.nextPieceGrid))
      }

      broadcast(user, Json.obj(GameAPIKeys.gameGrid -> user.state.gameGrid))
      gameTick(user)
    }
  }

  def broadcast(user: GameUserWithState, jsonObj: JsObject): Unit = {
    user.out ! Json.toJson(jsonObj + (GameAPIKeys.opponent -> JsBoolean(false))).toString()
    opponent(user.id).out ! Json.toJson(jsonObj + (GameAPIKeys.opponent -> JsBoolean(true))).toString()
  }

  def randomPiece(): Piece = Random.shuffle(List(BarPiece, InvLPiece, LPiece, SPiece, SquarePiece, TPiece, ZPiece)).head

  def deleteCompletedLines(gameGrid: Array[Array[Boolean]]): Array[Array[Boolean]] = {
    val res = gameGrid.filterNot(row => row.count(x => x) == nGameCols)

    if (res.length < nGameRows) {
      return Array.ofDim[Boolean](nGameRows - res.length, nGameCols) ++ res
    }

    res
  }

}

package game

import akka.actor.ActorRef
import shared.GameSettings.{nGameCols, nGameRows}
import play.api.libs.json.{JsBoolean, JsObject, Json}
import shared.Actions._

import scala.util.Random

class Game(val gameUser1: GameUser, val gameUser2: GameUser) {

  case class GameUserWithState(user: GameUser) {
    val id: String = user.id
    val out: ActorRef = user.ref
    val state: GameState = GameState(randomPiece(), randomPiece())
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
      case Left => gs.piece.moveLeft()
      case Right => gs.piece.moveRight()
      case Fall => gs.piece.fall()
      case Rotate => gs.piece.rotate()
    }

    if (moved) {
      broadcast(user, Json.obj("gameGrid" -> gs.gameGrid))
    }
  }

  def setReady(id: String): Unit = {
    users(id).state.ready = true
    println(id + " is ready")
    if (opponent(id).state.ready) {
      initBroadcast()
    }
  }

  def initBroadcast(): Unit = {
    broadcast(users(gameUser1.id), Json.obj("gameGrid" -> users(gameUser1.id).state.gameGrid))
    broadcast(users(gameUser1.id), Json.obj("nexPieceGrid" -> users(gameUser1.id).state.nextPieceGrid))
    broadcast(users(gameUser2.id), Json.obj("gameGrid" -> users(gameUser2.id).state.gameGrid))
    broadcast(users(gameUser2.id), Json.obj("nexPieceGrid" -> users(gameUser2.id).state.nextPieceGrid))
  }

  def broadcast(user: GameUserWithState, jsonObj: JsObject): Unit = {
    user.out ! Json.toJson(jsonObj + ("opponent" -> JsBoolean(false))).toString()
    opponent(user.id).out ! Json.toJson(jsonObj + ("opponent" -> JsBoolean(true))).toString()
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

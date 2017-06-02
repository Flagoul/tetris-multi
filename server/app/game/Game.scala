package game

import shared.GameSettings.{nGameCols, nGameRows}
import play.api.libs.json.{JsBoolean, JsObject, Json}
import shared.Actions._

import scala.util.Random

class Game(val user1: GameUser, val user2: GameUser) {
  case class UserGameState(user: GameUser, state: GameState)

  private val users: Map[String, UserGameState] = Map(
    user1.id -> UserGameState(user1, new GameState(randomPiece(), randomPiece())),
    user2.id -> UserGameState(user2, new GameState(randomPiece(), randomPiece()))
  )

  def opponent(userId: String): GameUser = {
    if (userId == user1.id) user2
    else user1
  }

  def gameState(userId: String): GameState = {
    users(userId).state
  }

  def movePiece(userId: String, action: Action): Unit = {
    println(s"Moving piece to ${action.name}")
    val user = users(userId)
    val gs = user.state

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

  def broadcast(user: UserGameState, jsonObj: JsObject): Unit = {
    user.user.ref ! Json.toJson(jsonObj + ("opponent" -> JsBoolean(false))).toString()
    opponent(user.user.id).ref ! Json.toJson(jsonObj + ("opponent" -> JsBoolean(true))).toString()
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

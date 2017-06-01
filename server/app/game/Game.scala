package game

import game.GameSettings.{nGameCols, nGameRows}
import play.api.libs.json.Json
import shared.Actions._

import scala.util.Random

class Game(val userId1: String, val userId2: String) {
  private val userGameStates: Map[String, GameState] = Map(
    userId1 -> new GameState(randomPiece(), randomPiece()),
    userId2 -> new GameState(randomPiece(), randomPiece())
  )

  def gameState(userId: String): GameState = {
    userGameStates(userId)
  }

  def gridToJson(grid: Array[Array[Boolean]]): String = {
    println("stringify")
    Json.stringify(Json.obj(
      "gameGrid" -> Json.arr(grid)
    ))
  }

  def movePiece(userId: String, action: Action): String = {
    val gs = gameState(userId)
    action match {
      case Left => gs.piece.moveLeft()
      case Right => gs.piece.moveRight()
      case Fall => gs.piece.fall()
      case Rotate => gs.piece.rotate()
    }
    gridToJson(gs.gameGrid)
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

package game

import shared.Pieces.Piece
import shared.GameRules._
import game.PiecesWithPosition._

class GameState(cur: Piece, next: Piece) {
  var gameSpeed: Long = 1000

  val gameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
  val nextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

  var curPiece: GamePiece = new GamePiece(cur, gameGrid)
  var nextPiece: NextPiece = new NextPiece(next, nextPieceGrid)

  var piecesPlaced: Int = 0
  var points: Long = 0

  var ready: Boolean = false

  def updateGameGrid(newValues: Array[Array[Boolean]]): Unit = {
    for (i <- gameGrid.indices; j <- gameGrid.head.indices) {
      gameGrid(i)(j) = newValues(i)(j)
    }
  }
}

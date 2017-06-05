package game

import shared.GameRules._
import game.PiecesWithPosition._
import shared.Pieces.randomPiece

class GameState() {
  var gameSpeed: Long = 1000

  val gameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
  val nextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

  var curPiece: GamePiece = new GamePiece(randomPiece(), gameGrid)
  var nextPiece: NextPiece = new NextPiece(randomPiece(), nextPieceGrid)

  var piecesPlaced: Int = 0
  var points: Long = 0

  var ready: Boolean = false

  def updateGameGrid(newValues: Array[Array[Boolean]]): Unit = {
    for (i <- gameGrid.indices; j <- gameGrid.head.indices) {
      gameGrid(i)(j) = newValues(i)(j)
    }
  }
}

package game

import game.pieces.{GamePiece, NextPiece}
import shared.GameRules._
import shared.Pieces.randomPiece
import shared.Types.Grid

/**
  * Represents the state during the game for a given player.
  */
class GameState() {
  // The current speed of the game.
  var gameSpeed: Long = 1000

  // The game grid that contains pieces falling.
  val gameGrid: Grid = Array.ofDim[Boolean](nGameRows, nGameCols)

  // The grid containing the next piece that will fall.
  val nextPieceGrid: Grid = Array.ofDim[Boolean](nGameRows, nGameCols)

  // The current and next pieces falling.
  var curPiece: GamePiece = new GamePiece(randomPiece(), gameGrid)
  var nextPiece: NextPiece = new NextPiece(randomPiece(), nextPieceGrid)

  // The number of pieces placed and points scored.
  var piecesPlaced: Int = 0
  var points: Long = 0

  /**
    * Updates the game grid with the new values specified.
    *
    * @param newValues The new values of the grid.
    */
  def updateGameGrid(newValues: Grid): Unit = {
    for (i <- gameGrid.indices; j <- gameGrid.head.indices) {
      gameGrid(i)(j) = newValues(i)(j)
    }
  }
}

package game

import GameSettings._
import game.PiecesWithPosition._

class GameState(var currentPiece: Piece, var nextPiece: Piece) {

  // FIXME temporary: should change with time
  val gameSpeed: Int = 500

  val gameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
  val nextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

  var piece = new GamePiece(currentPiece, gameGrid)
  var next = new NextPiece(nextPiece, nextPieceGrid)
}

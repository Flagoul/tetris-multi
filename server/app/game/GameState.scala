package game

import game.Pieces.Piece
import shared.GameSettings._
import game.PiecesWithPosition._

class GameState(cur: Piece, next: Piece) {
  var gameSpeed: Int = 1000

  var gameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
  val nextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

  var curPiece: GamePiece = new GamePiece(cur, gameGrid)
  var nextPiece: NextPiece = new NextPiece(next, nextPieceGrid)

  var ready: Boolean = false
}

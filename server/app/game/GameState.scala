package game

import shared.GameSettings._
import game.PiecesWithPosition._

case class GameState(var currentPiece: Piece, var nextPiece: Piece) {
  // FIXME temporary: should change with time
  val gameSpeed: Int = 500

  val gameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
  val nextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

  var piece: GamePiece = new GamePiece(currentPiece, gameGrid)
  var next: NextPiece = new NextPiece(nextPiece, nextPieceGrid)

  var ready: Boolean = false
}

package game

import game.Pieces.Piece
import shared.GameRules._
import game.PiecesWithPosition._

class GameState(cur: Piece, next: Piece) {
  var gameSpeed: Long = 1000

  var gameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
  val nextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

  var curPiece: GamePiece = new GamePiece(cur, gameGrid)
  var nextPiece: NextPiece = new NextPiece(next, nextPieceGrid)

  var piecesPlaced: Int = 0
  var points: Long = 0

  var ready: Boolean = false
}

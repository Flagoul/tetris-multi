package game.pieces

import shared.GameRules.{nNextPieceCols, nNextPieceRows}
import shared.Pieces.Piece
import shared.Types.Grid

/**
  * Represents a piece in the next piece grid.
  *
  * @param piece The piece to use.
  * @param nextPieceGrid The next piece grid.
  */
class NextPiece(piece: Piece, nextPieceGrid: Grid) extends PieceWithPositions(
  piece, nextPieceGrid,
  (nNextPieceRows / 2 - piece.shape.length / 2 , nNextPieceCols / 2 - piece.shape.head.length / 2)
)

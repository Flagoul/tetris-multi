package game.pieces

import shared.Pieces.Piece
import shared.Types.{Grid, Position}

/**
  * Represents a piece with positions in a grid.
  *
  * @param piece The piece to use.
  * @param grid The grid in which the piece will be.
  * @param initPos Where in the gris the piece will be initially positioned.
  */
class PieceWithPositions(val piece: Piece, grid: Grid, initPos: Position) {
  // The position of each block of the piece
  protected var positions: List[Position] = initPiecePositions()

  /**
    * Creates positions for the piece, based on the position where to start drawing.
    *
    * @return The positions of where the blocks of the pieces should be drawn.
    */
  def initPiecePositions(): List[Position] = {
    val pieceWidth = piece.shape.head.length
    val pieceHeight = piece.shape.length

    val positions = for {
      row <- 0 until pieceHeight
      col <- 0 until pieceWidth
      if piece.shape(row)(col)
    } yield (initPos._1 + row, initPos._2 + col)

    positions.toList
  }

  /**
    * @return The positions of the blocks of the piece in the context of the grid.
    */
  def getPositions: List[Position] = positions

  /**
    * Add the piece to the grid, changing the values in the grid according to the piece's positions.
    */
  def addToGrid(): Unit = {
    updateGridAtCurrentPosition(true)
  }

  /**
    * Removes the piece for the grid, changing the values in the grid according to the piece's positions.
    */
  def removeFromGrid(): Unit = {
    updateGridAtCurrentPosition(false)
  }

  /**
    * Updates the grid at the cells indicated by the positions of the piece.
    *
    * @param value The value to set at the piece's positions.
    */
  private def updateGridAtCurrentPosition(value: Boolean): Unit = {
    for (pos <- positions) {
      grid(pos._1)(pos._2) = value
    }
  }
}

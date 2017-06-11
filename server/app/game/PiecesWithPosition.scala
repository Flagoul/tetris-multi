package game

import shared.GameRules._
import shared.Pieces._
import shared.Types.Position

/**
  * Regroups pieces with positions and its derivatives.
  */
object PiecesWithPosition {

  /**
    * Represents a piece with positions in a grid.
    * @param piece The piece to use.
    * @param grid The grid in which the piece will be.
    * @param initPos Where in the gris the piece will be initially positioned.
    */
  class PieceWithPosition(val piece: Piece, grid: Array[Array[Boolean]], initPos: Position) {
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

  /**
    * Represents a piece in the grid of the game, where pieces are falling.
    *
    * @param piece The piece to use.
    * @param gameGrid The grid of the game.
    */
  class GamePiece(piece: Piece, gameGrid: Array[Array[Boolean]]) extends PieceWithPosition(
    piece, gameGrid,
    (1, nGameCols / 2 - piece.shape.head.length / 2)
  ) {

    /**
      * Updates the grid new positions of the blocks of the piece.
      *
      * @param newPositions The new positions of the piece.
      */
    private def updateGrid(newPositions: List[Position]): Unit = {
      removeFromGrid()
      positions = newPositions
      addToGrid()
    }

    /**
      * Determines whether moving the piece with at the new position would collide with the blocks already existing in
      * the grid or be outside the grid.
      *
      * @param newPositions The new positions of the blocks of the piece.
      * @param inBounds A predicate that determines whether a position is in the bounds of the grid.
      * @return Whether the new positions would make the piece collide.
      */
    private def wouldCollide(newPositions: List[Position], inBounds: Position => Boolean): Boolean = {
      newPositions.exists(p => !inBounds(p) || (gameGrid(p._1)(p._2) && !positions.contains(p)))
    }

    /**
      * Applies a transformation on the current positions if they are valid positions, such as the piece would not collide
      * with anything.
      *
      * @param transform The transformation to apply to each position.
      * @param inBounds A predicate determining whether a position is in the bounds of the grid.
      * @param updateGridOnMove Whether the grid should be updated if the positions are valid. Default true.
      * @return Whether the piece could be moved.
      */
    private def move(transform: Position => Position, inBounds: Position => Boolean, updateGridOnMove: Boolean = true): Boolean = {
      val newPositions: List[Position] = positions.map(transform)

      val couldMove = !wouldCollide(newPositions, inBounds)
      if (couldMove) {
        if (updateGridOnMove) updateGrid(newPositions)
        else positions = newPositions
      }

      couldMove
    }

    /**
      * Helpers predicates to known whether a given position p is in bound relatively to the grid bounds.
      */
    private def inBoundLeft(p: Position): Boolean = p._2 >= 0
    private def inBoundRight(p: Position): Boolean = p._2 < nGameCols
    private def inBoundBottom(p: Position): Boolean = p._1 < nGameRows
    private def inBoundUp(p: Position): Boolean = p._1 >= 0
    private def inBounds(p: Position): Boolean = inBoundLeft(p) && inBoundRight(p) && inBoundBottom(p) && inBoundUp(p)

    /**
      * Helpers to transform a given position p according to the direction wanted.
      */
    private def transformLeft(p: Position): Position = (p._1, p._2 - 1)
    private def transformRight(p: Position): Position = (p._1, p._2 + 1)
    private def transformDown(p: Position): Position = (p._1 + 1, p._2)
    private def transformUp(p: Position): Position = (p._1 - 1, p._2)

    /**
      * Moves the piece at the left direction.
      *
      * @param updateGridOnMove Whether the grid should be updated on move. Default true.
      * @return Whether the piece could move.
      */
    def moveLeft(updateGridOnMove: Boolean = true): Boolean = move(transformLeft, inBoundLeft, updateGridOnMove)

    /**
      * Moves the piece right direction.
      *
      * @param updateGridOnMove Whether the grid should be updated on move. Default true.
      * @return Whether the piece could move.
      */
    def moveRight(updateGridOnMove: Boolean = true): Boolean = move(transformRight, inBoundRight, updateGridOnMove)

    /**
      * Moves the piece downwards.
      *
      * @param updateGridOnMove Whether the grid should be updated on move. Default true.
      * @return Whether the piece could move.
      */
    def moveDown(updateGridOnMove: Boolean = true): Boolean = move(transformDown, inBoundBottom, updateGridOnMove)

    /**
      * Moves the piece upwards without checking collisions with grid blocks.
      *
      * @param updateGridOnMove Whtehr the grid must be updated on move. Default true.
      * @return
      */
    def moveUpWithOnlyGridBoundsCheck(updateGridOnMove: Boolean = true): Boolean = {
      val newPositions = positions.map(transformUp)
      val inBounds = isInBounds

      if (inBounds) {
        if (updateGridOnMove) updateGrid(newPositions)
        else positions = newPositions
      }

      inBounds
    }

    /**
      * Makes the piece fall to the bottom of the grid.
      *
      * @return whether the piece could move.
      */
    def fall(): Boolean = {
      var moved = false
      do {
        moved = moveDown()
      } while (moved)
      true
    }

    /**
      * Rotates the piece.
      *
      * The piece is rotated clockwise using the center of rotation of the piece for 3-blocks wide pieces. For the bar,
      * a transposition is used.
      *
      * @return Whether the piece could be rotated.
      */
    def rotate(): Boolean = piece match {
      case SquarePiece => true
      case _ =>

        // The position of the center of the piece; it is the axis of rotation.
        // It is always the block at (0, 1) in the shape of the piece; to know the actual position we have to
        // compute how many "true" they are in the first line of the shape and use it as index in the actual positions.
        val center = positions(piece.shape.head.slice(0, 2).count(x => x) - 1)

        // origin of square surrounding piece
        val orig = (center._1 - 1, center._2 - 1)

        // translating positions relatively to origin and do transpose rotation for bar or 90° rotation for others
        val newPositions = positions.map(p => {
          val tRow = p._1 - orig._1
          val tCol = p._2 - orig._2

          piece match {
            case BarPiece => (tCol + orig._1, tRow + orig._2)
            case _ => (tCol + orig._1, 3 - tRow - 1 + orig._2)
          }
        })

        val couldMove = !wouldCollide(newPositions, inBounds)
        if (couldMove) {
          updateGrid(newPositions)
        }

        couldMove
    }

    /**
      * @return Whether the piece is in grid bounds.
      */
    private def isInBounds: Boolean = positions.forall(p => inBounds(p))

    /**
      * @return whether the piece would collide with grid blocks if added to the grid.
      */
    def wouldCollideIfAddedToGrid(): Boolean = positions.exists(p => gameGrid(p._1)(p._2))
  }

  /**
    * Represents a piece in the next piece grid.
    *
    * @param piece The piece to use.
    * @param nextPieceGrid The next piece grid.
    */
  class NextPiece(piece: Piece, nextPieceGrid: Array[Array[Boolean]]) extends PieceWithPosition(
    piece, nextPieceGrid,
    (nNextPieceRows / 2 - piece.shape.length / 2 , nNextPieceCols / 2 - piece.shape.head.length / 2)
  )
}

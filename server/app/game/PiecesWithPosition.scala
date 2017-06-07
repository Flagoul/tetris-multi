package game

import shared.GameRules._
import shared.Pieces._
import shared.Types.Position

object PiecesWithPosition {
  class PieceWithPosition(val piece: Piece, grid: Array[Array[Boolean]], initPos: Position) {
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

    def getPositions: List[Position] = positions

    def addToGrid(): Unit = {
      updateGridAtCurrentPosition(true)
    }

    def removeFromGrid(): Unit = {
      updateGridAtCurrentPosition(false)
    }

    private def updateGridAtCurrentPosition(value: Boolean): Unit = {
      for (pos <- positions) {
        grid(pos._1)(pos._2) = value
      }
    }
  }

  class GamePiece(piece: Piece, gameGrid: Array[Array[Boolean]]) extends PieceWithPosition(
    piece, gameGrid,
    (1, nGameCols / 2 - piece.shape.head.length / 2)
  ) {

    def updateGrid(newPositions: List[Position]): Unit = {
      removeFromGrid()
      positions = newPositions
      addToGrid()
    }

    def wouldCollide(newPositions: List[Position], inBounds: Position => Boolean): Boolean = {
      newPositions.exists(p => !inBounds(p) || (gameGrid(p._1)(p._2) && !positions.contains(p)))
    }

    private def move(transform: Position => Position, inBounds: Position => Boolean, updateGridOnMove: Boolean = true): Boolean = {
      val newPositions: List[Position] = positions.map(transform)

      val couldMove = !wouldCollide(newPositions, inBounds)
      if (couldMove) {
        if (updateGridOnMove) updateGrid(newPositions)
        else positions = newPositions
      }

      couldMove
    }

    def inBoundLeft(p: Position): Boolean = p._2 >= 0
    def inBoundRight(p: Position): Boolean = p._2 < nGameCols
    def inBoundBottom(p: Position): Boolean = p._1 < nGameRows
    def inBoundUp(p: Position): Boolean = p._1 >= 0
    def inBounds(p: Position): Boolean = inBoundLeft(p) && inBoundRight(p) && inBoundBottom(p)

    def transformLeft(p: Position): Position = (p._1, p._2 - 1)
    def transformRight(p: Position): Position = (p._1, p._2 + 1)
    def transformDown(p: Position): Position = (p._1 + 1, p._2)
    def transformUp(p: Position): Position = (p._1 - 1, p._2)

    def moveLeft(updateGridOnMove: Boolean = true): Boolean = move(transformLeft, inBoundLeft, updateGridOnMove)
    def moveRight(updateGridOnMove: Boolean = true): Boolean = move(transformRight, inBoundRight, updateGridOnMove)
    def moveDown(updateGridOnMove: Boolean = true): Boolean = move(transformDown, inBoundBottom, updateGridOnMove)
    def moveUp(updateGridOnMove: Boolean = true): Boolean = move(transformUp, inBoundUp, updateGridOnMove)

    def wouldCollideIfAddedToGrid(): Boolean = positions.exists(p => gameGrid(p._1)(p._2))

    def fall(): Boolean = {
      var moved = false
      do {
        moved = moveDown()
      } while (moved)
      true
    }

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
  }

  class NextPiece(piece: Piece, gameGrid: Array[Array[Boolean]]) extends PieceWithPosition(
    piece, gameGrid,
    (nNextPieceRows / 2 - piece.shape.length / 2 , nNextPieceCols / 2 - piece.shape.head.length / 2)
  )
}

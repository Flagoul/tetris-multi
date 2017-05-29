package tetris.game

import org.scalajs.dom

import scala.util.Random
import scala.scalajs.js.timers.setInterval

class Game {
  val nGameRows: Int = 22
  val nGameCols: Int = 10
  val nNextPieceRows: Int = 5
  val nNextPieceCols: Int = 5

  // FIXME temporary: should change with time
  val gameSpeed: Int = 500

  private val userGB: GameBox = new GameBox("user-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)
  private val opponentGB: GameBox = new GameBox("opponent-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)

  // A position in a grid on the form (row, col)
  type Position = (Int, Int)


  class PieceWithPosition(piece: Piece, grid: Array[Array[Boolean]], initPos: Position) {
    protected var positions: List[Position] = initPiecePositions()

    /**
      * Creates positions for the piece, based on the position where to start drawing.
      *
      * @return The positions of where the blocks of the pieces should be drawn.
      */
    def initPiecePositions(): List[Position] = {
      val pieceShape = piece.shape()
      val pieceWidth = pieceShape.head.length
      val pieceHeight = pieceShape.length

      val positions = for {
        row <- 0 until pieceHeight
        col <- 0 until pieceWidth
        if pieceShape(row)(col)
      } yield (initPos._1 + row, initPos._2 + col)

      positions.toList
    }

    def getPositions: List[Position] = positions
  }

  class GamePiece(piece: Piece, gameGrid: Array[Array[Boolean]]) extends PieceWithPosition(
    piece, gameGrid,
    (1, nGameCols / 2 - piece.shape().head.length / 2)
  ) {

    private def move(transform: Position => Position, outOfBounds: Position => Boolean): Boolean = {
      val newPositions: List[Position] = positions.map(transform)

      // piece out of bounds or collided with other piece
      if (newPositions.exists(p => outOfBounds(p) || (gameGrid(p._1)(p._2) && !positions.contains(p)))) {
        return false
      }

      updateGridAtPositions(gameGrid, positions, value = false)
      updateGridAtPositions(gameGrid, newPositions, value = true)
      userGB.drawGame(gameGrid)

      positions = newPositions

      true
    }

    def moveLeft(): Boolean = {
      move(
        p => (p._1, p._2 - 1),
        p => p._2 < 0 || (gameGrid(p._1)(p._2) && !positions.contains(p))
      )
    }

    def moveRight(): Boolean = {
      move(
        p => (p._1, p._2 + 1),
        p => p._2 >= nGameCols
      )
    }

    def moveDown(): Boolean = {
      move(
        p => (p._1 + 1, p._2),
        p => p._1 >= nGameRows
      )
    }

    def fall(): Unit = {
      var moved = false
      do {
        moved = moveDown()
      } while (moved)
    }

    def rotate(): Boolean = piece match {
      case Square => true
      case _ =>

        // The position of the center of the piece; it is the axis of rotation.
        // It is always the block at (0, 1) in the shape of the piece; to know the actual position we have to
        // compute how many "true" they are in the first line of the shape and use it as index in the actual positions.
        val center = positions(piece.shape().head.slice(0, 2).count(x => x) - 1)

        // origin of square surrounding piece
        val orig = (center._1 - 1, center._2 - 1)

        // translating positions relatively to origin and do right rotation
        val newPositions = piece match {
          case Bar => positions.map(p => {
            val tRow = p._1 - orig._1
            val tCol = p._2 - orig._2
            (tCol + orig._1, tRow + orig._2)
          })
          case _ => positions.map(p => {
            val tRow = p._1 - orig._1
            val tCol = p._2 - orig._2
            (tCol + orig._1, 3 - tRow - 1 + orig._2)
          })
        }

        def outOfBounds(pos: Position) = pos._1 < 0 || pos._1 >= nGameRows || pos._2 < 0 || pos._2 >= nGameCols

        // piece out of bounds or collided with other piece
        if (newPositions.exists(p => outOfBounds(p) || (gameGrid(p._1)(p._2) && !positions.contains(p)))) {
          return false
        }

        updateGridAtPositions(gameGrid, positions, value = false)
        updateGridAtPositions(gameGrid, newPositions, value = true)
        userGB.drawGame(gameGrid)

        positions = newPositions

        true
    }
  }

  class NextPiece(piece: Piece, gameGrid: Array[Array[Boolean]]) extends PieceWithPosition(
    piece, gameGrid,
    (nNextPieceRows / 2 - piece.shape().length / 2 , nNextPieceCols / 2 - piece.shape().head.length / 2)
  )

  def randomPiece(): Piece = Random.shuffle(List(Bar, InvL, L, S, Square, T, Z)).head

  def updateGridAtPositions(grid: Array[Array[Boolean]], positions: List[Position], value: Boolean): Unit = {
    for (pos <- positions) {
      grid(pos._1)(pos._2) = value
    }
  }

  def run(): Unit = {
    val gameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
    val nextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

    val opponentGameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
    val opponentNextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

    var currentPiece = randomPiece()
    var nextPiece: Piece = randomPiece()

    var piece = new GamePiece(currentPiece, gameGrid)
    var next = new NextPiece(nextPiece, nextPieceGrid)

    updateGridAtPositions(gameGrid, piece.getPositions, value = true)
    updateGridAtPositions(nextPieceGrid, next.getPositions, value = true)

    userGB.drawGame(gameGrid)
    userGB.drawNextPiece(nextPieceGrid)

    opponentGB.drawGame(opponentGameGrid)
    opponentGB.drawNextPiece(opponentNextPieceGrid)

    dom.window.onkeypress = { (e: dom.KeyboardEvent) =>
      e.charCode match {
        case 97 => piece.moveLeft()
        case 100 => piece.moveRight()
        case 115 => piece.fall()
        case 119 => piece.rotate()
      }
    }

    setInterval(gameSpeed) {
      val moved = piece.moveDown()

      if (!moved) {
        updateGridAtPositions(nextPieceGrid, next.getPositions, value = false)

        currentPiece = nextPiece
        nextPiece = randomPiece()

        piece = new GamePiece(currentPiece, gameGrid)
        next = new NextPiece(nextPiece, nextPieceGrid)

        updateGridAtPositions(gameGrid, piece.getPositions, value = true)
        updateGridAtPositions(nextPieceGrid, next.getPositions, value = true)

        userGB.drawNextPiece(nextPieceGrid)
      }

      userGB.drawGame(gameGrid)
    }
  }
}

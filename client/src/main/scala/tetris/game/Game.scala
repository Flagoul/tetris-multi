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
    (0, nGameCols / 2 - piece.shape().head.length / 2)
  ) {

    //private var center: Position = ???

    private def move(transform: Position => Position, outOfBounds: Position => Boolean): Boolean = {
      val newPositions: List[Position] = positions.map(transform)

      // piece out of bounds or collided with other piece
      if (newPositions.exists(p => outOfBounds(p) || (gameGrid(p._1)(p._2) && !positions.contains(p)))) {
        return false
      }

      updateGridAtPositions(gameGrid, positions, false)
      updateGridAtPositions(gameGrid, newPositions, true)
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
      case Bar => ???
      case _ =>
        // top left of a rect surrounding piece
        val topLeft = positions.foldLeft((nGameRows, nGameCols))((a, b) => (Math.min(a._1, b._1), Math.min(a._2, b._2)))
        println("topLeft", topLeft)

        val bottomRight = positions.foldLeft((0, 0))((a, b) => (Math.max(a._1, b._1), Math.min(a._2, b._2)))

        val width = bottomRight._2 - topLeft._2
        val height = bottomRight._1 - topLeft._1

        val orig = (
          if (height == 2) topLeft._1 - 1 else topLeft._1,
          if (width == 2) topLeft._2 - 1 else topLeft._2
        )

        // translating (0,0) to matrix top left and do right rotation surrounding piece then rotation
        val newPositions = positions.map(p => {
          val tRow = p._1 - orig._1
          val tCol = p._2 - orig._2
          (tCol + orig._1, 3 - tRow - 1 + orig._2)
        })

        def outOfBounds(pos: Position) = pos._1 < 0 || pos._1 >= nGameRows || pos._2 < 0 || pos._2 >= nGameCols

        println("Positions")
        positions.foreach(println(_))

        println("New positions")
        newPositions.foreach(println(_))

        // piece out of bounds or collided with other piece
        if (newPositions.exists(p => outOfBounds(p) || (gameGrid(p._1)(p._2) && !positions.contains(p)))) {
          return false
        }

        updateGridAtPositions(gameGrid, positions, false)
        updateGridAtPositions(gameGrid, newPositions, true)
        userGB.drawGame(gameGrid)

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
    var gameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
    var nextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

    var opponentGameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
    var opponentNextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

    var currentPiece = randomPiece()
    var nextPiece: Piece = randomPiece()

    var piece = new GamePiece(currentPiece, gameGrid)
    var next = new NextPiece(nextPiece, nextPieceGrid)

    updateGridAtPositions(gameGrid, piece.getPositions, value = true)
    updateGridAtPositions(nextPieceGrid, piece.getPositions, value = true)

    userGB.drawGame(gameGrid)
    userGB.drawNextPiece(nextPieceGrid)

    opponentGB.drawGame(opponentGameGrid)
    opponentGB.drawNextPiece(opponentNextPieceGrid)

    dom.window.onkeypress = { (e: dom.KeyboardEvent) =>
      println("key pressed", e.charCode)
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
        updateGridAtPositions(nextPieceGrid, next.getPositions, false)

        currentPiece = nextPiece
        nextPiece = randomPiece()

        piece = new GamePiece(currentPiece, gameGrid)
        next = new NextPiece(nextPiece, nextPieceGrid)

        updateGridAtPositions(gameGrid, piece.getPositions, true)
        updateGridAtPositions(nextPieceGrid, next.getPositions, true)

        userGB.drawNextPiece(nextPieceGrid)
      }

      userGB.drawGame(gameGrid)
    }
  }
}

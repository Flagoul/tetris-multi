package tetris.game

import org.scalajs.dom
import tetris.game.Types.Position

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


  def randomPiece(): Piece = Random.shuffle(List(Bar, InvL, L, S, Square, T, Z)).head

  /**
    * Creates positions for a given piece, based on the position where to start drawing.
    *
    * @param pieceShape The shape of the piece, i.e. the matrix values that define a piece.
    * @param drawPos The position where to begin drawing.
    * @return The positions of where the blocks of the pieces should be drawn.
    */
  def piecePositions(pieceShape: Array[Array[Boolean]], drawPos: Position): List[Position] = {
    val pieceWidth = pieceShape.head.length
    val pieceHeight = pieceShape.length

    val positions = for {
      row <- 0 until pieceHeight
      col <- 0 until pieceWidth
      if pieceShape(row)(col)
    } yield (drawPos._1 + row, drawPos._2 + col)

    positions.toList
  }

  /**
    * Initializes the positions for a piece in the game grid (top center of grid).
    *
    * @param piece The piece which positions must be initialized.
    * @return The positions of the piece.
    */
  def initGamePiecePositions(piece: Piece): List[Position] = {
    val shape = piece.shape()
    piecePositions(shape, (0, nGameCols / 2 - shape.head.length / 2))
  }

  /**
    * Initializes the position for a piece drawn in the nexte piece preview (at the center of preview).
    *
    * @param piece The piece which positions must be initialized.
    * @return The positions of the piece.
    */
  def nextPiecePositions(piece: Piece): List[Position] = {
    val shape = piece.shape()
    piecePositions(shape, (nNextPieceRows / 2 - shape.length / 2 , nNextPieceCols / 2 - shape.head.length / 2))
  }

  private def move(gameGrid: Array[Array[Boolean]], positions: List[Position],
                   transform: Position => Position, outOfBounds: Position => Boolean): List[Position] = {
    val newPositions: List[Position] = positions.map(transform)

    // piece out of bounds or collided with other piece
    if (newPositions.exists(p => outOfBounds(p) || (gameGrid(p._1)(p._2) && !positions.contains(p)))) {
      return List()
    }

    updateGridAtPositions(gameGrid, positions, false)
    updateGridAtPositions(gameGrid, newPositions, true)
    userGB.drawGame(gameGrid)

    newPositions
  }

  def moveDown(gameGrid: Array[Array[Boolean]], positions: List[Position]): List[Position] = {
    move(
      gameGrid, positions,
      p => (p._1 + 1, p._2),
      p => p._1 >= nGameRows
    )
  }

  def moveLeft(gameGrid: Array[Array[Boolean]], positions: List[Position]): List[Position] = {
    move(
      gameGrid, positions,
      p => (p._1, p._2 - 1),
      p => p._2 < 0 || (gameGrid(p._1)(p._2) && !positions.contains(p))
    )
  }

  def moveRight(gameGrid: Array[Array[Boolean]], positions: List[Position]): List[Position] = {
    move(
      gameGrid, positions,
      p => (p._1, p._2 + 1),
      p => p._2 >= nGameCols
    )
  }

  def rotate(gameGrid: Array[Array[Boolean]], positions: List[Position], piece: Piece): List[Position] = piece match {
    case Square => positions
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
        return List()
      }

      updateGridAtPositions(gameGrid, positions, false)
      updateGridAtPositions(gameGrid, newPositions, true)
      userGB.drawGame(gameGrid)

      newPositions
  }

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

    var piece: Piece = randomPiece()
    var nextPiece: Piece = randomPiece()

    var piecePositions: List[Position] = initGamePiecePositions(piece)
    var npPositions: List[Position] = nextPiecePositions(nextPiece)

    updateGridAtPositions(gameGrid, piecePositions, true)
    updateGridAtPositions(nextPieceGrid, npPositions, true)

    userGB.drawGame(gameGrid)
    userGB.drawNextPiece(nextPieceGrid)

    opponentGB.drawGame(opponentGameGrid)
    opponentGB.drawNextPiece(opponentNextPieceGrid)


    def movePieceLeft(): Unit = {
      val positions = moveLeft(gameGrid, piecePositions)
      if (positions.nonEmpty) {
        piecePositions = positions
      }
    }

    def movePieceRight(): Unit = {
      val positions = moveRight(gameGrid, piecePositions)
      if (positions.nonEmpty) {
        piecePositions = positions
      }
    }

    def movePieceDown(): Unit = {
      do {
        piecePositions = moveDown(gameGrid, piecePositions)
      } while (piecePositions.nonEmpty)
    }

    def rotatePiece(): Unit = {
      val positions = rotate(gameGrid, piecePositions, piece)
      if (positions.nonEmpty) {
        piecePositions = positions
      }
    }

    dom.window.onkeypress = { (e: dom.KeyboardEvent) =>
      println("key pressed", e.charCode)
      e.charCode match {
        case 97 => movePieceLeft()
        case 100 => movePieceRight()
        case 115 => movePieceDown()
        case 119 => rotatePiece()
      }
    }

    setInterval(gameSpeed) {
      piecePositions = moveDown(gameGrid, piecePositions)

      if (piecePositions.isEmpty) {
        piece = nextPiece
        nextPiece = randomPiece()

        updateGridAtPositions(nextPieceGrid, npPositions, false)

        piecePositions = initGamePiecePositions(piece)
        npPositions = nextPiecePositions(nextPiece)

        updateGridAtPositions(gameGrid, piecePositions, true)
        updateGridAtPositions(nextPieceGrid, npPositions, true)

        userGB.drawNextPiece(nextPieceGrid)
      }

      userGB.drawGame(gameGrid)
    }
  }
}

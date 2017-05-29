package example.game

import org.scalajs.dom

import scala.util.Random
import scala.scalajs.js.timers.{setInterval, clearInterval}

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
                   transform: Position => Position, inGridBorder: Position => Boolean): List[Position] = {
    val newPositions: List[Position] = positions.map(transform)

    // piece collided with bottom or other piece
    if (newPositions.exists(p => inGridBorder(p) || (gameGrid(p._1)(p._2) && !positions.contains(p)))) {
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

  def fall(gameGrid: Array[Array[Boolean]], positions: List[Position]): Unit = {
    // TODO do some animation
    var newPositions = moveDown(gameGrid, positions)

    while (newPositions.nonEmpty) {
      userGB.drawGame(gameGrid)
      newPositions = moveDown(gameGrid, newPositions)
    }
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

    dom.window.onkeypress = { (e: dom.KeyboardEvent) =>
      println("key pressed", e.charCode)
      e.charCode match {
        case 97 => movePieceLeft()
        case 100 => movePieceRight()
        case 115 =>
          do {
            movePieceDown()
          } while (piecePositions.nonEmpty)
        // case 119 => rotate()
      }
    }

    setInterval(gameSpeed) {
      movePieceDown()
    }
  }
}

package example.game

import scala.util.Random
import scala.scalajs.js.timers.setInterval

class Game {
  val nGameRows: Int = 22
  val nGameCols: Int = 10
  val nNextPieceRows: Int = 5
  val nNextPieceCols: Int = 5

  // FIXME temporary: should change with time
  val gameSpeed: Int = 500

  // A position in a grid on the form (row, col)
  type Position = (Int, Int)

  def randomPiece(): Piece = Random.shuffle(List(Bar, InvL, L, S, Square, T, Z)).head

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

  def initGamePiecePositions(piece: Piece): List[Position] = {
    val shape = piece.shape()
    piecePositions(shape, (0, nGameCols / 2 - shape.head.length / 2))
  }

  def nextPiecePositions(piece: Piece): List[Position] = {
    val shape = piece.shape()
    piecePositions(shape, (nNextPieceRows / 2 - shape.length / 2 , nNextPieceCols / 2 - shape.head.length / 2))
  }

  def fall(gameGrid: Array[Array[Boolean]], positions: List[Position]): List[Position] = {
    val newPositions: List[Position] = positions.map(p => (p._1 + 1, p._2))

    // piece collided with bottom or other piece
    if (newPositions.exists(p => p._1 >= nGameRows || (gameGrid(p._1)(p._2) && !positions.contains(p)))) {
      return List()
    }

    updateGridAtPositions(gameGrid, positions, false)
    updateGridAtPositions(gameGrid, newPositions, true)

    newPositions
  }

  def updateGridAtPositions(grid: Array[Array[Boolean]], positions: List[Position], value: Boolean): Unit = {
    for (pos <- positions) {
      grid(pos._1)(pos._2) = value
    }
  }

  def run(): Unit = {
    val userGB: GameBox = new GameBox("user-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)
    val opponentBG: GameBox = new GameBox("opponent-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)

    var gameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
    var nextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

    var opponentGameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
    var opponentNextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

    var piece: Piece = randomPiece()
    var nextPiece: Piece = randomPiece()

    var positions: List[Position] = initGamePiecePositions(piece)
    var npPositions: List[Position] = nextPiecePositions(nextPiece)

    updateGridAtPositions(gameGrid, positions, true)
    updateGridAtPositions(nextPieceGrid, npPositions, true)

    setInterval(gameSpeed) {
      userGB.drawGame(gameGrid)
      userGB.drawNextPiece(nextPieceGrid)

      opponentBG.drawGame(opponentGameGrid)
      opponentBG.drawNextPiece(opponentNextPieceGrid)

      positions = fall(gameGrid, positions)

      if (positions.isEmpty) {
        piece = nextPiece
        nextPiece = randomPiece()

        updateGridAtPositions(nextPieceGrid, npPositions, false)

        positions = initGamePiecePositions(piece)
        npPositions = nextPiecePositions(nextPiece)

        updateGridAtPositions(gameGrid, positions, true)
        updateGridAtPositions(nextPieceGrid, npPositions, true)
      }
    }
  }
}

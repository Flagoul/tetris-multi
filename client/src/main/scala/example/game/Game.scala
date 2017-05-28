package example.game

import scala.util.Random

class Game {
  val nGameRows: Int = 22
  val nGameCols: Int = 10
  val nNextPieceRows: Int = 5
  val nNextPieceCols: Int = 5

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

  def move(piece: Piece, positions: List[Position]): Unit = ???

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
    val npPositions: List[Position] = nextPiecePositions(nextPiece)

    // TODO: tons of things here: move piece, handle rotate, redraw, ...

    for (pos <- positions) {
      gameGrid(pos._1)(pos._2) = true
    }

    for (pos <- npPositions) {
      nextPieceGrid(pos._1)(pos._2) = true
    }

    userGB.drawGame(gameGrid)
    userGB.drawNextPiece(nextPieceGrid)

    opponentBG.drawGame(opponentGameGrid)
    opponentBG.drawNextPiece(opponentNextPieceGrid)

    println("Game launched")
  }
}

package example.game

class Game {
  def randomPiece(): Piece = {
    Square // FIXME temporary
  }

  def move(piece: Piece): Unit = {
    ??? // TODO
  }

  def run(): Unit = {
    // FIXME put these values elsewhere ?
    val nGameRows: Int = 22
    val nGameCols: Int = 10
    val nNextPieceRows: Int = 5
    val nNextPieceCols: Int = 5

    val userGB: GameBox = new GameBox("user-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)
    val opponentBG: GameBox = new GameBox("opponent-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)

    var gameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
    var nextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

    var curPiece: Piece = randomPiece()
    var curRow: Int = 5 // FIXME
    var nextPiece: Piece = randomPiece()

    var opponentGameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
    var opponentNextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)

    // TODO: tons of things here: move piece, handle rotate, redraw, ...

    // FIXME: draw piece instead
    gameGrid(0)(5) = true
    gameGrid(1)(5) = true
    gameGrid(1)(6) = true
    gameGrid(2)(6) = true

    userGB.drawGame(gameGrid)
    userGB.drawNextPiece(nextPieceGrid)

    opponentBG.drawGame(opponentGameGrid)
    opponentBG.drawNextPiece(opponentNextPieceGrid)

    println("Game launched")
  }
}

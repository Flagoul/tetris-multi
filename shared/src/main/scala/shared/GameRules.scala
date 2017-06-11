package shared

/**
  * Regroups the game settings and rules.
  */
object GameRules {
  // The number of rows in the game grid.
  val nGameRows: Int = 22

  // The number of cols in the game grid.
  val nGameCols: Int = 10

  // The number of rows in the "next piece" grid.
  val nNextPieceRows: Int = 5

  // The number of cols in the "next piece" grid.
  val nNextPieceCols: Int = 5

  // The initial speed of the game, in milliseconds.
  val initGameSpeed: Long = 1000

  /**
    * Determines how many points to give when a piece is at the bottom of grid.
    *
    * The points are determined according to number of blocks above piece, the number of lines
    * completed with it and the game speed.
    *
    * @param nBlocksAbove The number of blocks above the fallen piece.
    * @param nLinesCompleted The number of lines completed on piece down.
    * @param gameSpeed The current speed of the game.
    * @return The number of points.
    */
  def pointsForPieceDown(nBlocksAbove: Int, nLinesCompleted: Int, gameSpeed: Long): Long = {
    val linePoints = nLinesCompleted match {
      case 1 => 40
      case 2 => 100
      case 3 => 300
      case 4 => 1200
      case _ => 0
    }
    nBlocksAbove + linePoints * ((initGameSpeed - gameSpeed)/100 + 1)
  }

  /**
    * Determines how many lines to send to opponent according to how many lines were completed.
    *
    * @param n The number of lines completed.
    * @return How many lines to send.
    */
  def numLinesToSend(n: Int): Int = n match {
    case 1 | 2 | 3 => n - 1
    case _ => n
  }

  /**
    * Determines the next speed of the game according to the current one and the number of lines that were completed
    * when the current piece is down.
    *
    * @param gameSpeed The current game speed.
    * @param nLinesCompleted How many lines there are.
    * @return The next speed.
    */
  def nextSpeed(gameSpeed: Long, nLinesCompleted: Int): Long = {
    if (nLinesCompleted <= 0) gameSpeed
    else nextSpeed(gameSpeed - (gameSpeed * 0.03).toLong, nLinesCompleted - 1)
  }
}

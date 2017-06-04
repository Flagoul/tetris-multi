package shared

object GameRules {
  val nGameRows: Int = 22
  val nGameCols: Int = 10
  val nNextPieceRows: Int = 5
  val nNextPieceCols: Int = 5
  val initGameSpeed: Long = 1000

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

  def nextSpeed(gameSpeed: Long): Long = {
    gameSpeed - (gameSpeed * 0.03).toLong
  }
}

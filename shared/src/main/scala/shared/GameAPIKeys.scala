package shared

/**
  * Keys of values sent by the server to the player in a json object.
  */
object GameAPIKeys {
  val error = "error"
  val opponentUsername = "opponentUsername"
  val ready: String = "ready"
  val action: String = "action"
  val gameGrid: String = "gameGrid"
  val nextPieceGrid: String = "nextPieceGrid"
  val piecePositions: String = "piecePositions"
  val opponent: String = "opponent"
  val points: String = "points"
  val piecesPlaced: String = "piecesPlaces"
  val won: String = "won"
  val draw: String = "draw"
}

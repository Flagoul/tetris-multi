package models

import java.sql.Timestamp

import managers.DBWrapper.api._
import slick.lifted.ProvenShape


/**
  * Representation of the UserSession model.
  *
  * @param id unique identifier of the model
  * @param winnerId id of the winner
  * @param winnerScore score marked by the winner
  * @param winnerPieces number of pieces put by the winner
  * @param loserId id of the loser
  * @param loserScore score marked by the loser
  * @param loserPieces number of pieces put by the loser
  * @param duration time spent in the game
  * @param timestamp at which the result was saved
  */
case class Result(id: Option[Long],
                  winnerId: Long, winnerScore: Long, winnerPieces: Long,
                  loserId: Long, loserScore: Long, loserPieces: Long,
                  duration: Long, timestamp: Option[Timestamp])
  extends AbstractModel(id) {
}


/**
  * Table in which to store game results.
  *
  * @param tag to give to the table
  */
class ResultTable(tag: Tag) extends AbstractTable[Result](tag, "results") {
  /**
    * Table containing users for joining
    */
  val users: TableQuery[UserTable] = TableQuery[UserTable]

  /**
    * Column containing the winner's id
    *
    * @return the column containing the winner's id
    */
  def winnerId: Rep[Long] = column[Long]("winner_id")

  /**
    * Column containing the first winner's score
    *
    * @return the column containing the winner's score
    */
  def winnerScore: Rep[Long] = column[Long]("winner_score")

  /**
    * Column containing the winner's number of pieces
    *
    * @return the column containing the winner's pieces
    */
  def winnerPieces: Rep[Long] = column[Long]("winner_pieces")

  /**
    * Column containing the loser's id
    *
    * @return the column containing the loser's id
    */
  def loserId: Rep[Long] = column[Long]("loser_id")

  /**
    * Column containing the loser's score
    *
    * @return the column containing the loser's score
    */
  def loserScore: Rep[Long] = column[Long]("loser_score")

  /**
    * Column containing the loser's number of pieces
    *
    * @return the column containing the loser's pieces
    */
  def loserPieces: Rep[Long] = column[Long]("loser_pieces")

  /**
    * Column containing the duration of the game
    *
    * @return the column containing the duration of the game
    */
  def duration: Rep[Long] = column[Long]("duration")

  /**
    * Column containing the timestamp at which the game was recorded
    *
    * @return the column containing the timestamp at which the game was recored
    */
  def timestamp: Rep[Timestamp] = column[Timestamp]("timestamp")

  override def * : ProvenShape[Result] =
    (id.?, winnerId, winnerScore, winnerPieces, loserId, loserScore, loserPieces, duration, timestamp.?) <>
      (Result.tupled, Result.unapply)
}

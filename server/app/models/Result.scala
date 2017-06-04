package models

import managers.DBWrapper.api._
import slick.lifted.ProvenShape


/**
  * Representation of the UserSession model.
  *
  * @param id unique identifier of the model
  * @param player1Id id of the first player
  * @param player1Score score marked by the first player
  * @param player1Pieces number of pieces put by the first player
  * @param player2Id id of the second player
  * @param player2Score score marked by the second player
  * @param player2Pieces number of pieces put by the second player
  * @param time time spent in the game
  */
case class Result(id: Option[Long],
                  player1Id: Long, player1Score: Long, player1Pieces: Long,
                  player2Id: Long, player2Score: Long, player2Pieces: Long,
                  time: Long)
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
    * Column containing the first user's id
    *
    * @return the column containing the first user's id
    */
  def player1Id: Rep[Long] = column[Long]("player_1_id")

  /**
    * Column containing the first user's score
    *
    * @return the column containing the first user's score
    */
  def player1Score: Rep[Long] = column[Long]("player_1_score")

  /**
    * Column containing the first user's number of pieces
    *
    * @return the column containing the first user's pieces
    */
  def player1Pieces: Rep[Long] = column[Long]("player_1_pieces")

  /**
    * Column containing the second user's id
    *
    * @return the column containing the second user's id
    */
  def player2Id: Rep[Long] = column[Long]("player_2_id")

  /**
    * Column containing the second user's score
    *
    * @return the column containing the second user's score
    */
  def player2Score: Rep[Long] = column[Long]("player_2_score")

  /**
    * Column containing the second user's number of pieces
    *
    * @return the column containing the second user's pieces
    */
  def player2Pieces: Rep[Long] = column[Long]("player_2_pieces")

  /**
    * Column containing the time spent in the game
    *
    * @return the column containing the time spend in the game
    */
  def time: Rep[Long] = column[Long]("time")

  override def * : ProvenShape[Result] =
    (id.?, player1Id, player1Score, player1Pieces, player2Id, player2Score, player2Pieces, time) <>
      (Result.tupled, Result.unapply)
}

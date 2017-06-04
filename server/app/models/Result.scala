package models

import java.sql.Timestamp

import managers.DBWrapper.api._
import slick.lifted.{ForeignKeyQuery, ProvenShape}


/**
  * Representation of the UserSession model.
  *
  * @param id unique identifier of the model
  * @param uuid to store on the http session to access the session
  * @param expiration date of expiration for this session
  * @param user_id for which this session is valid
  */
case class Result(id: Option[Long],
                  player1Id: Long, player1Score: Long, player1Pieces: Long,
                  player2Id: Long, player2Score: Long, player2Pieces: Long,
                  time: Timestamp)
  extends AbstractModel(id) {
}


class ResultTable(tag: Tag) extends AbstractTable[Result](tag, "results") {
  /**
    * Table containing users for joining
    */
  val users: TableQuery[UserTable] = TableQuery[UserTable]

  def player1Id: Rep[Long] = column[Long]("player_1_id")
  def player1Score: Rep[Long] = column[Long]("player_1_score")
  def player1Pieces: Rep[Long] = column[Long]("player_1_pieces")

  def player2Id: Rep[Long] = column[Long]("player_2_id")
  def player2Score: Rep[Long] = column[Long]("player_2_score")
  def player2Pieces: Rep[Long] = column[Long]("player_2_pieces")

  def time: Rep[Timestamp] = column[Timestamp]("time")

  def player1: ForeignKeyQuery[UserTable, User] =
    foreignKey("player1_id", player1Id, users)(_.id)

  def player2: ForeignKeyQuery[UserTable, User] =
    foreignKey("player2_id", player2Id, users)(_.id)

  override def * : ProvenShape[Result] =
    (id.?, player1Id, player1Score, player1Pieces, player2Id, player2Score, player2Pieces, time) <>
      (Result.tupled, Result.unapply)
}

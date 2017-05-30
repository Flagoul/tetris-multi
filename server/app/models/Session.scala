package models

import java.sql.Timestamp

import slick.jdbc.MySQLProfile.api._


case class Session(id: Option[Long], uuid: String, expiration: Option[Timestamp], user_id: Long) extends AbstractModel(id)


class SessionTable(tag: Tag) extends AbstractTable[Session](tag, "sessions") {
  val users = TableQuery[UserTable]

  def uuid = column[String]("uuid", O.Unique)
  def expiration = column[Timestamp]("expiration")
  def user_id = column[Long]("user_id")

  def user = foreignKey("user_fk", user_id, users)(_.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)

  override def * = (id.?, uuid, expiration.?, user_id) <> (Session.tupled, Session.unapply)
}

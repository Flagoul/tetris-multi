package models

import managers.DBWrapper.api._



case class User(id: Option[Long], username: String, password: String) extends AbstractModel(id)


class UserTable(tag: Tag) extends AbstractTable[User](tag, "users") {
  def username = column[String]("username", O.Unique)
  def password = column[String]("password")

  override def * = (id.?, username, password) <> (User.tupled, User.unapply)
}

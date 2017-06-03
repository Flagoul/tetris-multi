package models

import managers.DBWrapper.api._
import slick.lifted.ProvenShape


/**
  * Representation of the User model.
  *
  * @param id unique identifier of the model
  * @param username of the user
  * @param password identifying the user
  */
case class User(id: Option[Long], username: String, password: String) extends AbstractModel(id)


/**
  * Table in which to store users
  *
  * @param tag to give to the table
  */
class UserTable(tag: Tag) extends AbstractTable[User](tag, "users") {
  /**
    * Column containing the username
    *
    * @return the column for username
    */
  def username: Rep[String] = column[String]("username", O.Unique)

  /**
    * Column containing the password
    *
    * @return the column for the password
    */
  def password: Rep[String] = column[String]("password")

  override def * : ProvenShape[User] = (id.?, username, password) <> (User.tupled, User.unapply)
}

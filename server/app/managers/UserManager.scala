package managers

import javax.inject._

import models.{User, UserTable}
import org.mindrot.jbcrypt.BCrypt
import play.api.Application
import DBWrapper.api._

import scala.concurrent.{ExecutionContext, Future}


/**
  * This manager handles users.
  *
  * @param appProvider to get access to the database configuration.
  * @param ec execution context in which to execute
  */
@Singleton
class UserManager @Inject()(val appProvider: Provider[Application])
                           (implicit ec: ExecutionContext) extends AbstractManager[User, UserTable] {

  /**
    * Query builder to use in the manager.
    */
  protected val query: TableQuery[UserTable] = TableQuery[UserTable]

  /**
    * Copy the given user, giving it a new id.
    *
    * @param user to update
    * @param id to give to the session
    * @return copy of the provided user with the new id
    */
  override protected def withUpdatedId(user: User, id: Long): User = user.copy(id = Some(id))

  /**
    * Insert the given user in the database and returns it with its new id.
    *
    * This method automatically hashes the user's password before putting it in the  database.
    *
    * @param u user to save to the database
    * @return the user inserted with its new id
    */
  override def create(u: User): Future[User] = super.create(u.copy(password =
    // FIXME : salt round should be put in configuration
    BCrypt.hashpw(u.password, BCrypt.gensalt(12))))

  /**
    * Authenticate the given user.
    *
    * @param user to authenticate
    * @return the user if the password was valid, otherwise None
    */
  def authenticate(user: User): Future[Option[User]] = {
    db.run(
      query.filter(_.username === user.username)
        .result.map(_.headOption.filter(u => BCrypt.checkpw(user.password, u.password)))
    )
  }
}

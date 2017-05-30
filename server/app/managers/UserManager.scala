package managers

import javax.inject._

import models.{User, UserTable}
import org.mindrot.jbcrypt.BCrypt
import play.api.Application
import slick.lifted.TableQuery
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future


@Singleton
class UserManager @Inject()(val appProvider: Provider[Application]) extends AbstractManager[User, UserTable] {
  protected val query: TableQuery[UserTable] = TableQuery[UserTable]

  override protected def withUpdatedId(user: User, id: Long): User = {
    user.copy(id = Some(id))
  }

  override def create(u: User): Future[User] = super.create(u.copy(password =
    // FIXME : salt round should be put in configuration
    BCrypt.hashpw(u.password, BCrypt.gensalt(12))))

  def authenticate(user: User): Future[Option[User]] = {
    db.run(
      query.filter(_.username === user.username)
        .result.map(_.headOption.filter(u => BCrypt.checkpw(user.password, u.password)))
    )
  }
}

package managers

import javax.inject._

import models.{User, UserTable}
import play.api.Application
import slick.lifted.TableQuery
import slick.jdbc.MySQLProfile.api._


import scala.concurrent.Future


@Singleton
class UserManager @Inject()(val appProvider: Provider[Application]) extends AbstractManager[User, UserTable] {
  protected val query: TableQuery[UserTable] = TableQuery[UserTable]

  override protected def withUpdatedId(user: User, id: Long): User = {
    user.copy(id = Some(id))
  }
}

package managers

import javax.inject._

import models.{Session, SessionTable}
import play.api.Application
import slick.lifted.TableQuery


@Singleton
class SessionManager @Inject()(val appProvider: Provider[Application]) extends AbstractManager[Session, SessionTable] {
  protected val query: TableQuery[SessionTable] = TableQuery[SessionTable]

  override protected def withUpdatedId(session: Session, id: Long): Session = {
    session.copy(id = Some(id))
  }
}

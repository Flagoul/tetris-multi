package managers

import java.time.Instant
import java.util.UUID
import javax.inject._

import models.{SessionTable, UserSession}
import play.api.{Application, mvc}
import play.api.mvc.Request
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class SessionManager @Inject()(val appProvider: Provider[Application])
                              (implicit ec: ExecutionContext) extends AbstractManager[UserSession, SessionTable] {

  protected val query: TableQuery[SessionTable] = TableQuery[SessionTable]

  override protected def withUpdatedId(session: UserSession, id: Long): UserSession = {
    session.copy(id = Some(id))
  }

  def registerSession[T](userId: Long)(implicit request: Request[T]): Future[mvc.Session] = {
    create(UserSession(None, UUID.randomUUID.toString, None, userId))
      .map(session => request.session + ("uuid" -> session.uuid.toString))
  }

  def getSession[T](implicit request: Request[T]): Future[Option[UserSession]] = {
    request.session.
      get("uuid").map(uuid =>
        db.run(query.filter(_.uuid === uuid).result.headOption).flatMap({
          case Some(session) if session.expiration.get.toInstant.getEpochSecond < Instant.now.getEpochSecond =>
            delete(session.id.get).map(_ => None)
          case Some(session) => update(session.id.get, session)
          case None => Future.successful(None)
        })
      )
      .getOrElse(Future.successful(None))
  }
}

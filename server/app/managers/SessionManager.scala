package managers

import java.time.Instant
import java.util.UUID
import javax.inject._

import models.{SessionTable, UserSession}
import play.api.{Application, mvc}
import play.api.mvc.{Request, RequestHeader}
import DBWrapper.api._

import scala.concurrent.{ExecutionContext, Future}


/**
  * This manager handles user sessions.
  *
  * @param appProvider to get access to the database configuration.
  * @param ec execution context in which to execute
  */
@Singleton
class SessionManager @Inject()(val appProvider: Provider[Application])
                              (implicit ec: ExecutionContext) extends AbstractManager[UserSession, SessionTable] {

  /**
    * Query builder to use in the manager.
    */
  protected val query: TableQuery[SessionTable] = TableQuery[SessionTable]

  /**
    * Copy the given session, giving it a new id.
    *
    * @param session to update
    * @param id to give to the session
    * @return copy of the provided session with the new id
    */
  override protected def withUpdatedId(session: UserSession, id: Long): UserSession = {
    session.copy(id = Some(id))
  }

  /**
    * Register a new session for the given user id.
    *
    * @param userId for which to create a session
    * @param request in which the session must be registered
    * @tparam T request type
    * @return the newly registered session
    */
  def registerSession[T](userId: Long)(implicit request: Request[T]): Future[mvc.Session] = {
    create(UserSession(None, UUID.randomUUID.toString, None, userId))
      .map(session => request.session + ("uuid" -> session.uuid.toString))
  }

  /**
    * Get the session related to the given request it it is still valid.
    *
    * @param request for which to get the session
    * @tparam T request type
    * @return the session if it exists or None
    */
  def getSession[T](implicit request: RequestHeader): Future[Option[UserSession]] = {
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

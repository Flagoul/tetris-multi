package controllers.authentication

import managers.SessionManager
import models.UserSession
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}


class AuthenticatedRequest[A](val userSession: Option[UserSession], request: Request[A])
  extends WrappedRequest[A](request)


trait AuthenticatedController extends Controller {
  implicit val sessions: SessionManager
  implicit val ec: ExecutionContext

  object Action extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      sessions.getSession(request).flatMap(session => block(new AuthenticatedRequest[A](session, request)))
    }
  }

  object AuthenticatedAction extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      sessions.getSession(request).flatMap({
        case None => Future.successful(Redirect("/login"))
        case session => block(new AuthenticatedRequest[A](session, request))
      })
    }
  }

  object UnAuthenticatedAction extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      sessions.getSession(request).flatMap({
        case None => block(new AuthenticatedRequest[A](None, request))
        case _ => Future.successful(Redirect("/"))
      })
    }
  }
}





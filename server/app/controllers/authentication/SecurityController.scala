package controllers.authentication

import managers.SessionManager
import models.UserSession
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}


/**
  * Request wrapper that may contain a user session.
  *
  * @param userSession if it exists
  * @param request to wrap
  * @tparam A the body content type
  */
class AuthenticatedRequest[A](val userSession: Option[UserSession], request: Request[A])
  extends WrappedRequest[A](request)


/**
  * Controller aware of sessions.
  *
  * This class provides three different actions depending on the user's sessions :
  *
  *   - Action that passes an AuthenticatedRequest but does not take any action
  *   - AuthenticatedAction that redirects the user to the login page if it is not logged in
  *   - UnAuthenticatedAction that redirects the user to the root if it it not logged in
  */
abstract class SecurityController extends Controller {

  /**
    * Session manager
    */
  implicit val sessions: SessionManager

  /**
    * Execution context in which the controller runs
    */
  implicit val ec: ExecutionContext

  /**
    * Action to take when the user is already logged in and should not be.
    *
    * @return a future of Result that is returned directly
    */
  def onLoggedIn(): Future[Result] = Future.successful(Redirect("/"))

  /**
    * Action to take when the user is not logged in and should be.
    *
    * @return a future of Result that is returned directly
    */
  def onNotLoggedIn(): Future[Result] = Future.successful(Redirect("/login"))

  /**
    * This action wraps the request in an AuthenticatedRequest to give access to the user's session if it exists.
    */
  object Action extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      sessions.getSession(request).flatMap(session => block(new AuthenticatedRequest[A](session, request)))
    }
  }

  /**
    * This action forces the user to be authenticated to be resolved correctly.
    *
    * Otherwise, it executes the `onNotLoggedIn` of the controller.
    *
    * It also wraps the request in an AuthenticatedRequest to give access to the user's session
    */
  object AuthenticatedAction extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      sessions.getSession(request).flatMap({
        case None => onNotLoggedIn()
        case session => block(new AuthenticatedRequest[A](session, request))
      })
    }
  }

  /**
    * This action forces the user to not be authenticated to be resolved correctly.
    *
    * Otherwise, it executes the `onLoggedIn` of the controller.
    *
    * It also wraps the request in an AuthenticatedRequest to give access to the user's session
    * (which is always None here)
    */
  object UnAuthenticatedAction extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      sessions.getSession(request).flatMap({
        case None => block(new AuthenticatedRequest[A](None, request))
        case _ => onLoggedIn()
      })
    }
  }
}





package controllers

import javax.inject.Inject

import controllers.authentication.SecurityController
import managers.SessionManager

import scala.concurrent.ExecutionContext


/**
  * Root controller of the application.
  *
  * @param sessions manager for the sessions
  * @param ec execution context in which to run
  */
class Application @Inject()(val sessions: SessionManager, val ec: ExecutionContext) extends SecurityController {
  /**
    * Redirect to the index page.
    *
    * @return the index page
    */
  def index = Action { implicit request =>
    request.userSession match {
      case None => Redirect(routes.Session.login())
      case Some(_) => Redirect(routes.Stats.index())
    }
  }
}

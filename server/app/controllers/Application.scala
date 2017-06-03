package controllers

import javax.inject.Inject

import controllers.authentication.SecurityController
import managers.SessionManager
import shared.SharedMessages

import scala.concurrent.ExecutionContext


/**
  * Root controller of the application.
  *
  * @param sessions manager for the sessions
  * @param ec execution context in which to run
  */
class Application @Inject()(val sessions: SessionManager, val ec: ExecutionContext) extends SecurityController {
  /**
    * Get the index page of the application.
    *
    * @return the index page
    */
  def index = Action { implicit request =>
    Ok(views.html.index(SharedMessages.itWorks))
  }
}

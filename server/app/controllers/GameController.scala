package controllers

import javax.inject.Inject

import controllers.authentication.SecurityController
import managers.SessionManager

import scala.concurrent.ExecutionContext

/**
  * Temporary controller rendering game, for development purpose.
  */
class GameController @Inject() (val sessions: SessionManager, val ec: ExecutionContext) extends SecurityController {

  def index() = AuthenticatedAction { implicit request =>
    Ok(views.html.game())
  }
}

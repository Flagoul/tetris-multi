package controllers

import javax.inject.Inject

import controllers.authentication.AuthenticatedController
import managers.SessionManager
import play.api.mvc.Controller

import scala.concurrent.ExecutionContext

/**
  * Temporary controller rendering game, for development purpose.
  */
class Game @Inject() (val sessions: SessionManager, val ec: ExecutionContext) extends Controller with AuthenticatedController {
  def index() = AuthenticatedAction { implicit request =>
    Ok(views.html.game())
  }
}

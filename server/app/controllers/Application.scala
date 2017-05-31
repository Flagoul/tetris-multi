package controllers

import javax.inject.Inject

import controllers.authentication.AuthenticatedController
import managers.SessionManager
import play.api.mvc._
import shared.SharedMessages

import scala.concurrent.ExecutionContext

class Application @Inject()(val sessions: SessionManager, val ec: ExecutionContext)
  extends Controller with AuthenticatedController {

  def index = Action { implicit request =>
    Ok(views.html.index(SharedMessages.itWorks))
  }
}

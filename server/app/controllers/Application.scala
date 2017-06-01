package controllers

import javax.inject.Inject

import controllers.authentication.SecurityController
import managers.SessionManager
import shared.SharedMessages

import scala.concurrent.ExecutionContext

class Application @Inject()(val sessions: SessionManager, val ec: ExecutionContext) extends SecurityController {

  def index = Action { implicit request =>
    Ok(views.html.index(SharedMessages.itWorks))
  }
}

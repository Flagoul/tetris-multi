package controllers


import javax.inject.Inject

import controllers.authentication.SecurityController
import managers.{ResultManager, SessionManager}

import scala.concurrent.ExecutionContext


class Stats @Inject() (val sessions: SessionManager, val results: ResultManager, val ec: ExecutionContext) extends SecurityController {
  def index() = AuthenticatedAction { implicit request =>
    Ok(views.html.stats())
  }
}

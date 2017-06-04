package controllers


import javax.inject.Inject

import controllers.authentication.SecurityController
import managers.{ResultManager, SessionManager, UserManager}

import scala.concurrent.ExecutionContext


class Stats @Inject() (val users: UserManager, val sessions: SessionManager, val results: ResultManager)
                      (implicit val ec: ExecutionContext) extends SecurityController {

  def index() = Action { implicit request =>
    request.userSession match {
      case None => Redirect(routes.Application.index())
      case Some(session) => Redirect(routes.Stats.userStats(session.user_id))
    }
  }

  def userStats(id: Long) = Action.async { implicit request =>
    users.get(id).map({
      case None => NotFound("Not Found")
      case Some(user) => Ok(views.html.stats(user))
    })
  }
}

package controllers

import javax.inject.Inject

import controllers.authentication.AuthenticatedController
import managers.SessionManager
import play.api.mvc.Controller

import scala.concurrent.ExecutionContext

class Logout @Inject() (val sessions: SessionManager, implicit val ec: ExecutionContext)
  extends Controller with AuthenticatedController {

  def index() = AuthenticatedAction.async { implicit request =>
    sessions.delete(request.userSession.get.id.get).map(
      _ => Redirect("/login").withSession(request.session - "uuid")
    )
  }
}

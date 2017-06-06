package controllers

import javax.inject.Inject

import controllers.authentication.SecurityController
import managers.{SessionManager, UserManager}
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.ExecutionContext

class GameController @Inject() (val sessions: SessionManager, users: UserManager)(implicit val ec: ExecutionContext) extends SecurityController {

  def index(): Action[AnyContent] = AuthenticatedAction.async { implicit request =>
    users.get(request.userSession.get.userId).map({
      case Some(user) => Ok(views.html.game(user.username))
    })
  }
}

package controllers

import javax.inject.Inject

import controllers.authentication.SecurityController
import managers.{SessionManager, UserManager}
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.ExecutionContext

/**
  * The Game controller.
  *
  * This is used to display the game page.
  *
  * @param sessions The manager to handle user sessions.
  * @param users The manager to handle user sessions.
  * @param ec The execution context in which to run.
  */
class GameController @Inject() (val sessions: SessionManager, users: UserManager)(implicit val ec: ExecutionContext) extends SecurityController {

  /**
    * Get the page for the game.
    * @return The game page.
    */
  def index(): Action[AnyContent] = AuthenticatedAction.async { implicit request =>
    users.get(request.userSession.get.userId).map({
      case Some(user) => Ok(views.html.game(user.username))
    })
  }
}

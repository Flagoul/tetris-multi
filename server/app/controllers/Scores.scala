package controllers


import javax.inject.Inject

import controllers.authentication.SecurityController
import managers.{ResultManager, SessionManager, UserManager}
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.ExecutionContext


/**
  * Scores controller
  *
  * This controller is used to display highest scores for every players.
  *
  * @param users manager to handle users
  * @param sessions manager to handle sessions
  * @param results manager to handle user's results
  * @param ec execution context in which to run
  */
class Scores @Inject() (val users: UserManager, val sessions: SessionManager, val results: ResultManager)
                      (implicit val ec: ExecutionContext) extends SecurityController {

  /**
    * Get the page for the highest scores
    *
    * @return the scores pages.
    */
  def index(): Action[AnyContent] = Action.async { implicit request =>
    results.getHighestScores().map(scores => Ok(views.html.scores(scores)))
  }
}

package controllers


import javax.inject.Inject

import controllers.authentication.SecurityController
import managers.{ResultManager, SessionManager, UserManager}
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.{ExecutionContext, Future}


/**
  * Statistics controller
  *
  * @param users manager to handle users
  * @param sessions manager to handle sessions
  * @param results manager to handle user's results
  * @param ec execution context in which to run
  */
class Stats @Inject() (val users: UserManager, val sessions: SessionManager, val results: ResultManager)
                      (implicit val ec: ExecutionContext) extends SecurityController {

  /**
    * Get the main page for statistics.
    *
    * This returns to the logged in user's statistics.
    *
    * @return the stats page for the logged in user.
    */
  def index() = Action { implicit request =>
    request.userSession match {
      case None => Redirect(routes.Application.index())
      case Some(session) => Redirect(routes.Stats.userStats(session.user_id))
    }
  }

  /**
    * Get the statistics page for the given user id.
    *
    * @return the stats page for the requested user.
    */
  def userStats(id: Long): Action[AnyContent] = Action.async { implicit request =>
    users.get(id).flatMap({
      case None => Future.successful(NotFound("Not Found"))
      case Some(user) =>
        val stats = for {
          games <- results.getGames(user.id)
          gamesWon <- results.getNumberOfGamesWonFor(user.id.get)
          piecesPlayed <- results.getNumberOfPiecesPlayedFor(user.id.get)
          maximumPoints <- results.getMaximumPointsFor(user.id.get)
          timePlayed <- results.getTimePlayedFor(user.id.get)
        } yield (games, gamesWon, piecesPlayed, maximumPoints, timePlayed)

        stats.map(results =>
          Ok(views.html.stats(user, results._1, results._2, results._3, results._4, results._5)))
    })
  }
}

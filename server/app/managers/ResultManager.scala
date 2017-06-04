package managers

import javax.inject._

import managers.DBWrapper.api._
import models.{Result, ResultTable}
import play.api.Application

import scala.concurrent.{ExecutionContext, Future}


/**
  * This manager handles game results.
  *
  * @param appProvider tog et access to the database configuration
  * @param ec execution context in which to execute
  */
@Singleton
class ResultManager @Inject()(val appProvider: Provider[Application])
                             (implicit ec: ExecutionContext) extends AbstractManager[Result, ResultTable] {

  /**
    * Query builder to use in the manager.
    */
  protected val query: TableQuery[ResultTable] = TableQuery[ResultTable]

  /**
    * Copy the given result, giving it a new id.
    *
    * @param result to update
    * @param id to give to the result
    * @return copy of the provided result with the new id
    */
  override protected def withUpdatedId(result: Result, id: Long): Result = result.copy(id = Some(id))

  /**
    * Filter the game results to include the given user.
    *
    * @param playerId for which to filter the results.
    * @return results, filtered for the given user.
    */
  private def filterGamesFor(playerId: Long) = query.filter(r => r.player1Id === playerId || r.player2Id === playerId)

  /**
    * Get all results for the given user.
    *
    * @param playerId player id
    * @return sequence of all results.
    */
  def getGamesFor(playerId: Long): Future[Seq[Result]] = {
    db.run(filterGamesFor(playerId).result)
  }

  /**
    * Get the number of games won for the givne user.
    *
    * @param playerId player id
    * @return number of won games
    */
  def getNumberOfGamesWonFor(playerId: Long): Future[Int] = {
    db.run(query.filter(r =>
      (r.player1Id === playerId && r.player1Score > r.player2Score) ||
        (r.player2Id === playerId && r.player2Score > r.player1Score)
    ).length.result)
  }

  /**
    * Get the number of pieces put by the given player.
    *
    * @param playerId player id
    * @return number of pieces put by the player
    */
  def getNumberOfPiecesPlayedFor(playerId: Long): Future[Long] = {
    db.run(filterGamesFor(playerId).map({ result =>
      Case If result.player1Id === playerId Then result.player1Pieces Else result.player2Pieces
    }).sum.result).map(_.getOrElse(0))
  }

  /**
    * Get the maximum number of points marked by the given user.
    *
    * @param playerId player id
    * @return points marked by the user
    */
  def getMaximumPointsFor(playerId: Long): Future[Long] = {
    db.run(filterGamesFor(playerId).map({ result =>
      Case If result.player1Id === playerId Then result.player1Score Else result.player2Score
    }).max.result).map(_.getOrElse(0))
  }

  /**
    * Get the total time a user has played.
    *
    * @param playerId player id
    * @return total time played by the given user.
    */
  def getTimePlayedFor(playerId: Long): Future[Long] = {
    db.run(filterGamesFor(playerId).map(_.time).sum.result).map(_.getOrElse(0))
  }

}

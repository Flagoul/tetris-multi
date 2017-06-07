package managers

import javax.inject._

import managers.DBWrapper.api._
import models.{Result, ResultTable, User, UserTable}
import play.api.Application

import scala.concurrent.{ExecutionContext, Future}


/**
  * This class encapsulates a result with both users concerned.
  *
  * @param result to encapsulate
  * @param winner first player in the result
  * @param loser second player in the result
  */
case class ResultsWithUser(result: Result, winner: User, loser: User)

/**
  * Score a user made
  *
  * @param user user to which the score applies
  * @param score score made by the player
  * @param pieces number of peices put by the player
  * @param duration duration of the game
  */
case class UserScore(user: User, score: Long, pieces: Long, duration: Long)


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

  protected val userQuery: TableQuery[UserTable] = TableQuery[UserTable]

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
  private def filterGamesFor(playerId: Long) = query.filter(r => r.winnerId === playerId || r.loserId === playerId)

  /**
    * Get all game results.
    *
    * @param playerId player id. If this is specified, will filter all results for the given user to be included.
    * @return sequence of all results.
    */
  def getGames(playerId: Option[Long] = None): Future[Seq[ResultsWithUser]] = {
    val q = playerId match {
      case None => query
      case Some(id) => filterGamesFor(id)
    }
    db.run(q.join(userQuery).on(_.winnerId === _.id).join(userQuery).on(_._1.loserId === _.id).result)
      .map(e => e.map(r => ResultsWithUser(r._1._1, r._1._2, r._2)))
  }


  /**
    * Get the number of games won for the given user.
    *
    * @param playerId player id
    * @return number of won games
    */
  def getNumberOfGamesWonFor(playerId: Long): Future[Int] =
    db.run(query.filter(r => r.winnerId === playerId).length.result)

  /**
    * Get the number of pieces put by the given player.
    *
    * @param playerId player id
    * @return number of pieces put by the player
    */
  def getNumberOfPiecesPlayedFor(playerId: Long): Future[Long] = {
    db.run(filterGamesFor(playerId).map({ result =>
      Case If result.winnerId === playerId Then result.winnerPieces Else result.loserPieces
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
      Case If result.winnerId === playerId Then result.winnerScore Else result.loserScore
    }).max.result).map(_.getOrElse(0))
  }

  /**
    * Get the total time a user has played.
    *
    * @param playerId player id
    * @return total time played by the given user.
    */
  def getTimePlayedFor(playerId: Long): Future[Long] = {
    db.run(filterGamesFor(playerId).map(_.duration).sum.result).map(_.getOrElse(0))
  }

  /**
    * Get the highest score for each player.
    *
    * @return list of scores for each players
    */
  def getHighestScores: Future[Seq[UserScore]] = {
    db.run(
      query.map(res => (res.winnerId, res.winnerScore, res.winnerPieces, res.duration))
        .union(query.map(res => (res.loserId, res.loserScore, res.loserPieces, res.duration)))
        .groupBy(_._1)
        .map({
          case (playerId, data) => (playerId, data.map(_._2).max, data.map(_._3).max, data.map(_._4).max)
        })
        .join(userQuery).on(_._1 === _.id)
        .result
    ).map(data => data.map(e => UserScore(e._2, e._1._2.getOrElse(0), e._1._3.getOrElse(0), e._1._4.getOrElse(0))))
  }
}

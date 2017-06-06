package game

import akka.actor.PoisonPill
import managers.ResultManager
import models.Result
import play.api.libs.json.Json
import shared.GameAPIKeys

import scala.collection.mutable

case class GameManager(results: ResultManager) {
  private var waitingPlayers: mutable.Queue[Player] = mutable.Queue()
  private var waitingIds: Set[Long] = Set()
  private var games: Map[Long, Game] = Map()

  def playerAlreadyInGame(implicit id: Long): Boolean = waitingIds.contains(id) || games.contains(id)

  def joinGame(player: Player): Unit = this.synchronized {
    val playerId = player.user.id.get

    if (waitingPlayers.isEmpty) {
      waitingPlayers += player
      waitingIds += playerId
      println("waiting")
    }
    else {
      val opponent = waitingPlayers.dequeue
      val opponentId = opponent.user.id.get

      waitingIds -= opponentId

      val game = new Game(player, opponent, this)

      games += (playerId -> game)
      games += (opponentId -> game)

      println("playing")
      player.out ! Json.obj(GameAPIKeys.opponentUsername -> opponent.user.username).toString()
      opponent.out ! Json.obj(GameAPIKeys.opponentUsername -> player.user.username).toString()
    }
  }

  def getGame(implicit id: Long): Game = games.getOrElse(id, null)

  def endGame(result: Result): Unit = {
    games -= result.player1Id
    games -= result.player2Id

    results.create(result)
  }
}

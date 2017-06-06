package game

import akka.actor.ActorRef
import managers.ResultManager
import models.Result
import play.api.libs.json.Json
import shared.GameAPIKeys

import scala.collection.mutable

case class GameManager(results: ResultManager) {
  private var waitingPlayers: mutable.LinkedHashMap[Long, Player] = mutable.LinkedHashMap()
  private var games: Map[Long, Game] = Map()

  def isPlayerWaiting(implicit id: Long): Boolean = waitingPlayers.contains(id)

  def removeWaitingPlayer(implicit id: Long): Unit = waitingPlayers -= id

  def isPlayerInGame(implicit id: Long): Boolean = games.contains(id)

  def putBackPlayerInGame(out: ActorRef)(implicit id: Long): Unit = {
    val game = games(id)
    game.putBackPlayerInGame(out)
  }

  def joinGame(player: Player): Unit = this.synchronized {
    val playerId = player.user.id.get

    if (waitingPlayers.isEmpty) {
      waitingPlayers += (playerId -> player)
      println("waiting")
    }
    else {
      val firstInMap = waitingPlayers.keySet.iterator.next
      val opponent = waitingPlayers(firstInMap)
      val opponentId = opponent.user.id.get

      waitingPlayers -= opponentId

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

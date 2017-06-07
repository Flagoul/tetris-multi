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

  def putBackPlayerInGame(player: Player)(implicit id: Long): Unit = {
    val game = games(id)

    if (game.everyoneReady()) {
      game.putBackPlayerInGame(player.out)
    }
    else {
      // FIXME make the player lose but use a timeout as well, not only check here at reconnection
      game.lose
      joinGame(player)
    }
  }

  def joinGame(player: Player)(implicit id: Long): Unit = this.synchronized {
    if (isPlayerInGame) {
      println("putting back player in game")
      putBackPlayerInGame(player)
      return
    }

    if (isPlayerWaiting) {
      println("Removing player and putting back in waiting list")
      removeWaitingPlayer
    }

    if (waitingPlayers.isEmpty) {
      waitingPlayers += (id -> player)
      println("waiting")
    }
    else {
      val firstInMap = waitingPlayers.keySet.iterator.next
      val opponent = waitingPlayers(firstInMap)
      val opponentId = opponent.user.id.get

      waitingPlayers -= opponentId

      val game = new Game(player, opponent, this)

      games += (id -> game)
      games += (opponentId -> game)

      println("playing")
      player.out ! Json.obj(GameAPIKeys.opponentUsername -> opponent.user.username).toString()
      opponent.out ! Json.obj(GameAPIKeys.opponentUsername -> player.user.username).toString()
    }
  }

  def getGame(implicit id: Long): Option[Game] = games.get(id)

  def endGame(result: Result): Unit = {
    games -= result.player1Id
    games -= result.player2Id

    results.create(result)
  }
}

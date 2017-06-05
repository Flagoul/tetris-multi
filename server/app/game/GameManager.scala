package game

import managers.ResultManager
import models.Result
import play.api.libs.json.Json
import shared.GameAPIKeys

import scala.collection.mutable

case class GameManager(results: ResultManager) {
  private var waitingPlayers: mutable.Queue[Player] = mutable.Queue()
  private var games: Map[Long, Game] = Map()

  def joinGame(player: Player): Unit = this.synchronized {
    if (waitingPlayers.isEmpty) {
      waitingPlayers += player
      println("waiting")
    } else {
      val opponent = waitingPlayers.dequeue
      val game = new Game(player, opponent, this)

      games += (player.user.id.get -> game)
      games += (opponent.user.id.get -> game)

      println("playing")
    }

    player.out ! Json.stringify(Json.obj(GameAPIKeys.id -> player.user.username))
  }

  def getGame(implicit id: Long): Game = games(id)

  def endGame(result: Result): Unit = {
    games -= result.player1Id
    games -= result.player2Id

    results.create(result)
  }
}

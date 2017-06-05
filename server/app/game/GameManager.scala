package game

import play.api.libs.json.Json
import shared.GameAPIKeys

import scala.collection.mutable

case class GameManager() {
  private var waitingPlayers: mutable.Queue[Player] = mutable.Queue()
  private var games: Map[String, Game] = Map()

  def joinGame(player: Player): Unit = this.synchronized {
    if (waitingPlayers.isEmpty) {
      waitingPlayers += player
      println("waiting")
    } else {
      val opponent = waitingPlayers.dequeue
      val game = new Game(player, opponent, this)

      games += (player.id -> game)
      games += (opponent.id -> game)

      println("playing")
    }

    player.out ! Json.stringify(Json.obj(GameAPIKeys.id -> player.id))
  }

  def getGame(id: String): Game = games(id)

  def deleteGame(id: String): Unit = {
    games -= id
    println(s"There are now ${games.size} games")
  }
}

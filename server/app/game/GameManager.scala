package game

import managers.ResultManager
import models.Result
import play.api.libs.json.{JsBoolean, Json}
import shared.GameAPIKeys

import scala.collection.mutable

case class GameManager(results: ResultManager) {
  private var waitingPlayers: mutable.Queue[Player] = mutable.Queue()
  private var games: Map[Long, Game] = Map()


  def joinGame(player: Player): Unit = this.synchronized {
    if (waitingPlayers.isEmpty) {
      waitingPlayers += player
      println("waiting")
    }
    else {
      val opponent = waitingPlayers.dequeue
      val game = new Game(player, opponent, this)

      games += (player.user.id.get -> game)
      games += (opponent.user.id.get -> game)

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

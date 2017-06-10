package game

import akka.actor.{ActorRef, PoisonPill}
import managers.ResultManager
import models.Result
import play.api.Logger.logger
import play.api.libs.json.Json
import shared.{Actions, GameAPIKeys}

import scala.collection.mutable

case class GameManager(results: ResultManager) {
  private var players: Map[Long, Player] = Map()
  private var waitingPlayers: mutable.LinkedHashMap[Long, Player] = mutable.LinkedHashMap()
  private var games: Map[Long, Game] = Map()

  def joinGame(player: Player)(implicit id: Long): Unit = this.synchronized {
    if (isPlayerPlaying) {
      println(s"Putting back $id in game he was playing")
      games(id).putBackPlayerInGame(player.out)
      return
    }

    // FIXME remove after timer
    if (isPlayerInLobby) {
      makePlayerLose(player)
    }

    if (isPlayerWaiting) {
      removeWaitingPlayer
    }

    players += (id -> player)

    if (waitingPlayers.isEmpty) {
      waitingPlayers += (id -> player)
      println(s"Putting player $id in waiting list")
    }
    else {
      val firstInMap = waitingPlayers.keySet.iterator.next
      val opponent = waitingPlayers(firstInMap)
      val opponentId = opponent.user.id.get

      waitingPlayers -= opponentId

      val game = new Game(player, opponent, this)

      games += (id -> game)
      games += (opponentId -> game)

      println(s"Game was created with $id and $opponentId")
      player.out ! Json.obj(GameAPIKeys.opponentUsername -> opponent.user.username).toString()
      opponent.out ! Json.obj(GameAPIKeys.opponentUsername -> player.user.username).toString()
    }

    displayPlayers()
  }

  private def makePlayerLose(player: Player)(implicit id: Long): Unit = {
    println(s"$id loses because he left")
    val game = games(id)
    game.lose
  }

  private def isPlayerWaiting(implicit id: Long): Boolean = waitingPlayers.contains(id)
  private def isPlayerInGame(implicit id: Long): Boolean = games.contains(id)
  private def isPlayerInLobby(implicit id: Long): Boolean = isPlayerInGame && !games(id).everyoneReady()
  private def isPlayerPlaying(implicit id: Long): Boolean = isPlayerInGame && games(id).everyoneReady()

  private def removeWaitingPlayer(implicit id: Long): Unit = this.synchronized {
    println(s"$id was removed from waiting list")
    waitingPlayers -= id
    players -= id

    displayPlayers()
  }

  def handleLeave(implicit id: Long): Unit = {
    println(s"$id left before playing")
    if (isPlayerWaiting) removeWaitingPlayer
    else if (isPlayerInLobby) makePlayerLose(players(id))
  }

  def handleGameAction(action: String, out: ActorRef)(implicit id: Long): Unit = games.get(id) match {
    case Some(game) =>
      action match {
        case Actions.Ready.name => game.setReady
        case Actions.Left.name => game.movePiece(Actions.Left)
        case Actions.Right.name => game.movePiece(Actions.Right)
        case Actions.Rotate.name => game.movePiece(Actions.Rotate)
        case Actions.Down.name => game.movePiece(Actions.Down)
        case Actions.Fall.name => game.movePiece(Actions.Fall)
        case _ =>
          logger.warn(s"Unknown action received: $action")
          out ! PoisonPill
      }
    case None =>
      out ! Json.obj(GameAPIKeys.error -> "There is no opponent yet!").toString()
  }

  def endGame(result: Result): Unit = this.synchronized {
    println("end of the game")
    games -= result.winnerId
    games -= result.loserId

    players -= result.winnerId
    players -= result.loserId

    displayPlayers()

    results.create(result)
  }

  // TODO remove at the end
  private def displayPlayers(): Unit = {
    println(" players")
    print(" ")
    println(players)
    println()

    println(" waiting players")
    print(" ")
    println(waitingPlayers)
  }
}

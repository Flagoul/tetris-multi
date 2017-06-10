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
  private var lobbies: Map[Long, Lobby] = Map()
  private var games: Map[Long, Game] = Map()

  def joinGame(player: Player)(implicit id: Long): Unit = this.synchronized {
    if (isPlayerInGame) {
      println(s"Putting back $id in game")
      games(id).putBackPlayerInGame(player.out)
      return
    }

    if (isPlayerInLobby) makePlayerLose
    else if (isPlayerWaiting) removeWaitingPlayer

    players += (id -> player)

    if (waitingPlayers.isEmpty) addPlayerToQueue(player)
    else matchWithOpponent(player)

    displayPlayers()
  }

  private def addPlayerToQueue(player: Player): Unit = {
    waitingPlayers += (player.user.id.get -> player)
    println(s"Putting player ${player.user.id.get} in waiting list")
  }

  private def matchWithOpponent(player: Player): Unit = {
    println("creating lobby")

    val firstInMap = waitingPlayers.keySet.iterator.next
    val opponent = waitingPlayers(firstInMap)
    val opponentId = opponent.user.id.get

    waitingPlayers -= opponentId

    createLobby(player, opponent)
  }

  private def createLobby(p1: Player, p2: Player): Lobby = {
    val lobby = Lobby(p1, p2)
    lobbies += (p1.user.id.get -> lobby)
    lobbies += (p2.user.id.get -> lobby)

    p1.out ! Json.obj(GameAPIKeys.opponentUsername -> p2.user.username).toString()
    p2.out ! Json.obj(GameAPIKeys.opponentUsername -> p1.user.username).toString()

    lobby.open()

    lobby
  }

  private def deleteLobby(lobby: Lobby): Unit = {
    val p1 = lobby.player1
    val p2 = lobby.player2

    lobbies -= p1.user.id.get
    lobbies -= p2.user.id.get

    lobby.close()
  }

  private def createGame(p1: Player, p2: Player): Game = {
    val game = new Game(p1, p2, this)
    games += (p1.user.id.get -> game)
    games += (p2.user.id.get -> game)
    game
  }

  private def makePlayerLose(implicit id: Long): Unit = {
    println(s"$id loses because he left lobby")
    val lobby = lobbies(id)
    val p1 = lobby.player1
    val p2 = lobby.player2

    new Game(p1, p2, this).lose

    deleteLobby(lobby)
  }

  private def isPlayerWaiting(implicit id: Long): Boolean = waitingPlayers.contains(id)
  private def isPlayerInGame(implicit id: Long): Boolean = games.contains(id)
  private def isPlayerInLobby(implicit id: Long): Boolean = lobbies.contains(id)

  private def removeWaitingPlayer(implicit id: Long): Unit = this.synchronized {
    println(s"$id was removed from waiting list")
    waitingPlayers -= id
    players(id).out ! PoisonPill
    players -= id

    displayPlayers()
  }

  def handleLeave(implicit id: Long): Unit = {
    println(s"$id left before playing")
    if (isPlayerWaiting) removeWaitingPlayer(id)
    else if (isPlayerInLobby) makePlayerLose
  }

  def handleReady(out: ActorRef)(implicit id: Long): Unit = lobbies.get(id) match {
    case Some(lobby) =>
      lobby.setReady(id)

      val player = players(id)
      val opponent = if (player == lobby.player1) lobby.player2 else lobby.player1

      Network.broadcast(player.out, opponent.out, Json.obj(GameAPIKeys.ready -> true))

      if (lobby.everyoneReady()) {
        println(s"everyone is ready, creating and starting game with ${lobby.player1.user.id.get} and ${lobby.player2.user.id.get}")

        val game = createGame(lobby.player1, lobby.player2)
        deleteLobby(lobby)
        game.start()
      }
    case None =>
      logger.warn(s"Ready received when player not in lobby")
      out ! PoisonPill
  }

  def handleGameAction(action: String, out: ActorRef)(implicit id: Long): Unit = games.get(id) match {
    case Some(game) =>
      action match {
        case Actions.Left.name => game.movePiece(Actions.Left)
        case Actions.Right.name => game.movePiece(Actions.Right)
        case Actions.Rotate.name => game.movePiece(Actions.Rotate)
        case Actions.Down.name => game.movePiece(Actions.Down)
        case Actions.Fall.name => game.movePiece(Actions.Fall)
        case _ =>
          logger.warn(s"Unknown action received: $action")
          out ! PoisonPill
      }
    case None => // ignore action
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
    println()

    println(" lobbies")
    print(" ")
    println(lobbies)
    println()
  }
}

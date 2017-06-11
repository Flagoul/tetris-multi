package game

import akka.actor.{ActorRef, PoisonPill}
import game.player.Player
import game.utils.Network
import managers.ResultManager
import models.Result
import play.api.libs.json.Json
import shared.{Actions, GameAPIKeys}

import scala.collection.mutable

/**
  * Matches players together and spawns / handles / removes lobbies and games.
  *
  * Also used to create results at the end of a game.
  *
  * @param results The ResultManager to use to create game results.
  */
case class GameManager(results: ResultManager) {
  // The players that are either in a lobby or in a game
  private var players: Map[Long, Player] = Map()

  // The queue as a LinkedHashSet: allows FIFO like a queue, and allows easy element removal
  private var waitingPlayers: mutable.LinkedHashSet[Long] = mutable.LinkedHashSet()

  // The existing lobbies
  private var lobbies: Map[Long, Lobby] = Map()

  // The existing games
  private var games: Map[Long, Game] = Map()

  /**
    * Makes a player join a game.
    *
    * @param player The player to make
    * @param id The id of the player.
    */
  def joinGame(player: Player)(implicit id: Long): Unit = this.synchronized {
    if (isPlayerInGame) {
      games(id).putBackPlayerInGame(player.out)
      return
    }

    if (isPlayerInLobby) makePlayerLose
    else if (isPlayerWaiting) removePlayerFromQueue

    players += (id -> player)

    if (waitingPlayers.isEmpty) addPlayerToQueue
    else matchWithOpponent
  }

  /**
    * Adds a player to the waiting queue.
    *
    * @param id The id of the player.
    */
  private def addPlayerToQueue(implicit id: Long): Unit = {
    waitingPlayers += id
  }

  /**
    * Matches the current player with the first opponent.
    *
    * @param id The id of the player.
    */
  private def matchWithOpponent(implicit id: Long): Unit = {
    val opponentId = waitingPlayers.iterator.next
    waitingPlayers -= opponentId

    val p1 = players(id)
    val p2 = players(opponentId)

    val lobby = Lobby(p1, p2)
    lobbies += (id -> lobby)
    lobbies += (opponentId -> lobby)

    p1.out ! Json.obj(GameAPIKeys.opponentUsername -> p2.user.username).toString()
    p2.out ! Json.obj(GameAPIKeys.opponentUsername -> p1.user.username).toString()
  }

  /**
    * Closes the specified lobby by removes it from the lobbies list.
    *
    * @param lobby The specified lobby.
    */
  private def closeLobby(lobby: Lobby): Unit = {
    val p1 = lobby.player1
    val p2 = lobby.player2

    lobbies -= p1.user.id.get
    lobbies -= p2.user.id.get
  }

  /**
    * Creates and adds a new game to the game list using the specified players.
    *
    * @param p1 The first player.
    * @param p2 The second player.
    * @return The game created.
    */
  private def createGame(p1: Player, p2: Player): Game = {
    val game = new Game(p1, p2, this)
    games += (p1.user.id.get -> game)
    games += (p2.user.id.get -> game)
    game
  }

  /**
    * Makes the player lose the game that would be launched after the lobby in which he currently is.
    *
    * @param id The id of the player.
    */
  private def makePlayerLose(implicit id: Long): Unit = {
    val lobby = lobbies(id)
    val p1 = lobby.player1
    val p2 = lobby.player2

    new Game(p1, p2, this).lose

    closeLobby(lobby)
  }

  /**
    *Removes the player from the waiting queue.
    *
    * @param id The id of the current player.
    */
  private def removePlayerFromQueue(implicit id: Long): Unit = this.synchronized {
    waitingPlayers -= id
    players(id).out ! PoisonPill
    players -= id
  }

  /**
    * Takes the appropriate action when the player leaves the lobby or the queue.
    *
    * @param id The id of the current player.
    */
  def handleLeave(implicit id: Long): Unit = {
    if (isPlayerWaiting) removePlayerFromQueue(id)
    else if (isPlayerInLobby) makePlayerLose
  }

  /**
    * Takes the appropriate action when a ready action is received.
    *
    * @param out The actor ref of the player.
    * @param id The id of the player.
    */
  def handleReady(out: ActorRef)(implicit id: Long): Unit = lobbies.get(id) match {
    case Some(lobby) =>
      lobby.setReady(id)

      val player = players(id)
      val opponent = if (player == lobby.player1) lobby.player2 else lobby.player1

      Network.broadcastWithOpponent(player.out, opponent.out, Json.obj(GameAPIKeys.ready -> true))

      if (lobby.everyoneReady()) {
        val game = createGame(lobby.player1, lobby.player2)
        closeLobby(lobby)
        game.start()
      }
    case None =>
      out ! PoisonPill
  }

  /**
    * Takes the appropriate action according to the message received.
    *
    * @param action The action to take.
    * @param out The actor ref of the current player.
    * @param id The id of the player.
    */
  def handleGameAction(action: String, out: ActorRef)(implicit id: Long): Unit = games.get(id) match {
    case Some(game) =>
      action match {
        case Actions.Left.name => game.movePiece(Actions.Left)
        case Actions.Right.name => game.movePiece(Actions.Right)
        case Actions.Rotate.name => game.movePiece(Actions.Rotate)
        case Actions.Down.name => game.movePiece(Actions.Down)
        case Actions.Fall.name => game.movePiece(Actions.Fall)
        case _ =>
          out ! PoisonPill
      }
    case None => // ignore action
  }

  /**
    * Removes the game using the id of the players and save the result of the game.
    *
    * @param result The result to save.
    */
  def endGame(result: Result): Unit = this.synchronized {
    games -= result.winnerId
    games -= result.loserId

    players -= result.winnerId
    players -= result.loserId

    results.create(result)
  }

  /**
    * Helpers to know at which state of the play process a player is, using his id.
    */
  private def isPlayerWaiting(implicit id: Long): Boolean = waitingPlayers.contains(id)
  private def isPlayerInGame(implicit id: Long): Boolean = games.contains(id)
  private def isPlayerInLobby(implicit id: Long): Boolean = lobbies.contains(id)
}

package game

import game.player.Player

/**
  * A lobby where players are put when matched together before they begin to play.
  *
  * @param player1 The first player.
  * @param player2 The second player.
  */
case class Lobby(player1: Player, player2: Player) {
  // A player with his ready state
  private case class PlayerWithReadyState(player: Player, var isReady: Boolean = false)
  private val p1 = PlayerWithReadyState(player1)
  private val p2 = PlayerWithReadyState(player2)

  // Stores the players to be accessed by their id
  private val players: Map[Long, PlayerWithReadyState] = Map(
    player1.user.id.get -> p1,
    player2.user.id.get -> p2
  )

  /**
    * Determines whether the player with the given id is ready to play.
    * @param id The id of the player.
    * @return Whether the player is ready.
    */
  def isReady(id: Long): Boolean = {
    players(id).isReady
  }

  /**
    * @return Whether both players are ready to play.
    */
  def everyoneReady(): Boolean = p1.isReady && p2.isReady

  /**
    * Sets the player with the given id ready to play.
    *
    * @param id The id of the player.
    */
  def setReady(id: Long): Unit = {
    val player = players(id)
    player.isReady = true
    println(id + " is ready")
  }
}

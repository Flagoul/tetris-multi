package game

import akka.actor.{ActorRef}
import models.User

/**
  * A wrapper containing the player with his game state.
  *
  * @param player The player that will own a game state.
  */
case class PlayerWithState(player: Player) {
  // The user related to the player.
  val user: User = player.user

  // The actor ref to use when sending message to the player.
  var out: ActorRef = player.out

  // The game state related to the player.
  val state: GameState = new GameState()

  /**
    * Allows updating of actor reference of the player.
    *
    * @param newRef The new ref to use.
    */
  def changeActorRef(newRef: ActorRef): Unit = {
    player.changeActorRef(newRef)
    out = newRef
  }
}

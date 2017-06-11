package game.player

import akka.actor.{ActorRef, PoisonPill}
import models.User

/**
  * Represents a player in the game.
  *
  * @param user The user related to the player.
  * @param out The actor reference to use when sending messages to the player.
  */
case class Player(user: User, var out: ActorRef) {
  /**
    * Changes the actor reference used to send message to the player and close old websocket.
    *
    * @param newRef The new actor reference.
    */
  def changeActorRef(newRef: ActorRef): Unit = {
    out ! PoisonPill
    out = newRef
  }
}

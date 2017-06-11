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
  def changeActorRef(newRef: ActorRef): Unit = {
    out ! PoisonPill
    out = newRef
  }
}

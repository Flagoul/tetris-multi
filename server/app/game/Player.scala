package game

import akka.actor.{ActorRef, PoisonPill}
import models.User

case class Player(user: User, var out: ActorRef) {
  def changeActorRef(newRef: ActorRef): Unit = {
    out ! PoisonPill
    out = newRef
  }
}

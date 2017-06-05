package game

import akka.actor.ActorRef

case class Player(id: String, out: ActorRef)

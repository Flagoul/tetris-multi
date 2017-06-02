package game

import akka.actor.ActorRef

class GameUser(val id: String, val ref: ActorRef)

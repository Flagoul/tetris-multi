package game

import akka.actor.ActorRef
import models.User

case class Player(user: User, out: ActorRef)

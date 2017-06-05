package game

import akka.actor.ActorRef
import models.User

case class PlayerWithState(player: Player) {
  val user: User = player.user
  val out: ActorRef = player.out
  val state: GameState = new GameState()
}

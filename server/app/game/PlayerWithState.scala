package game

import akka.actor.ActorRef

case class PlayerWithState(player: Player) {
  val id: String = player.id
  val out: ActorRef = player.out
  val state: GameState = new GameState()
}

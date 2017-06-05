package game

import akka.actor.ActorRef
import shared.Pieces.randomPiece

case class PlayerWithState(player: Player) {
  val id: String = player.id
  val out: ActorRef = player.ref
  val state: GameState = new GameState(randomPiece(), randomPiece())
}

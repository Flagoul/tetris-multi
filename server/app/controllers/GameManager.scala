package controllers

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.stream.Materializer
import game.Game
import shared.Actions
import play.api.Logger.logger
import play.api.libs.json._
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket

import scala.collection.mutable

class GameManager @Inject()(implicit system: ActorSystem, materializer: Materializer) {
  private var waitingUsers: mutable.Queue[String] = mutable.Queue()
  private var games: Map[String, Game] = Map()

  def socket: WebSocket = WebSocket.accept[String, String] { _ =>
    ActorFlow.actorRef(out => Props(new GameWSActor(out)))
  }

  class GameWSActor(out: ActorRef) extends Actor {
    override def preStart(): Unit = {
      super.preStart()

      val id = java.util.UUID.randomUUID.toString

      // create game or join according to availability
      if (waitingUsers.isEmpty) {
        waitingUsers += id
        println("waiting")
      } else {
        val opponentId = waitingUsers.dequeue
        val game = new Game(id, opponentId)

        games += (id -> game)
        games += (opponentId -> game)

        println("playing")
      }

      out ! Json.stringify(Json.obj("id" -> id))
    }

    def receive: PartialFunction[Any, Unit] = {
      case msg: String => {
        try {
          val data: JsValue = Json.parse(msg)
          val action = (data \ "action").as[String]
          val id = (data \ "id").as[String]

          val game = games(id)

          //FIXME change what to do
          action match {
            case "start" => out ! "start"
            case "left" => out ! game.movePiece(id, Actions.Left)
            case "right" => out ! game.movePiece(id, Actions.Right)
            case "rotate" => out ! game.movePiece(id, Actions.Rotate)
            case "fall" => out ! game.movePiece(id, Actions.Fall)
            case "quit" => ???
            case _ => logger.warn(s"Unknown event received: $action")
          }
        } catch {
          case e: JsResultException =>
            logger.warn(s"Invalid json: ${e.errors}")
            self ! PoisonPill
        }
      }
      case _ =>
        logger.warn("Unsupported data format received!")
        self ! PoisonPill
    }
  }
}

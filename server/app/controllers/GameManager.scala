package controllers

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import play.api.Logger.logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket

class GameManager @Inject()(implicit system: ActorSystem, materializer: Materializer) {

  def socket: WebSocket = WebSocket.accept[String, String] { _ =>
    ActorFlow.actorRef(out => Props(new GameWSActor(out)))
  }

  class GameWSActor(out: ActorRef) extends Actor {

    def receive: PartialFunction[Any, Unit] = {
      case msg: String => {
        val data: JsValue = Json.parse(msg)
        val event = (data \ "event").as[String]

        //FIXME change what to do
        event match {
          case "start" => out ! "start"
          case "left" => out ! "left"
          case "right" => out ! "right"
          case "rotate" => out ! "rotate"
          case "fall" => out ! "fall"
          case _ => logger.warn(s"Unknown event received: $event")
        }
      }
      case _ =>
        logger.warn("Unsupported data format received!")
    }
  }
}

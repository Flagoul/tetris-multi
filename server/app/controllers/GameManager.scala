package controllers

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import game.Game
import play.api.Logger.logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket

import scala.collection.mutable

class GameManager @Inject()(implicit system: ActorSystem, materializer: Materializer) {
  private var waitingGames: mutable.Queue[Game] = mutable.Queue()
  private var games: mutable.Queue[Game] = mutable.Queue()

  //FIXME temporary
  private var game: Game = new Game()

  def socket: WebSocket = WebSocket.accept[String, String] { _ =>
    ActorFlow.actorRef(out => Props(new GameWSActor(out)))
  }

  class GameWSActor(out: ActorRef) extends Actor {
    override def preStart(): Unit = {
      super.preStart()

      // TODO link game with user
      // create game or join according to availability
      if (waitingGames.isEmpty) {
        waitingGames += new Game()
        println("waiting")
      } else {
        games += waitingGames.dequeue
        println("playing")
      }
    }

    def receive: PartialFunction[Any, Unit] = {
      case msg: String => {
        val data: JsValue = Json.parse(msg)
        val action = (data \ "action").as[String]

        //FIXME change what to do
        action match {
          case "play" =>
          case "start" => out ! "start"
          case "left" => out ! "left"
          case "right" => out ! "right"
          case "rotate" => out ! "rotate"
          case "fall" => out ! "fall"
          case "quit" => ???
          case _ => logger.warn(s"Unknown event received: $action")
        }
      }
      case _ =>
        logger.warn("Unsupported data format received!")
    }
  }
}

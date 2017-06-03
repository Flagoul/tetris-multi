package game

import java.util.UUID
import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.stream.Materializer
import play.api.Logger.logger
import play.api.libs.json._
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket
import shared.{Actions, GameAPIKeys}

import scala.collection.mutable

class GameManager @Inject()(implicit system: ActorSystem, materializer: Materializer) {
  private var waitingUsers: mutable.Queue[GameUser] = mutable.Queue()
  private var games: Map[String, Game] = Map()

  def socket: WebSocket = WebSocket.accept[String, String] { _ =>
    ActorFlow.actorRef(out => Props(new GameWSActor(out)))
  }

  class GameWSActor(out: ActorRef) extends Actor {
    override def preStart(): Unit = {
      super.preStart()
      joinGame(new GameUser(UUID.randomUUID.toString, out))
    }

    def receive: PartialFunction[Any, Unit] = {
      case msg: String => {
        try {
          val data: JsValue = Json.parse(msg)

          handleAction(
            (data \ GameAPIKeys.id).as[String],
            (data \ GameAPIKeys.action).as[String]
          )

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

    def joinGame(user: GameUser): Unit = {
      if (waitingUsers.isEmpty) {
        waitingUsers += user
        println("waiting")
      } else {
        val opponent = waitingUsers.dequeue
        val game = new Game(user, opponent)

        games += (user.id -> game)
        games += (opponent.id -> game)

        println("playing")
      }

      out ! Json.stringify(Json.obj(GameAPIKeys.id -> user.id))
    }

    def handleAction(id: String, action: String): Unit = {
      val game = games(id)
      action match {
        case "start" => game.setReady(id)
        case "quit" => self ! PoisonPill
        case "left" => game.movePiece(id, Actions.Left)
        case "right" => game.movePiece(id, Actions.Right)
        case "rotate" => game.movePiece(id, Actions.Rotate)
        case "fall" => game.movePiece(id, Actions.Fall)
        case _ =>
          logger.warn(s"Unknown action received: $action")
          self ! PoisonPill
      }
    }
  }
}

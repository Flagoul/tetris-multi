package game

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.stream.Materializer
import managers.{SessionManager, UserManager}
import models.User
import play.api.Logger.logger
import play.api.libs.json._
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Controller, WebSocket}
import shared.{Actions, GameAPIKeys}

import scala.concurrent.{ExecutionContext, Future}


class GameWSController @Inject()(sessions: SessionManager, users: UserManager)
                                (implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext)
  extends Controller {

  private val gameManager: GameManager = GameManager()

  def socket: WebSocket = WebSocket.acceptOrResult[String, String] { implicit request =>
    // TODO(Benjamin) : verifying CORS would be good
    // see https://www.playframework.com/documentation/2.5.x/ScalaWebSockets#rejecting-a-websocket for reference
    sessions.getSession.flatMap({
      case None => Future.successful(Left(Forbidden))
      case Some(session) => users.get(session.userId).map({
        case None => Left(Forbidden)
        case Some(u) => Right(ActorFlow.actorRef(out => Props(new GameWSActor(out, u))))
      })
    })
  }

  class GameWSActor(out: ActorRef, user: User) extends Actor {
    override def preStart(): Unit = {
      super.preStart()
      gameManager.joinGame(Player(user.username, out))
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

    def handleAction(id: String, action: String): Unit = {
      val game = gameManager.getGame(id)
      action match {
        case "start" => game.setReady(id)
        case "quit" => game.lose(id)
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

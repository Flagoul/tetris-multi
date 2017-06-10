package game

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.stream.Materializer
import managers.{ResultManager, SessionManager, UserManager}
import models.User
import play.api.Logger.logger
import play.api.libs.json._
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Controller, WebSocket}
import shared.{Actions, GameAPIKeys}

import scala.concurrent.{ExecutionContext, Future}


class GameWSController @Inject()(sessions: SessionManager, users: UserManager, results: ResultManager)
                                (implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext)
  extends Controller {

  private val gameManager: GameManager = GameManager(results)

  def socket: WebSocket = WebSocket.acceptOrResult[String, String] { implicit request =>
    // NOTE(Benjamin) : verifying CORS would be good
    // see https://www.playframework.com/documentation/2.5.x/ScalaWebSockets#rejecting-a-websocket for reference
    sessions.getSession.flatMap({
      case None => Future.successful(Left(Forbidden))
      case Some(session) => users.get(session.userId).map({
        case None => Left(Forbidden)
        case Some(u) => Right(ActorFlow.actorRef(out => Props(new GameWSActor(out, u)(u.id.get))))
      })
    })
  }

  class GameWSActor(out: ActorRef, user: User)(implicit id: Long) extends Actor {
    override def preStart(): Unit = {
      super.preStart()
      gameManager.joinGame(Player(user, out))
    }

    def receive: PartialFunction[Any, Unit] = {
      case msg: String => {
        try {
          handleAction((Json.parse(msg) \ GameAPIKeys.action).as[String])
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

    def handleAction(action: String)(implicit id: Long): Unit = action match {
      case Actions.Leave.name => gameManager.handleLeave
      case Actions.Ready.name => gameManager.handleReady(out)
      case _ => gameManager.handleGameAction(action, out)
    }
  }
}

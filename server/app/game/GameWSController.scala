package game

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.stream.Materializer
import game.player.Player
import managers.{ResultManager, SessionManager, UserManager}
import models.User
import play.api.Logger.logger
import play.api.libs.json._
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Controller, WebSocket}
import shared.{Actions, GameAPIKeys}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Controller for websockets related to the game.
  *
  * Uses Akka actors to receive and send data as json.
  *
  * @param sessions The session manager to use for the user sessions.
  * @param users The user manager to user to interact with user data.
  * @param results The result manager used to store game results.
  * @param materializer The materializer to use.
  * @param system The actor system to use.
  * @param ec The execution context in which to run.
  */
class GameWSController @Inject()(sessions: SessionManager, users: UserManager, results: ResultManager)
                                (implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext)
  extends Controller {

  // The game manager to use.
  private val gameManager: GameManager = GameManager(results)

  def socket: WebSocket = WebSocket.acceptOrResult[String, String] { implicit request =>
    // Note (Benjamin): verifying CORS could have been good here. To do that, we could have used this:
    // https://www.playframework.com/documentation/2.5.x/ScalaWebSockets#rejecting-a-websocket for reference
    sessions.getSession.flatMap({
      case None => Future.successful(Left(Forbidden))
      case Some(session) => users.get(session.userId).map({
        case None => Left(Forbidden)
        case Some(u) => Right(ActorFlow.actorRef(out => Props(new GameWSActor(out, u)(u.id.get))))
      })
    })
  }

  /**
    * The actor handling received info on the websocket.
    *
    * @param out The actor reference to use to send messages to the client.
    * @param user The user connected to the websocket.
    * @param id The id of the user.
    */
  class GameWSActor(out: ActorRef, user: User)(implicit id: Long) extends Actor {

    /**
      * Actions to take when the client opens a websocket to this endpoint.
      *
      * Makes the player join the game when a websocket is opened by the client.
      */
    override def preStart(): Unit = {
      super.preStart()
      gameManager.joinGame(Player(user, out))
    }

    /**
      * Handles data received by the actor.
      *
      * @return A partial function, a serie of case statements defining which message the actor can handle.
      */
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

    /**
      * Takes the appropriate action according to the action sent by the client.
      *
      * @param action The action sent by the client.
      * @param id The id of the user.
      */
    def handleAction(action: String)(implicit id: Long): Unit = action match {
      case Actions.Leave.name => gameManager.handleLeave
      case Actions.Ready.name => gameManager.handleReady(out)
      case _ => gameManager.handleGameAction(action, out)
    }
  }
}

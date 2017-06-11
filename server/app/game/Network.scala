package game

import akka.actor.ActorRef
import play.api.libs.json.{JsBoolean, JsObject}
import shared.GameAPIKeys

object Network {
  def broadcastWithOpponent(dest1: ActorRef, dest2: ActorRef, data: JsObject): Unit = {
    dest1 ! (data + (GameAPIKeys.opponent -> JsBoolean(false))).toString()
    dest2 ! (data + (GameAPIKeys.opponent -> JsBoolean(true))).toString()
  }
}

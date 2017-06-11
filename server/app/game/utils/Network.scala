package game.utils

import akka.actor.ActorRef
import play.api.libs.json.{JsBoolean, JsObject}
import shared.GameAPIKeys

/**
  * Regroups utility functions related to networking (sending message, for example).
  */
object Network {
  /**
    * Sends the given object to the 2 specified players using their actor references and includes a Boolean that
    * indicates whether the data sent is related to the opponent or not.
    *
    * @param dest1 The actor reference of the first player.
    * @param dest2 The actor reference of the second player.
    * @param data The object to send.
    */
  def broadcastWithOpponent(dest1: ActorRef, dest2: ActorRef, data: JsObject): Unit = {
    dest1 ! (data + (GameAPIKeys.opponent -> JsBoolean(false))).toString()
    dest2 ! (data + (GameAPIKeys.opponent -> JsBoolean(true))).toString()
  }
}

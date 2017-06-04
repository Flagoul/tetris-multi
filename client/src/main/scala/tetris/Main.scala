package tetris

import org.scalajs.dom
import shared.SharedMessages
import tetris.game._

import scala.scalajs.js

object Main extends js.JSApp {
  def main(): Unit = {
    // FIXME find a way to load code according to url
    shoutOut()
    game()
  }

  def shoutOut(): Unit = {
    val scalajsShoutOut = dom.document.getElementById("scalajsShoutOut")

    // FIXME find a way to load code according to url
    if (scalajsShoutOut != null) {
      scalajsShoutOut.textContent = SharedMessages.itWorks
    }
  }

  def game(): Unit = {
    // FIXME find a way to load code according to url
    if (dom.document.querySelector("#player-game-box") == null) {
      return
    }

    new Game().init()
  }
}

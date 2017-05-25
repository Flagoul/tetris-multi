package example

import example.game._
import org.scalajs.dom
import shared.SharedMessages

import scala.scalajs.js

object ScalaJSExample extends js.JSApp {
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
    if (dom.document.querySelector("#user-game-box") == null) {
      return
    }

    new Game().run()
  }
}

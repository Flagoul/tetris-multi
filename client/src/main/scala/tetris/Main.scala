package tetris

import tetris.helpers.DataTable

import scala.scalajs.js

/**
  * Launches the Scala JS app.
  */
object Main extends js.JSApp {
  def main(): Unit = {
    DataTable.attach()
  }
}

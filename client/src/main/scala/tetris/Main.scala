package tetris

import tetris.helpers.DataTable

import scala.scalajs.js

/**
  * Launches the Scala JS app.
  *
  * In this case, the app seems to only load DataTable, but in reality, components are exported with the @JSExportTopLevel
  * annotation and can then be used in the templates to be loaded whenever required.
  */
object Main extends js.JSApp {
  def main(): Unit = {
    DataTable.attach()
  }
}

package tetris.helpers

import org.scalajs.dom
import org.scalajs.jquery.jQuery

import scala.scalajs.js

/**
  * Contains useful methods related to DataTable library.
  */
object DataTable {
  /**
    * Allows injection of DataTable tables into our ScalaJS app.
    */
  def attach(): Unit = {
    jQuery(dom.document).ready(() => {
      // Note (Benjamin): here, a facade would be cleaner if we need more calls from this library.
      // In the actual state, this would be overkill.
      jQuery("table[dataTable]").each((table: dom.Element) => jQuery(table).asInstanceOf[js.Dynamic].DataTable())
    })
  }
}

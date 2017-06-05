package tetris.helpers

import org.scalajs.dom
import org.scalajs.jquery.jQuery

import scala.scalajs.js


object DataTable {
  def attach(): Unit = {
    jQuery(dom.document).ready(() => {
      // TODO(Benjamin) A facade would be cleaner if we need more calls from this library.
      // In the actual state, this is overkill.
      jQuery("table[dataTable]").each((table: dom.Element) => jQuery(table).asInstanceOf[js.Dynamic].DataTable())
    })
  }
}

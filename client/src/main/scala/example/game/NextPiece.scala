package example.game

import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.Element

class NextPiece(rows: Int, cols: Int, canvas: Canvas) extends GridCanvas(rows, cols, canvas) {
  override def draw(): Unit = {
    super.draw()
    println("Drawing next piece")
  }
}

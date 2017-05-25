package example.game

import org.scalajs.dom.html.Canvas

class GameCanvas(rows: Int, cols: Int, canvas: Canvas) extends GridCanvas(rows, cols, canvas) {
  def draw(): Unit = ???
}

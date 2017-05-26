package example.game

import org.scalajs.dom.html.Canvas

class GameCanvas(rows: Int, cols: Int, canvas: Canvas) extends GridCanvas(rows, cols, canvas) {
  override def draw(): Unit = {
    super.draw()
    println("Game drawn")
  }
}

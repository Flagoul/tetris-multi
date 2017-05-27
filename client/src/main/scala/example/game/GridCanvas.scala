package example.game

import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.CanvasRenderingContext2D

class GridCanvas(rows: Int, cols: Int, canvas: Canvas) {
  def draw(): Unit = {
    println(canvas)
    val ctx = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

    canvas.height = canvas.scrollHeight
    canvas.width = canvas.scrollWidth

    val rowStep = canvas.height.toFloat / rows
    val colStep = canvas.width.toFloat / cols

    ctx.strokeStyle = "#bbb"
    ctx.lineWidth = 0.2

    for (i <- 1 to rows) {
      val y = i * rowStep
      ctx.moveTo(0, y)
      ctx.lineTo(canvas.width, y)
      ctx.stroke()
    }

    for (i <- 1 to cols) {
      val x = i * colStep
      ctx.moveTo(x, 0)
      ctx.lineTo(x, canvas.height)
      ctx.stroke()
    }
  }
}

package example.game

import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.CanvasRenderingContext2D

class GridCanvas(rows: Int, cols: Int, canvas: Canvas) {
  def context2D: CanvasRenderingContext2D = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

  def resize(): Unit = {
    canvas.height = canvas.scrollHeight
    canvas.width = canvas.scrollWidth
  }

  def drawGrid(): Unit = {
    val ctx = context2D

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

  def drawGridContent(content: Array[Array[Boolean]]): Unit = {
    val ctx = context2D

    val blockHeight = canvas.height.toFloat / rows - 1
    val blockWidth = canvas.width.toFloat / cols - 1

    ctx.strokeStyle = "#000"

    for (row <- content.indices; col <- content.head.indices) {
      if (content(row)(col)) {
        ctx.fillRect(col * (blockWidth + 1), row * (blockHeight + 1), blockWidth, blockHeight)
      }
    }
  }
}

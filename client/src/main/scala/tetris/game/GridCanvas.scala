package tetris.game

import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.CanvasRenderingContext2D
import shared.Types.Grid

/**
  * Represents a canvas containing a grid.
  *
  * @param rows The number of rows in the grid.
  * @param cols The number of cols in the grid.
  * @param canvas The canvas in which to draw.
  * @param gridColor The color of the grid. Default is #bbb.
  * @param cellColor The color of the cells once filled. Default is #000.
  */
class GridCanvas(rows: Int, cols: Int, canvas: Canvas, gridColor: String = "#bbb", cellColor: String = "000") {
  /**
    * Gets the context 2D of the canvas.
    *
    * @return The context 2D of the canvas.
    */
  private def context2D: CanvasRenderingContext2D = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

  /**
    * Resizes the canvas's drawing area according to the effective size of the canvas (its real size on the page).
    */
  def resize(): Unit = {
    canvas.height = canvas.scrollHeight
    canvas.width = canvas.scrollWidth
  }

  /**
    * Draws a grid on the canvas.
    */
  def drawGrid(): Unit = {
    val ctx = context2D

    val rowStep = canvas.height.toFloat / rows
    val colStep = canvas.width.toFloat / cols

    ctx.strokeStyle = gridColor
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

  /**
    * Draws the content of the grid on the canvas.
    *
    * @param content The content of the grid.
    */
  def drawGridContent(content: Grid): Unit = {
    val ctx = context2D

    val blockHeight = canvas.height.toFloat / rows - 1
    val blockWidth = canvas.width.toFloat / cols - 1

    ctx.strokeStyle = cellColor

    for (row <- content.indices; col <- content.head.indices) {
      if (content(row)(col)) {
        ctx.fillRect(col * (blockWidth + 1), row * (blockHeight + 1), blockWidth, blockHeight)
      }
    }
  }
}

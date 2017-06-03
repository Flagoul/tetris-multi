package tetris.game

import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.{Element, HTMLDivElement}

/**
  * Represents a game box and its content.
  *
  * It is linked to a HTML element that is supposed to contain various children:
  * - the game canvas, with class .game
  * - the canvas displaying the next piece, contained in a div with class .next-piece
  *
  * @param id The HTML id to refer to this game box.
  */
class GameBox(id: String, nGameRows: Int, nGameCols: Int, nNextPieceRows: Int, nNextPieceCols: Int) {
  private val box: Element = dom.document.querySelector("#" + id)

  private val gameCanvas: GridCanvas = new GridCanvas(
    nGameRows, nGameCols, box.querySelector(".game").asInstanceOf[Canvas]
  )
  private val nextPieceCanvas: GridCanvas = new GridCanvas(
    nNextPieceRows, nNextPieceCols, box.querySelector(".next-piece canvas").asInstanceOf[Canvas]
  )

  private val piecesPlacedDiv: HTMLDivElement = box.querySelector(".pieces-placed").asInstanceOf[HTMLDivElement]
  private val pointsDiv: HTMLDivElement = box.querySelector(".points").asInstanceOf[HTMLDivElement]

  def setPiecesPlaced(piecesPlaced: Int): Unit = piecesPlacedDiv.innerHTML = piecesPlaced.toString

  def setPoints(points: Int): Unit = pointsDiv.innerHTML = points.toString

  def drawGame(gridContent: Array[Array[Boolean]]): Unit = {
    gameCanvas.resize()
    gameCanvas.drawGrid()
    gameCanvas.drawGridContent(gridContent)
  }

  def drawNextPiece(gridContent: Array[Array[Boolean]]): Unit = {
    nextPieceCanvas.resize()
    nextPieceCanvas.drawGrid()
    nextPieceCanvas.drawGridContent(gridContent)
  }
}

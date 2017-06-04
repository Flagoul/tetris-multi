package tetris.game

import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.{Element, HTMLDivElement, HTMLParagraphElement}
import shared.Types.Position

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

  private val layer: HTMLDivElement = box.querySelector(".layer").asInstanceOf[HTMLDivElement]
  private val layerText: HTMLParagraphElement = box.querySelector("p").asInstanceOf[HTMLParagraphElement]

  private val piecesPlacedDiv: HTMLDivElement = box.querySelector(".pieces-placed").asInstanceOf[HTMLDivElement]
  private val pointsDiv: HTMLDivElement = box.querySelector(".points").asInstanceOf[HTMLDivElement]

  private var gameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
  private var nextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nNextPieceRows, nNextPieceCols)
  private var piecePositions: List[Position] = List()

  def setLayerText(text: String): Unit = layerText.innerHTML = text

  def hideLayer(): Unit = layer.style.display = "none"

  def showLayer(): Unit = layer.style.display = "block"

  def setPiecesPlaced(piecesPlaced: String): Unit = piecesPlacedDiv.innerHTML = piecesPlaced

  def setPoints(points: String): Unit = pointsDiv.innerHTML = points


  def updateGameGrid(grid: Array[Array[Boolean]]): Unit = {
    gameGrid = grid
    drawGame()
  }
  def updateNextPieceGrid(grid: Array[Array[Boolean]]): Unit = {
    nextPieceGrid = grid
    drawNextPieceGrid()
  }

  def updatePiecePositions(positions: List[Position]): Unit = {
    piecePositions.foreach(p => gameGrid(p._1)(p._2) = false)
    positions.foreach(p => gameGrid(p._1)(p._2) = true)
    piecePositions = positions
    drawGame()
  }

  def drawGame(): Unit = {
    gameCanvas.resize()
    gameCanvas.drawGrid()
    gameCanvas.drawGridContent(gameGrid)
  }

  def drawNextPieceGrid(): Unit = {
    nextPieceCanvas.resize()
    nextPieceCanvas.drawGrid()
    nextPieceCanvas.drawGridContent(nextPieceGrid)
  }
}

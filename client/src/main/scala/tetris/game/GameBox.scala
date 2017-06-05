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
  * - the canvas displaying the next piece, a div with class .next-piece that contains the canvas
  * - the number of pieces placed, with class .piece-placed
  * - the number of points placed, with class .points
  * - the layer covering the box to display info, with class .layer
  *
  * @param id The HTML id to refer to this game box.
  * @param nGameRows The number of rows for the game grid.
  * @param nGameCols The number of columns for the game grid.
  * @param nNextPieceRows The HTML id to refer to this game box.
  * @param nNextPieceCols The HTML id to refer to this game box.
  */
class GameBox(id: String, nGameRows: Int, nGameCols: Int, nNextPieceRows: Int, nNextPieceCols: Int) {
  // The HTML element representing the box
  private val box: Element = dom.document.querySelector("#" + id)

  // The canvas where to draw game grid and content
  private val gameCanvas: GridCanvas = new GridCanvas(
    nGameRows, nGameCols, box.querySelector(".game").asInstanceOf[Canvas]
  )

  // The canvas where to draw the next piece
  private val nextPieceCanvas: GridCanvas = new GridCanvas(
    nNextPieceRows, nNextPieceCols, box.querySelector(".next-piece canvas").asInstanceOf[Canvas]
  )

  // The info layer and its text
  private val layer: HTMLDivElement = box.querySelector(".layer").asInstanceOf[HTMLDivElement]
  private val layerText: HTMLParagraphElement = box.querySelector("p").asInstanceOf[HTMLParagraphElement]

  // The divs containing the number of pieces placed and points
  private val piecesPlacedDiv: HTMLDivElement = box.querySelector(".pieces-placed").asInstanceOf[HTMLDivElement]
  private val pointsDiv: HTMLDivElement = box.querySelector(".points").asInstanceOf[HTMLDivElement]

  // The game and next piece grids as well as the current piece positions
  private var gameGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
  private var nextPieceGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nNextPieceRows, nNextPieceCols)
  private var piecePositions: List[Position] = List()

  /**
    * Sets the text on the info layer.
    *
    * @param text The text to set.
    */
  def setLayerText(text: String): Unit = layerText.innerHTML = text

  /**
    * Hides the info layer.
    */
  def hideLayer(): Unit = layer.style.display = "none"

  /**
    * Shows the info layer.
    */
  def showLayer(): Unit = layer.style.display = "block"

  /**
    * Sets the number of pieces placed.
    *
    * @param piecesPlaced The number of pieces placed as a string.
    */
  def setPiecesPlaced(piecesPlaced: String): Unit = piecesPlacedDiv.innerHTML = piecesPlaced

  /**
    * Sets the number of points.
    *
    * @param points The number of points as a string.
    */
  def setPoints(points: String): Unit = pointsDiv.innerHTML = points

  /**
    * Updates the game grid by replacing it completely with the new value.
    *
    * @param grid The new grid.
    */
  def updateGameGrid(grid: Array[Array[Boolean]]): Unit = {
    gameGrid = grid
    drawGame()
  }

  /**
    * Updates the next piece grid by replacing it completely with the new value.
    *
    * @param grid The new grid.
    */
  def updateNextPieceGrid(grid: Array[Array[Boolean]]): Unit = {
    nextPieceGrid = grid
    drawNextPieceGrid()
  }

  /**
    * Updates the positions of the current piece.
    *
    * @param positions The new positions.
    */
  def updatePiecePositions(positions: List[Position]): Unit = {
    piecePositions.foreach(p => gameGrid(p._1)(p._2) = false)
    positions.foreach(p => gameGrid(p._1)(p._2) = true)
    piecePositions = positions
    drawGame()
  }

  /**
    * Draws the game grid and its content.
    */
  def drawGame(): Unit = {
    gameCanvas.resize()
    gameCanvas.drawGrid()
    gameCanvas.drawGridContent(gameGrid)
  }

  /**
    * Draws the next piece grid and its content.
    */
  def drawNextPieceGrid(): Unit = {
    nextPieceCanvas.resize()
    nextPieceCanvas.drawGrid()
    nextPieceCanvas.drawGridContent(nextPieceGrid)
  }
}

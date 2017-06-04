package tetris.game

import json._
import org.scalajs.dom
import org.scalajs.dom.WebSocket
import org.scalajs.dom.raw.{HTMLButtonElement, MessageEvent, MouseEvent}
import shared.Actions._
import shared.GameAPIKeys
import shared.GameRules._


class Game {
  private val playerGB: GameBox = new GameBox("player-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)
  private val opponentGB: GameBox = new GameBox("opponent-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)

  private val host: String = dom.window.location.host
  private val ws = new WebSocket(s"ws://$host/ws")

  private val startButton: HTMLButtonElement = dom.document.querySelector("#ready-button").asInstanceOf[HTMLButtonElement]

  private var id: String = ""

  def idExists(data: JValue): Boolean = data.isDefinedAt(GameAPIKeys.id)
  def readyExists(data: JValue): Boolean = data.isDefinedAt(GameAPIKeys.ready)
  def wonExists(data: JValue): Boolean = data.isDefinedAt(GameAPIKeys.won)
  def drawExists(data: JValue): Boolean = data.isDefinedAt(GameAPIKeys.draw)

  def wonValue(data: JValue): Boolean = data(GameAPIKeys.won).value.asInstanceOf[Boolean]
  def opponentValue(data: JValue): Boolean = data(GameAPIKeys.opponent).value.asInstanceOf[Boolean]

  def handleId(data: JValue): Unit = id = data(GameAPIKeys.id).value.asInstanceOf[String]

  def handleReady(data: JValue): Unit = {
    if (opponentValue(data)) {
      opponentGB.setLayerText("Ready")
    }
    else {
      playerGB.setLayerText("Ready")
      startButton.style.display = "none"
    }
  }

  def handleWonOrDraw(data: JValue): Unit = {
    playerGB.showLayer()
    opponentGB.showLayer()

    if (wonExists(data)) {
      val winnerGB = if (wonValue(data)) playerGB else opponentGB
      val loserGB = if (winnerGB == playerGB) opponentGB else playerGB

      winnerGB.setLayerText("Win")
      loserGB.setLayerText("Lose")
    }
    else if (drawExists(data)) {
      playerGB.setLayerText("Draw")
      opponentGB.setLayerText("Draw")
    }
  }

  def handleGame(data: JValue): Unit = {
    playerGB.hideLayer()
    opponentGB.hideLayer()

    val opponent = opponentValue(data)
    drawGridIfExists(data, GameAPIKeys.gameGrid, opponent)
    drawGridIfExists(data, GameAPIKeys.nextPieceGrid, opponent)
    changeInfoIfExists(data, GameAPIKeys.piecesPlaced, opponent)
    changeInfoIfExists(data, GameAPIKeys.points, opponent)
  }

  def handleMessage(data: JValue): Unit = {
    if (idExists(data)) handleId(data)
    else if (readyExists(data)) handleReady(data)
    else if (wonExists(data) || drawExists(data)) handleWonOrDraw(data)
    else handleGame(data)
  }

  def drawGridIfExists(data: JValue, key: String, opponent: Boolean): Unit = {
    if (data.isDefinedAt(key)) {
      // FIXME change to use seqs instead of arrays everywhere
      val grid = data(key).value.asInstanceOf[Seq[Seq[Boolean]]].map(_.toArray).toArray
      val gb = if (opponent) opponentGB else playerGB

      key match {
        case GameAPIKeys.gameGrid => gb.drawGame(grid)
        case GameAPIKeys.nextPieceGrid => gb.drawNextPiece(grid)
      }
    }
  }

  def changeInfoIfExists(data: JValue, key: String, opponent: Boolean): Unit = {
    if (data.isDefinedAt(key)) {
      val gb = if (opponent) opponentGB else playerGB
      key match {
        case GameAPIKeys.piecesPlaced => gb.setPiecesPlaced(data(key).value.toString)
        case GameAPIKeys.points => gb.setPoints(data(key).value.toString)
      }
    }
  }

  def sendAction(action: Action): Unit = {
    val json: String = Map(
      GameAPIKeys.id -> id,
      GameAPIKeys.action -> action.name
    ).js.toDenseString

    ws.send(json)
  }

  def handleKeyDown(keyCode: Int): Unit = {
    keyCode match {
      case 37 | 65 => sendAction(Left)
      case 38 | 87 => sendAction(Rotate)
      case 39 | 68 => sendAction(Right)
      case 40 | 83 => sendAction(Fall)
      case _ =>
    }
  }

  def init(): Unit = {
    playerGB.drawGame(Array.ofDim[Boolean](nGameRows, nGameCols))
    playerGB.drawNextPiece(Array.ofDim[Boolean](nNextPieceRows, nNextPieceCols))
    opponentGB.drawGame(Array.ofDim[Boolean](nGameRows, nGameCols))
    opponentGB.drawNextPiece(Array.ofDim[Boolean](nNextPieceRows, nNextPieceCols))

    startButton.onclick = (_: MouseEvent) => sendAction(Start)

    ws.onmessage = (e: MessageEvent) => handleMessage(JValue.fromString(e.data.toString))

    dom.window.onkeydown = (e: dom.KeyboardEvent) => handleKeyDown(e.keyCode)
  }
}

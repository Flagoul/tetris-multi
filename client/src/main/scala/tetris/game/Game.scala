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

  def sendAction(action: Action): Unit = {
    val json: String = Map(
      GameAPIKeys.id -> id,
      GameAPIKeys.action -> action.name
    ).js.toDenseString

    ws.send(json)
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

  def changeIntValueIfExists(data: JValue, key: String, opponent: Boolean): Unit = {
    if (data.isDefinedAt(key)) {
      val gb = if (opponent) opponentGB else playerGB
      key match {
        case GameAPIKeys.piecesPlaced => gb.setPiecesPlaced(data(key).value.toString)
        case GameAPIKeys.points => gb.setPoints(data(key).value.toString)
      }
    }
  }

  def handleMessage(data: JValue): Unit = {
    if (data.isDefinedAt(GameAPIKeys.id)) {
      id = data(GameAPIKeys.id).value.asInstanceOf[String]
    }
    else if (data.isDefinedAt(GameAPIKeys.ready)) {
      if (data(GameAPIKeys.opponent).value.asInstanceOf[Boolean]) {
        opponentGB.setLayerText("Ready")
      }
      else {
        playerGB.setLayerText("Ready")
        startButton.style.display = "none"
      }
    }
    else if (data.isDefinedAt(GameAPIKeys.won) || data.isDefinedAt(GameAPIKeys.draw)) {
      playerGB.showLayer()
      opponentGB.showLayer()

      if (data.isDefinedAt(GameAPIKeys.won)) {
        if (data(GameAPIKeys.won).value.asInstanceOf[Boolean]) {
          playerGB.setLayerText("Win")
          opponentGB.setLayerText("Lose")
        }
        else {
          playerGB.setLayerText("Lose")
          opponentGB.setLayerText("Win")
        }
      }
      else if (data.isDefinedAt(GameAPIKeys.draw)) {
        playerGB.setLayerText("Draw")
        opponentGB.setLayerText("Draw")
      }
    }
    else {
      playerGB.hideLayer()
      opponentGB.hideLayer()

      val opponent = data(GameAPIKeys.opponent).value.asInstanceOf[Boolean]
      drawGridIfExists(data, GameAPIKeys.gameGrid, opponent)
      drawGridIfExists(data, GameAPIKeys.nextPieceGrid, opponent)
      changeIntValueIfExists(data, GameAPIKeys.piecesPlaced, opponent)
      changeIntValueIfExists(data, GameAPIKeys.points, opponent)
    }
  }

  def init(): Unit = {
    playerGB.drawGame(Array.ofDim[Boolean](nGameRows, nGameCols))
    playerGB.drawNextPiece(Array.ofDim[Boolean](nNextPieceRows, nNextPieceCols))
    opponentGB.drawGame(Array.ofDim[Boolean](nGameRows, nGameCols))
    opponentGB.drawNextPiece(Array.ofDim[Boolean](nNextPieceRows, nNextPieceCols))

    playerGB.setLayerText("")

    startButton.onclick = (_: MouseEvent) => sendAction(Start)

    ws.onmessage = (e: MessageEvent) => handleMessage(JValue.fromString(e.data.toString))

    dom.window.onkeydown = (e: dom.KeyboardEvent) => {
      e.keyCode match {
        case 37 | 65 => sendAction(Left)
        case 38 | 87 => sendAction(Rotate)
        case 39 | 68 => sendAction(Right)
        case 40 | 83 => sendAction(Fall)
        case _ =>
      }
    }
  }
}

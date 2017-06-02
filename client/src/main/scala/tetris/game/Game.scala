package tetris.game

import json._
import org.scalajs.dom
import org.scalajs.dom.WebSocket
import org.scalajs.dom.raw.{HTMLButtonElement, MessageEvent, MouseEvent}
import shared.Actions._
import shared.GameAPIKeys
import shared.GameSettings._


class Game {
  private val userGB: GameBox = new GameBox("user-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)
  private val opponentGB: GameBox = new GameBox("opponent-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)

  private val host: String = dom.window.location.host
  private val ws = new WebSocket(s"ws://$host/ws")

  private var id: String = ""

  def sendAction(action: Action): Unit = {
    val json: String = Map(
      GameAPIKeys.id -> id,
      GameAPIKeys.action -> action.name
    ).js.toDenseString

    ws.send(json)
  }

  def handleMessage(data: JValue): Unit = {
    if (data("id") != JUndefined) {
      id = data("id").value.asInstanceOf[String]
    } else {
      val opponent = data(GameAPIKeys.opponent).value.asInstanceOf[Boolean]

      // FIXME change to use seqs instead of arrays everywhere
      if (data(GameAPIKeys.gameGrid) != JUndefined) {
        val grid = data(GameAPIKeys.gameGrid).value.asInstanceOf[Seq[Seq[Boolean]]].map(_.toArray).toArray
        if (opponent) opponentGB.drawGame(grid)
        else userGB.drawGame(grid)
      }

      if (data(GameAPIKeys.nextPieceGrid) != JUndefined) {
        val grid = data(GameAPIKeys.nextPieceGrid).value.asInstanceOf[Seq[Seq[Boolean]]].map(_.toArray).toArray
        if (opponent) opponentGB.drawNextPiece(grid)
        else userGB.drawNextPiece(grid)
      }
    }
  }

  def run(): Unit = {
    userGB.drawGame(Array.ofDim[Boolean](nGameRows, nGameCols))
    userGB.drawNextPiece(Array.ofDim[Boolean](nNextPieceRows, nNextPieceCols))
    opponentGB.drawGame(Array.ofDim[Boolean](nGameRows, nGameCols))
    opponentGB.drawNextPiece(Array.ofDim[Boolean](nNextPieceRows, nNextPieceCols))

    val startButton = dom.document.querySelector("#start-button").asInstanceOf[HTMLButtonElement]
    startButton.onclick = { (_: MouseEvent) =>
      sendAction(Start)
    }

    ws.onmessage = { (e: MessageEvent) =>
      val data = JValue.fromString(e.data.toString)
      handleMessage(data)
    }

    dom.window.onkeydown = { (e: dom.KeyboardEvent) =>
      e.keyCode match {
        case 37 | 65 => sendAction(Left)
        case 38 | 87 => sendAction(Rotate)
        case 39 | 68 => sendAction(Right)
        case 40 | 83 => sendAction(Fall)
        case _ => println(e.keyCode)
      }
    }
  }
}

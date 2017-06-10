package tetris.game

import json._
import org.scalajs.dom
import org.scalajs.dom.WebSocket
import org.scalajs.dom.raw._
import shared.Actions._
import shared.GameAPIKeys
import shared.GameRules._
import shared.Types.Position

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}


/**
  * Handles the display of the game content and the websocket connection to the server.
  *
  * Note that for this to work, the following elements must exist:
  * - a button with id #ready-button must exist in order for this class to bind the click event
  * - a div with the id #opponent-username to set the player's opponent username when he joins.
  * - a div with the id #error to display errors
  * - a div with the id #end-buttons containing the buttons with various actions suggested to player at the end
  */
@JSExportTopLevel("tetris.Game")
class Game {
  // The game boxes of the player and his opponent
  private val playerGB: GameBox = new GameBox("player-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)
  private val opponentGB: GameBox = new GameBox("opponent-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)

  // The opponent's username
  private val opponentUsername: HTMLDivElement = dom.document.querySelector("#opponent-username").asInstanceOf[HTMLDivElement]

  // The buttons with the actions displayable at the end
  private val endButtons: HTMLDivElement = dom.document.querySelector("#end-buttons").asInstanceOf[HTMLDivElement]

  private val wsProtocol = dom.window.location.protocol match {
    case "http:" => "ws"
    case "https:" => "wss"
  }

  // The websocket linked to the game connection url
  private val ws = new WebSocket(s"$wsProtocol://${dom.window.location.host}/ws")

  // The start button on the player side
  private val readyButton: HTMLButtonElement = dom.document.querySelector("#ready-button").asInstanceOf[HTMLButtonElement]

  private val error: HTMLDivElement = dom.document.querySelector("#error").asInstanceOf[HTMLDivElement]

  /**
    * Setups the game, events and websocket communication.
    */
  @JSExport
  def init(): Unit = {
    playerGB.drawGame()
    playerGB.drawNextPieceGrid()
    opponentGB.drawGame()
    opponentGB.drawNextPieceGrid()

    readyButton.onclick = (_: MouseEvent) => sendAction(Ready)

    ws.onmessage = (e: MessageEvent) => handleMessage(JValue.fromString(e.data.toString))

    dom.window.onkeydown = (e: dom.KeyboardEvent) => handleKeyDown(e.keyCode)

    dom.window.onunload = (_: dom.Event) => {
      println("left play page")
      sendAction(Leave)
    }
  }

  private def activateLeaveWarning(): Unit = {
    // Trick to trigger the "do you really want to leave" prompt. If some text is returned, the prompt will appear.
    dom.window.onbeforeunload = (_: dom.BeforeUnloadEvent) => {""}
  }

  private def suppressLeaveWarning(): Unit = {
    dom.window.onbeforeunload = null
  }

  /**
    * Sends the specified action to the server.
    *
    * @param action The action to do.
    */
  private def sendAction(action: Action): Unit = {
    val json: String = Map(
      GameAPIKeys.action -> action.name
    ).js.toDenseString

    ws.send(json)
  }

  /**
    * Takes the appropriate action according to key pressed (uses keydown event).
    *
    * @param keyCode The code of the key pressed.
    */
  private def handleKeyDown(keyCode: Int): Unit = {
    keyCode match {
      case 37 | 65 => sendAction(Left)
      case 38 | 87 => sendAction(Rotate)
      case 39 | 68 => sendAction(Right)
      case 40 | 83 => sendAction(Down)
      case 32 => sendAction(Fall)
      case _ =>
    }
  }

  /**
    * Handles received data during the game.
    *
    * @param data The received data.
    */
  private def handleMessage(data: JValue): Unit = {
    if (errorExists(data)) {
      error.innerHTML = getError(data)
    }
    else {
      error.innerHTML = ""
      if (opponentUsernameExists(data)) handleOpponentUsername(data)
      else if (readyExists(data)) handleReady(data)
      else if (wonExists(data) || drawExists(data)) handlesGameEnd(data)
      else handleGame(data)
    }
  }
  /**
    * Handles the reception of the opponent's username.
    *
    * @param data The received data containing the opponent username.
    */
  private def handleOpponentUsername(data: JValue): Unit = {
    opponentUsername.innerHTML = getOpponentUsername(data)
    opponentGB.setLayerText("Not ready")

    readyButton.style.display = "block"
    playerGB.setLayerText("")

    activateLeaveWarning()
  }

  /**
    * Handles the reception of a "ready" information.
    *
    * Depending on the "opponent" value, it can be either a confirmation that the player is ready or an information
    * that the opponent is.
    *
    * @param data The data containing the "ready" and "opponent" information.
    */
  private def handleReady(data: JValue): Unit = {
    if (getOpponentValue(data)) {
      opponentGB.setLayerText("Ready")
    }
    else {
      playerGB.setLayerText("Ready")
      readyButton.style.display = "none"
    }
  }

  /**
    * Handles the end of the game.
    *
    * It can be either a player that wins or a draw. According to the situation, the appropriate text is displayed.
    *
    * @param data The data containing either information about the end, as well that who is the winner if there is one.
    */
  private def handlesGameEnd(data: JValue): Unit = {
    playerGB.showLayer()
    opponentGB.showLayer()
    readyButton.style.display = "none"
    endButtons.style.display = "block"
    suppressLeaveWarning()

    if (wonExists(data)) {
      val winnerGB = if (getWonValue(data)) playerGB else opponentGB
      val loserGB = if (winnerGB == playerGB) opponentGB else playerGB

      winnerGB.setLayerText("Win")
      loserGB.setLayerText("Lose")
    }
    else if (drawExists(data)) {
      playerGB.setLayerText("Draw")
      opponentGB.setLayerText("Draw")
    }
  }

  /**
    * Handles game-related updates (grids, game info, pieces positions).
    *
    * @param data The data that contains the various updates.
    */
  private def handleGame(data: JValue): Unit = {
    playerGB.hideLayer()
    opponentGB.hideLayer()
    readyButton.style.display = "none"

    val opponent = getOpponentValue(data)
    drawGridIfExists(data, GameAPIKeys.gameGrid, opponent)
    drawGridIfExists(data, GameAPIKeys.nextPieceGrid, opponent)
    changeInfoIfExists(data, GameAPIKeys.piecesPlaced, opponent)
    changeInfoIfExists(data, GameAPIKeys.points, opponent)
    updatePiecePositionsIfExists(data, opponent)
  }

  /**
    * Draw the specified grid if it exists in the received data.
    *
    * The grid can be either the game grid or the grid displaying the next piece.
    *
    * @param data The data that potentially contains the grid.
    * @param key The key of the grid to retrieve in the data if it exists.
    * @param opponent Whether it is the opponent's or the player's grid.
    */
  private def drawGridIfExists(data: JValue, key: String, opponent: Boolean): Unit = {
    if (data.isDefinedAt(key)) {
      val grid = getGrid(data, key)
      val gb = if (opponent) opponentGB else playerGB

      key match {
        case GameAPIKeys.gameGrid => gb.updateGameGrid(grid)
        case GameAPIKeys.nextPieceGrid => gb.updateNextPieceGrid(grid)
      }
    }
  }

  /**
    * Updates the positions of the current piece if they are in the received data.
    *
    * @param data The received data that potentially contains new piece positions.
    * @param opponent Whether it is the opponent's or the player's piece positions.
    */
  private def updatePiecePositionsIfExists(data: JValue, opponent: Boolean): Unit = {
    if (piecePositionsExists(data)) {
      val gb = if (opponent) opponentGB else playerGB
      gb.updatePiecePositions(getPiecePositionsValue(data))
    }
  }

  /**
    * Changes the specified game info if it exists in the received data.
    *
    * The game info can be either the number of pieces placed or the points scored.
    *
    * @param data The data that potentially contains a new game info.
    * @param key The key of the value to retrieve in the data if it exists.
    * @param opponent Whether it should be the opponent or the player box to update with the info.
    */
  private def changeInfoIfExists(data: JValue, key: String, opponent: Boolean): Unit = {
    if (data.isDefinedAt(key)) {
      val gb = if (opponent) opponentGB else playerGB
      key match {
        case GameAPIKeys.piecesPlaced => gb.setPiecesPlaced(data(key).value.toString)
        case GameAPIKeys.points => gb.setPoints(data(key).value.toString)
      }
    }
  }

  /**
    * Helpers to improve readability when checking existence of a value in the received data.
    */
  private def errorExists(data: JValue): Boolean = data.isDefinedAt(GameAPIKeys.error)
  private def opponentUsernameExists(data: JValue): Boolean = data.isDefinedAt(GameAPIKeys.opponentUsername)
  private def readyExists(data: JValue): Boolean = data.isDefinedAt(GameAPIKeys.ready)
  private def wonExists(data: JValue): Boolean = data.isDefinedAt(GameAPIKeys.won)
  private def drawExists(data: JValue): Boolean = data.isDefinedAt(GameAPIKeys.draw)
  private def piecePositionsExists(data: JValue): Boolean = data.isDefinedAt(GameAPIKeys.piecePositions)

  /**
    * Helpers to improve readability when retrieving a value in the received data.
    */
  private def getError(data: JValue): String = data(GameAPIKeys.error).value.asInstanceOf[String]
  private def getOpponentUsername(data: JValue): String = data(GameAPIKeys.opponentUsername).value.asInstanceOf[String]
  private def getWonValue(data: JValue): Boolean = data(GameAPIKeys.won).value.asInstanceOf[Boolean]
  private def getOpponentValue(data: JValue): Boolean = data(GameAPIKeys.opponent).value.asInstanceOf[Boolean]
  private def getPiecePositionsValue(data: JValue): List[Position] = {
    data(GameAPIKeys.piecePositions).value.asInstanceOf[Seq[Seq[Int]]].map(l => (l.head, l.tail.head)).toList
  }
  private def getGrid(data: JValue, key: String): Array[Array[Boolean]] = {
    data(key).value.asInstanceOf[Seq[Seq[Boolean]]].map(_.toArray).toArray
  }
}

package game

import akka.actor.{ActorRef, PoisonPill}
import shared.Pieces._
import game.PiecesWithPosition.{GamePiece, NextPiece}
import models.Result
import play.api.libs.json.Json.JsValueWrapper
import shared.GameRules.nGameCols
import play.api.libs.json.{JsBoolean, JsObject, Json}
import shared.Actions._
import shared.{GameAPIKeys, GameRules}

import scala.concurrent.duration._


/**
  * Manages the game logic, loops and actions.
  *
  * @param p1 The first player.
  * @param p2 The second player.
  * @param gameManager The game manager used to execute end game actions (remove game and save score).
  */
class Game(p1: Player, p2: Player, gameManager: GameManager) {
  private val player1: PlayerWithState = PlayerWithState(p1)
  private val player2: PlayerWithState = PlayerWithState(p2)

  private var gameFinished: Boolean = false
  private var gameBeganAt: Long = System.currentTimeMillis()

  private val players: Map[Long, PlayerWithState] = Map(
    player1.user.id.get -> player1,
    player2.user.id.get -> player2
  )

  //Use the system's dispatcher as ExecutionContext
  private val system = akka.actor.ActorSystem("system")
  import system.dispatcher

  /**
    * Initializes the game and starts the game loops.
    */
  def start(): Unit = {
    player1.state.curPiece.addToGrid()
    player2.state.curPiece.addToGrid()

    player1.state.nextPiece.addToGrid()
    player2.state.nextPiece.addToGrid()

    broadcastState(player1)
    broadcastState(player2)

    gameBeganAt = System.currentTimeMillis()

    gameTick(player1)
    gameTick(player2)
  }

  /**
    * Moves the current piece according to the specified action.
    *
    * @param action The action to do with the piece.
    * @param id The id of the current player.
    */
  def movePiece(action: Action)(implicit id: Long): Unit = this.synchronized {
    val player = players(id)
    val gs = player.state

    val moved: Boolean = action match {
      case Left => gs.curPiece.moveLeft()
      case Right => gs.curPiece.moveRight()
      case Rotate => gs.curPiece.rotate()
      case Down => gs.curPiece.moveDown()
      case Fall => gs.curPiece.fall()
    }

    if (moved) {
      if (action == Fall) handlePieceBottom(player)
      else broadcastPiecePositions(player)
    }
  }

  /**
    * Puts back the current player in the game.
    *
    * Updates the actor ref of the player and sends him the current state of the game.
    *
    * @param out The new actor ref of the player.
    * @param id The id of th current player.
    */
  def putBackPlayerInGame(out: ActorRef)(implicit id: Long): Unit = this.synchronized {
    val player = players(id)
    val opp = opponent(player)

    player.changeActorRef(out)

    player.out ! Json.obj(GameAPIKeys.opponentUsername -> opp.user.username).toString()
    sendState(player, player)
    sendState(opp, player)
  }

  /**
    * Finds out who is the opponent of the specified player.
    *
    * @param player The specified player.
    * @return Who the opponent is.
    */
  private def opponent(player: PlayerWithState): PlayerWithState = {
    if (player == player1) player2
    else player1
  }

  /**
    * Buis the state of the game for the specified player in a JsObject.
    *
    * @param player The player which has the state to send.
    * @return The state of the game as a JsObject.
    */
  private def buildState(player: PlayerWithState): JsObject = {
    Json.obj(
      GameAPIKeys.gameGrid -> player.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> player.state.nextPieceGrid,
      GameAPIKeys.piecesPlaced -> player.state.piecesPlaced,
      GameAPIKeys.points -> player.state.points,
      piecePositionsToKeyVal(player)
    )
  }

  /**
    * Sends the state of the specified player to both the player and his opponent.
    *
    * @param player The specified player.
    */
  private def broadcastState(player: PlayerWithState): Unit = {
    broadcast(player, buildState(player))
  }

  /**
    * Sends the state of the specified player to the specified destination.
    *
    * @param dest Where to send the state.
    * @param playerWithStateToSend Which state to send.
    */
  private def sendState(playerWithStateToSend: PlayerWithState, dest: PlayerWithState): Unit = {
    dest.out ! (
      buildState(playerWithStateToSend) + (GameAPIKeys.opponent -> JsBoolean(dest != playerWithStateToSend))
    ).toString()
  }

  /**
    * Takes appropriate actions when a piece lands.
    *
    * @param player The player with his state that contains the piece.
    */
  private def handlePieceBottom(player: PlayerWithState): Unit = this.synchronized {
    val opp = opponent(player)
    val state = player.state
    val nBlocksAbove = state.curPiece.getPositions.minBy(_._1)._1

    if (nBlocksAbove <= 1) {
      lose(player)
    }

    val removed = removeCompletedLines(player.state)

    state.piecesPlaced += 1
    state.points += GameRules.pointsForPieceDown(nBlocksAbove, removed.length, state.gameSpeed)

    val newSpeed = GameRules.nextSpeed(state.gameSpeed, removed.length)
    state.gameSpeed = newSpeed
    opp.state.gameSpeed = newSpeed

    val oppLost = sendLinesToOpponent(removed, state, opp)
    if (oppLost) {
      lose(opp)
      return
    }

    generateNewPiece(player)
  }

  /**
    * Generates a new piece and adds it to the grid.
    *
    * @param player The player with his state that will be updated.
    */
  private def generateNewPiece(player: PlayerWithState): Unit = {
    val state = player.state
    state.curPiece = new GamePiece(state.nextPiece.piece, state.gameGrid)

    if (state.curPiece.wouldCollideIfAddedToGrid()) {
      lose(player)
      return
    }

    state.curPiece.addToGrid()

    state.nextPiece.removeFromGrid()
    state.nextPiece = new NextPiece(randomPiece(), state.nextPieceGrid)
    state.nextPiece.addToGrid()

    broadcastPiecePositions(player)

    broadcast(player, Json.obj(
      GameAPIKeys.gameGrid -> player.state.gameGrid,
      GameAPIKeys.nextPieceGrid -> player.state.nextPieceGrid,
      GameAPIKeys.piecesPlaced -> state.piecesPlaced,
      GameAPIKeys.points -> state.points
    ))
  }

  /**
    * The game loop that moves down the current piece of the player.
    *
    * @param player The player with his state containing the current piece.
    */
  private def gameTick(player: PlayerWithState): Unit = {
    if (gameFinished) {
      return
    }

    system.scheduler.scheduleOnce(player.state.gameSpeed.milliseconds) {
      if (!player.state.curPiece.moveDown()) handlePieceBottom(player)
      else broadcastPiecePositions(player)

      gameTick(player)
    }
  }

  /**
    * Sends the same information to the player and his opponent, with an additional Boolean "opponent" related
    * to whether the information is related to the opponent.
    *
    * @param player The player with his state
    * @param data The data to send.
    */
  private def broadcast(player: PlayerWithState, data: JsObject): Unit = {
    Network.broadcastWithOpponent(player.out, opponent(player).out, data)
  }

  /**
    * Sends the positions of the current piece of the specified player to both players.
    *
    * @param player The player that own the piece.
    */
  private def broadcastPiecePositions(player: PlayerWithState): Unit = {
    broadcast(player, Json.obj(piecePositionsToKeyVal(player)))
  }

  /**
    * Transforms the positions (tuples) as arrays of size two and makes a key -> value with them.
    * The key is the key related to piece positions and the values are the positions themselves.
    *
    * @param player The player that has the positions of the current piece.
    * @return The current piece positions as key -> value.
    */
  private def piecePositionsToKeyVal(player: PlayerWithState): (String, JsValueWrapper) = {
    GameAPIKeys.piecePositions -> player.state.curPiece.getPositions.map(p => Array(p._1, p._2))
  }

  /**
    * Makes the specified player lose the game.
    *
    * @param loser The player that will lose.
    */
  private def lose(loser: PlayerWithState): Unit = this.synchronized {
    if (!gameFinished) {
      gameFinished = true

      val winner = opponent(loser)

      loser.out ! Json.obj(GameAPIKeys.won -> JsBoolean(false)).toString()
      winner.out ! Json.obj(GameAPIKeys.won -> JsBoolean(true)).toString()

      loser.out ! PoisonPill
      winner.out ! PoisonPill

      val timeSpent = (System.currentTimeMillis() - gameBeganAt) / 1000

      gameManager.endGame(Result(
        None,
        winner.user.id.get, winner.state.points, winner.state.piecesPlaced,
        loser.user.id.get, loser.state.points, loser.state.piecesPlaced,
        timeSpent, None
      ))
    }
  }

  /**
    * Makes the player with the specified id lose the game.
    *
    * @param id The id of the player that will lose.
    */
  def lose(implicit id: Long): Unit = {
    lose(players(id))
  }

  /**
    * Removes the completed lines of the grid in the specified game state.
    *
    * The lines deleted are returned with their row index in the grid.
    *
    * @param state The game state that contains the grid.
    * @return The lines with their row index in the grid.
    */
  def removeCompletedLines(state: GameState): Array[(Array[Boolean], Int)] = {
    val (removed, kept) = state.gameGrid
      .zipWithIndex
      .partition(p => p._1.count(x => x) == nGameCols)

    val newValues = Array.ofDim[Boolean](removed.length, nGameCols) ++ kept.map(_._1.clone)
    state.updateGameGrid(newValues)

    removed
  }

  /**
    * Sends the completed lines specified to the opponent.
    *
    * The lines sent are the ones that the player completed in the state they were before completion.
    *
    * @param removed The lines removed, with their row indices.
    * @param state The state containing the positions of the current piece.
    * @param opp The opponent with his state containing the grid to udpate.
    * @return Whether the opponent loses when his grid is updated.
    */
  def sendLinesToOpponent(removed: Array[(Array[Boolean], Int)], state: GameState, opp: PlayerWithState): Boolean = {
    val linesBeforeCompleted: Array[Array[Boolean]] = removed.map(p => {
      val row: Array[Boolean] = p._1
      val i = p._2
      row.indices.map(col => !state.curPiece.getPositions.contains((i, col))).toArray
    })

    val toSend = linesBeforeCompleted.length match {
      case 1 | 2 | 3 => linesBeforeCompleted.take(linesBeforeCompleted.length - 1)
      case _ => linesBeforeCompleted
    }

    if (pushLinesToGrid(toSend, opp.state)) {
      return true
    }

    broadcastPiecePositions(opp)
    broadcast(opp, Json.obj(GameAPIKeys.gameGrid -> opp.state.gameGrid))
    false
  }

  /**
    * Pushes the specified lines at the bottom of the grid contained in the specified state.
    *
    * If the lines would make the current piece in the state overlap with the other blocs in the grid, the piece
    * is moved up and if it can't be moved upward anymore, the player owning the state should lose.
    *
    * @param toPush The lines to push to the grid in the specified state.
    * @param state The state containing the grid to update.
    * @return Whether the player owning the state should lose.
    */
  def pushLinesToGrid(toPush: Array[Array[Boolean]], state: GameState): Boolean = {
    if (toPush.nonEmpty) {
      val piece = state.curPiece

      piece.removeFromGrid()

      state.updateGameGrid(state.gameGrid.drop(toPush.length).map(_.clone) ++ toPush)

      while (piece.wouldCollideIfAddedToGrid()) {
        if (!piece.moveUpWithOnlyGridBoundsCheck(updateGridOnMove = false)) {
          return true
        }
      }

      piece.addToGrid()
    }
    false
  }
}

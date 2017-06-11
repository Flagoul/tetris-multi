package game.utils

import game.GameState
import game.player.PlayerWithState
import shared.GameRules.nGameCols
import shared.Types.Grid

/**
  * utility functions related to grid manipulation.
  */
object GridUtils {
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
    * Pushes the specified lines at the bottom of the grid contained in the specified state.
    *
    * If the lines would make the current piece in the state overlap with the other blocs in the grid, the piece
    * is moved up and if it can't be moved upward anymore, the player owning the state should lose.
    *
    * @param toPush The lines to push to the grid in the specified state.
    * @param state The state containing the grid to update.
    * @return Whether the player owning the state should lose.
    */
  def pushLinesToGrid(toPush: Grid, state: GameState): Boolean = {
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

  /**
    * Sends the completed lines specified to the opponent grid.
    *
    * The lines sent are the ones that the player completed in the state they were before completion.
    *
    * @param removed The lines removed, with their row indices.
    * @param state The state containing the positions of the current piece.
    * @param oppState The opponent's state containing the grid to update.
    * @return Whether the opponent loses when his grid is updated.
    */
  def sendLinesToOpponent(removed: Array[(Array[Boolean], Int)], state: GameState, oppState: GameState): Boolean = {
    val linesBeforeCompleted: Grid = removed.map(p => {
      val row: Array[Boolean] = p._1
      val i = p._2
      row.indices.map(col => !state.curPiece.getPositions.contains((i, col))).toArray
    })

    val toSend = linesBeforeCompleted.length match {
      case 1 | 2 | 3 => linesBeforeCompleted.take(linesBeforeCompleted.length - 1)
      case _ => linesBeforeCompleted
    }

    GridUtils.pushLinesToGrid(toSend, oppState)
  }
}

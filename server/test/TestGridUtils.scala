import game.utils.GridUtils
import org.scalatest.{BeforeAndAfter, FunSuite, PrivateMethodTester}
import game.GameState
import shared.GameRules.{nGameCols, nGameRows}

/**
  * Tests fot the GridUtils functions.
  */
class TestGridUtils extends FunSuite with BeforeAndAfter with PrivateMethodTester {
  val state = new GameState()
  val emptyGrid: Grid = Array.ofDim[Boolean](nGameRows, nGameCols)
  val emptyRow: Array[Boolean] = Array.ofDim[Boolean](nGameCols)
  var grid: Grid = _

  val incompleteLine: Array[Boolean] = emptyRow.clone()
  incompleteLine(0) = true

  before {
    grid = emptyGrid.map(_.clone)
  }

  test("Lines deletion: 1 line") {
    grid(nGameRows-1) = emptyRow.map(_ => true)

    state.updateGameGrid(grid)
    val removed = GridUtils.removeCompletedLines(state)

    assert(removed.length == 1)
    assert(state.gameGrid(nGameRows-1).deep == emptyRow.deep)
  }

  test("Lines deletion: 2 lines spaced and other blocks") {
    grid(nGameRows-4) = incompleteLine
    grid(nGameRows-3) = emptyRow.map(_ => true)
    grid(nGameRows-2) = incompleteLine
    grid(nGameRows-1) = emptyRow.map(_ => true)

    state.updateGameGrid(grid)

    val removed = GridUtils.removeCompletedLines(state)

    assert(removed.length == 2)
    assert(state.gameGrid(nGameRows-4).deep == emptyRow.deep)
    assert(state.gameGrid(nGameRows-3).deep == emptyRow.deep)
    assert(state.gameGrid(nGameRows-2).deep == incompleteLine.deep)
    assert(state.gameGrid(nGameRows-1).deep == incompleteLine.deep)
  }
}

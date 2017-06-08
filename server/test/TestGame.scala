import org.scalatest.{BeforeAndAfter, FunSuite, PrivateMethodTester}
import game.{Game, GameState, Player}
import shared.GameRules.{nGameCols, nGameRows}

class TestGame extends FunSuite with BeforeAndAfter with PrivateMethodTester {
  val game = new Game(Player(null, null), Player(null, null), null)
  val state = new GameState()
  val emptyGrid: Array[Array[Boolean]] = Array.ofDim[Boolean](nGameRows, nGameCols)
  val emptyRow: Array[Boolean] = Array.ofDim[Boolean](nGameCols)
  var grid: Array[Array[Boolean]] = _

  val incompleteLine: Array[Boolean] = emptyRow.clone()
  incompleteLine(0) = true

  before {
    grid = emptyGrid.map(_.clone)
  }

  test("Lines deletion: 1 line") {
    grid(nGameRows-1) = emptyRow.map(_ => true)

    state.updateGameGrid(grid)
    val removed = game.removeCompletedLines(state)

    assert(removed.length == 1)
    assert(state.gameGrid(nGameRows-1).deep == emptyRow.deep)
  }

  test("Lines deletion: 2 lines spaced and other blocks") {
    grid(nGameRows-4) = incompleteLine
    grid(nGameRows-3) = emptyRow.map(_ => true)
    grid(nGameRows-2) = incompleteLine
    grid(nGameRows-1) = emptyRow.map(_ => true)

    state.updateGameGrid(grid)

    val removed = game.removeCompletedLines(state)

    assert(removed.length == 2)
    assert(state.gameGrid(nGameRows-4).deep == emptyRow.deep)
    assert(state.gameGrid(nGameRows-3).deep == emptyRow.deep)
    assert(state.gameGrid(nGameRows-2).deep == incompleteLine.deep)
    assert(state.gameGrid(nGameRows-1).deep == incompleteLine.deep)
  }
}

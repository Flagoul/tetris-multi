import org.scalatest.{BeforeAndAfter, FunSuite}
import game.GameState
import shared.GameRules.{nGameCols, nGameRows}

/**
  * Tests the game state.
  */
class TestGameState extends FunSuite with BeforeAndAfter {
  val state: GameState = new GameState()
  var values: Array[Array[Boolean]] = _

  before {
    values = Array.ofDim[Boolean](nGameRows, nGameCols)
  }

  test("Grid update") {
    values(nGameRows - 2)(0) = true
    values(nGameRows - 1)(0) = true

    state.updateGameGrid(values)

    assert(state.gameGrid.deep == values.deep)
  }

  test("Values as parameter are not changed") {
    values(nGameRows - 2)(0) = true
    values(nGameRows - 1)(0) = true

    val old = values.clone()
    state.updateGameGrid(values)

    assert(old.deep == values.deep)
  }
}

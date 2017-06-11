package shared

import shared.Types.Grid

import scala.util.Random

/**
  * The pieces of the game and their shape.
  */
object Pieces {
  abstract class Piece {
    val shape: Grid
  }

  object SquarePiece extends Piece {
    val shape: Grid = Array(
      Array(true, true),
      Array(true, true)
    )
  }

  object BarPiece extends Piece {
    val shape: Grid = Array(
      Array(true, true, true, true)
    )
  }

  object LPiece extends Piece {
    val shape: Grid = Array(
      Array(true, true, true),
      Array(true, false, false)
    )
  }

  object InvLPiece extends Piece {
    val shape: Grid = Array(
      Array(true, true, true),
      Array(false, false, true)
    )
  }

  object TPiece extends Piece {
    val shape: Grid = Array(
      Array(true, true, true),
      Array(false, true, false)
    )
  }

  object SPiece extends Piece {
    val shape: Grid = Array(
      Array(false, true, true),
      Array(true, true, false)
    )
  }

  object ZPiece extends Piece {
    val shape: Grid = Array(
      Array(true, true, false),
      Array(false, true, true)
    )
  }

  def randomPiece(): Piece = Random.shuffle(List(BarPiece, InvLPiece, LPiece, SPiece, SquarePiece, TPiece, ZPiece)).head
}

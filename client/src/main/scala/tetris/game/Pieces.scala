package tetris.game

abstract class Piece {
  def shape(): Array[Array[Boolean]]
}

object Square extends Piece {
  def shape(): Array[Array[Boolean]] = Array(
    Array(true, true),
    Array(true, true)
  )
}

object Bar extends Piece {
  def shape(): Array[Array[Boolean]] = Array(
    Array(true),
    Array(true),
    Array(true),
    Array(true)
  )
}

object L extends Piece {
  def shape(): Array[Array[Boolean]] = Array(
    Array(true, false),
    Array(true, false),
    Array(true, true)
  )
}

object InvL extends Piece {
  def shape(): Array[Array[Boolean]] = Array(
    Array(false, true),
    Array(false, true),
    Array(true, true)
  )
}

object T extends Piece {
  def shape(): Array[Array[Boolean]] = Array(
    Array(true, true, true),
    Array(false, true, false)
  )
}

object S extends Piece {
  def shape(): Array[Array[Boolean]] = Array(
    Array(false, true, true),
    Array(true, true, false)
  )
}

object Z extends Piece {
  def shape(): Array[Array[Boolean]] = Array(
    Array(true, true, false),
    Array(false, true, true)
  )
}


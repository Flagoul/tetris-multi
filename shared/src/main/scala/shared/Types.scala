package shared

/**
  * Regroups defined types.
  */
object Types {
  // A position in a grid on the form (row, col)
  type Position = (Int, Int)

  // A 2D grid.
  type Grid = Array[Array[Boolean]]
}

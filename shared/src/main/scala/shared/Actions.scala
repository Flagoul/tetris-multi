package shared

/**
  * Actions that the player can send to the server when playing.
  */
object Actions {
  case class Action(name: String)
  object Ready extends Action("start")
  object Leave extends Action("leave")
  object Left extends Action("left")
  object Rotate extends Action("rotate")
  object Right extends Action("right")
  object Down extends Action("down")
  object Fall extends Action("fall")
}

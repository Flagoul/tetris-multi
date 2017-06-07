package shared

object Actions {
  case class Action(name: String)
  object Start extends Action("start")
  object Left extends Action("left")
  object Rotate extends Action("rotate")
  object Right extends Action("right")
  object Down extends Action("down")
  object Fall extends Action("fall")
}

package shared

object Actions {
  class Action(val name: String)
  object Start extends Action("start")
  object Left extends Action("left")
  object Rotate extends Action("rotate")
  object Right extends Action("right")
  object Fall extends Action("fall")
}

package example.game

import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.Element

class GameBox(id: String) {
  val box: Element = dom.document.querySelector("#" + id)
  var gameCanvas: GameCanvas = new GameCanvas(10, 22, box.querySelector(".game").asInstanceOf[Canvas])
  var nextPieceCanvas: NextPiece = new NextPiece(5, 5, box.querySelector(".next-piece").asInstanceOf[Canvas])
  var gameInfo: GameInfo = new GameInfo()

  def draw(): Unit = {
    gameCanvas.draw()
    nextPieceCanvas.draw()
  }
}

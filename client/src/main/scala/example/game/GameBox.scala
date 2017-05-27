package example.game

import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.Element

class GameBox(id: String) {
  val box: Element = dom.document.querySelector("#" + id)
  val gameCanvas: GameCanvas = new GameCanvas(22, 10, box.querySelector(".game").asInstanceOf[Canvas])
  val nextPieceCanvas: NextPiece = new NextPiece(5, 5, box.querySelector(".next-piece canvas").asInstanceOf[Canvas])
  val gameInfo: GameInfo = new GameInfo()

  def draw(): Unit = {
    gameCanvas.draw()
    nextPieceCanvas.draw()
  }
}

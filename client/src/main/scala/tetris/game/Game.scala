package tetris.game

import json._
import org.scalajs.dom
import org.scalajs.dom.WebSocket
import org.scalajs.dom.raw.MessageEvent
import shared.Actions._
import shared.GameSettings._

import scala.util.Random

class Game {
  private val userGB: GameBox = new GameBox("user-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)
  private val opponentGB: GameBox = new GameBox("opponent-game-box", nGameRows, nGameCols, nNextPieceRows, nNextPieceCols)

  // A position in a grid on the form (row, col)
  type Position = (Int, Int)


  class PieceWithPosition(piece: Piece, grid: Array[Array[Boolean]], initPos: Position) {
    protected var positions: List[Position] = initPiecePositions()

    /**
      * Creates positions for the piece, based on the position where to start drawing.
      *
      * @return The positions of where the blocks of the pieces should be drawn.
      */
    def initPiecePositions(): List[Position] = {
      val pieceWidth = piece.shape.head.length
      val pieceHeight = piece.shape.length

      val positions = for {
        row <- 0 until pieceHeight
        col <- 0 until pieceWidth
        if piece.shape(row)(col)
      } yield (initPos._1 + row, initPos._2 + col)

      positions.toList
    }

    def getPositions: List[Position] = positions
  }

  class GamePiece(piece: Piece, gameGrid: Array[Array[Boolean]]) extends PieceWithPosition(
    piece, gameGrid,
    (1, nGameCols / 2 - piece.shape.head.length / 2)
  ) {

    def updateGrid(newPositions: List[Position]): Unit = {
      updateGridAtPositions(gameGrid, positions, value = false)
      updateGridAtPositions(gameGrid, newPositions, value = true)
      userGB.drawGame(gameGrid)

      positions = newPositions
    }

    def collides(newPositions: List[Position], inBounds: Position => Boolean): Boolean = {
      newPositions.exists(p => !inBounds(p) || (gameGrid(p._1)(p._2) && !positions.contains(p)))
    }

    private def move(transform: Position => Position, inBounds: Position => Boolean): Boolean = {
      val newPositions: List[Position] = positions.map(transform)

      val couldMove = !collides(newPositions, inBounds)
      if (couldMove) {
        updateGrid(newPositions)
      }

      couldMove
    }

    def inBoundLeft(p: Position): Boolean = p._2 >= 0
    def inBoundRight(p: Position): Boolean = p._2 < nGameCols
    def inBoundBottom(p: Position): Boolean = p._1 < nGameRows
    def inBounds(p: Position): Boolean = inBoundLeft(p) && inBoundRight(p) && inBoundBottom(p)

    def transformLeft(p: Position): Position = (p._1, p._2 - 1)
    def transformRight(p: Position): Position = (p._1, p._2 + 1)
    def transformDown(p: Position): Position = (p._1 + 1, p._2)

    def moveLeft(): Boolean = move(transformLeft, inBoundLeft)
    def moveRight(): Boolean = move(transformRight, inBoundRight)
    def moveDown(): Boolean = move(transformDown, inBoundBottom)

    def fall(): Unit = {
      var moved = false
      do {
        moved = moveDown()
      } while (moved)
    }

    def rotate(): Boolean = piece match {
      case SquarePiece => true
      case _ =>

        // The position of the center of the piece; it is the axis of rotation.
        // It is always the block at (0, 1) in the shape of the piece; to know the actual position we have to
        // compute how many "true" they are in the first line of the shape and use it as index in the actual positions.
        val center = positions(piece.shape.head.slice(0, 2).count(x => x) - 1)

        // origin of square surrounding piece
        val orig = (center._1 - 1, center._2 - 1)

        // translating positions relatively to origin and do transpose rotation for bar or 90° rotation for others
        val newPositions = positions.map(p => {
          val tRow = p._1 - orig._1
          val tCol = p._2 - orig._2

          piece match {
            case BarPiece => (tCol + orig._1, tRow + orig._2)
            case _ => (tCol + orig._1, 3 - tRow - 1 + orig._2)
          }
        })

        val couldMove = !collides(newPositions, inBounds)
        if (couldMove) {
          updateGrid(newPositions)
        }

        couldMove
    }
  }

  class NextPiece(piece: Piece, gameGrid: Array[Array[Boolean]]) extends PieceWithPosition(
    piece, gameGrid,
    (nNextPieceRows / 2 - piece.shape.length / 2 , nNextPieceCols / 2 - piece.shape.head.length / 2)
  )

  def randomPiece(): Piece = Random.shuffle(List(BarPiece, InvLPiece, LPiece, SPiece, SquarePiece, TPiece, ZPiece)).head

  def updateGridAtPositions(grid: Array[Array[Boolean]], positions: List[Position], value: Boolean): Unit = {
    for (pos <- positions) {
      grid(pos._1)(pos._2) = value
    }
  }

  def deleteCompletedLines(gameGrid: Array[Array[Boolean]]): Array[Array[Boolean]] = {
    val res = gameGrid.filterNot(row => row.count(x => x) == nGameCols)

    if (res.length < nGameRows) {
      return Array.ofDim[Boolean](nGameRows - res.length, nGameCols) ++ res
    }

    res
  }

  def sendAction(id: String, ws: WebSocket, action: Action): Unit = {
    val json: String = Map("id" -> id, "action" -> action.name).js.toDenseString
    ws.send(json)
  }

  def run(): Unit = {
    val host = dom.window.location.host
    val ws = new WebSocket(s"ws://$host/ws")

    var id: String = ""

    userGB.drawGame(Array.ofDim[Boolean](nGameRows, nGameCols))
    userGB.drawNextPiece(Array.ofDim[Boolean](nNextPieceRows, nNextPieceCols))
    opponentGB.drawGame(Array.ofDim[Boolean](nGameRows, nGameCols))
    opponentGB.drawNextPiece(Array.ofDim[Boolean](nNextPieceRows, nNextPieceCols))

    ws.onmessage = { (e: MessageEvent) =>
      println(e.data.toString)
      val data = JValue.fromString(e.data.toString)

      if (data("id") != JUndefined) {
        id = data("id").value.asInstanceOf[String]
      } else {
        val opponent = data("opponent").value.asInstanceOf[Boolean]

        // FIXME change to use seqs instead of arrays everywhere
        if (data("gameGrid") != JUndefined) {
          val grid = data("gameGrid").value.asInstanceOf[Seq[Seq[Boolean]]].map(_.toArray).toArray
          if (opponent) opponentGB.drawGame(grid)
          else userGB.drawGame(grid)
        }

        if (data("nextPieceGrid") != JUndefined) {
          val grid = data("nextPieceGrid").value.asInstanceOf[Seq[Seq[Boolean]]].map(_.toArray).toArray
          if (opponent) opponentGB.drawNextPiece(grid)
          else userGB.drawNextPiece(grid)
        }
      }
    }


    dom.window.onkeydown = { (e: dom.KeyboardEvent) =>
      // FIXME remove direct drawing after

      e.keyCode match {
        case 37 | 65 => sendAction(id, ws, Left)
        case 38 | 87 => sendAction(id, ws, Rotate)
        case 39 | 68 => sendAction(id, ws, Right)
        case 40 | 83 => sendAction(id, ws, Fall)
        case _ => println(e.keyCode);
      }
    }
  }
}

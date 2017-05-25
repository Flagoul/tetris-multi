package example.game

class Game {
  def run(): Unit = {
    val userGB = new GameBox("user-game-box")
    val opponentBG = new GameBox("opponent-game-box")

    println("Game launched")
  }
}

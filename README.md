# Tetris-multi
A webapp that was built in the context of our SCALA course at HEIG-VD. The idea was to build a webapp of our choice. 

In this project, an user can play tetris against other opponents in a 1-versus-1 game and see a leaderboard for the various stats (win/lose ratio, number of pieces placed, time played).

The frontend uses ScalaJS and the backend is made with play and uses Slick with MySQL to store user account and their stats. Websockets are used during the game to send user input and receive updates from server.

## Rules
The game follows the same rules for tetris in one player: a random piece is falling down and can be rotated in order to complete rows, that are then erased from the game grid. If the piece touches the upper bound of the game grid, the player loses.

Additionnally, the following rules apply:
- When n lines are completed, n - 1 lines are added at the bottom to the opponent grid, except for 4 lines where 4 are added.
- When a player completes a line, the game speed increases for both players.
- The number of points is computed according to the spaces above the piece placed, the rows completed when doing so and the game speed.

## Deploying the app

### Requirements
- Docker-compose

### Deployment

## Authors
[Benjamin Schubert](https://github.com/BenjaminSchubert/) and [Basile Vu](https://github.com/Flagoul/)

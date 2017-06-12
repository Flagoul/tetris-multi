# Tetris-multi
A webapp that was built in the context of our SCALA course at HEIG-VD. The idea was to build a webapp of our choice. 

In this project, an user can play tetris against other opponents in a 1-versus-1 game and see a leaderboard for the various stats (win/lose ratio, number of pieces placed, time played).

The frontend uses ScalaJS and the backend is made with play and uses Slick with MySQL to store user account and their stats. Websockets are used during the game to send user input and receive updates from server.

## Rules
The game follows the same rules for tetris in one player: a random piece is falling down and can be rotated in order to complete rows, that are then erased from the game grid. If the piece touches the upper bound of the game grid, the player loses.

Additionally, the following rules apply:
- When n lines are completed, n - 1 lines are added at the bottom to the opponent grid, except for 4 lines where 4 are added.
- When a player completes a line, the game speed increases for both players.
- The number of points is computed according to the spaces above the piece placed, the rows completed when doing so and the game speed.

## Deploying the app

The simplest way of deploying the app is by using the provided docker-compose file, which
will setup MySQL and everything else for you.


### Deploying with Docker

#### Requirements
- Docker-compose version 1.9+
- Docker version version 1.12+

Please refer to [the official installation guide](https://docs.docker.com/engine/installation/) for instruciton on how to install them.

#### Deployment

Multiple environment variables are available to configure your application.

- TETRIS_DB_PASSWORD: this is the password your MySQL database will use.
- TETRIS_SECRET: the secret Play will use to encrypt sessions and tokens.
- TETRIS_PORT: the port on which to export your application. Defaults to 9000.


You then spawn your application like that : 

    TETRIS_DB_PASSWORD=YOUR_PASSWORD TETRIS_SECRET=YOUR_SECRET docker-compose up

**Warning**: This takes a lot of time since it needs to install scala and sbt, download the dependencies and then build the project. You also need at least 2GB of free RAM for the build to be successful.

### Deploying manually

This application requires a MySQL Database. The configuration of the database is not 
explained here, please look at the official documentation for this.

You will need a user, a database and a password to configure the application.


To deploy the application, please refer to [Play's documentation](https://www.playframework.com/documentation/2.5.x/Production)
to know which version suites you the best.

To run the application, you need to add the following environment variables when
launching the app.

- TETRIS_DB_URL: the url without "mysql://" at which the Database is accessible. For example `localhost/tetris`
- TETRIS_DB_USER: user to connect to the database. For example `tetris`
- TETRIS_DB_PASSWORD: the password for the database user, For example `verysecure`

You might need to modify [server/conf/application.conf](server/conf/application.conf) if you have specific requirements.

When launching the application, you will also need to add 
`-Dplay.evolutions.db.default.autoApply=true -Dplay.crypto.secret=YOUR_APPLICATION_SECRET` in order to have it securely
deployed.

If you need more references, please look at [images/scala/runserver.sh](images/scala/runserver.sh) to how we run the server.
## Authors
[Benjamin Schubert](https://github.com/BenjaminSchubert/) and [Basile Vu](https://github.com/Flagoul/)

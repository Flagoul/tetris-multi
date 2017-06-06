#!/usr/bin/env sh

set -eux

rm -f /srv/app/server/target/universal/stage/RUNNING_PID

sleep 10

echo Starting Server

/srv/app/server/target/universal/stage/bin/server -Dplay.evolutions.db.default.autoApply=true -Dplay.crypto.secret=${TETRIS_SECRET}

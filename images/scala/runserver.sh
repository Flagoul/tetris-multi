#!/usr/bin/env sh

set -eux

rm -f /srv/app/server/target/universal/stage/RUNNING_PID

if [ -z ${TETRIS_SECRET} ]; then
    echo "Production Secret not set, aborting" >&2
    exit 1
fi

/srv/app/server/target/universal/stage/bin/server -Dplay.evolutions.db.default.autoApply=true -Dplay.crypto.secret=${TETRIS_SECRET}

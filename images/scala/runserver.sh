#!/usr/bin/env sh

set -eux

rm -f /srv/app/server/target/universal/stage/RUNNING_PID

/srv/app/server/target/universal/stage/bin/server
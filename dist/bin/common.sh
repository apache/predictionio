# PredictionIO Shared Shell Code

# This script should be sourced with $BASE set to the base of the repository

VERSION=0.3-SNAPSHOT

# Play framework related
PLAY_OPTS=""
PLAY_START_OPTS="-Dsbt.log.noformat=true -Dconfig.file=conf/predictionio.conf"

# Log related
LOGDIR="$BASE/logs"

# Play apps related
ADMIN_PORT=9000
API_PORT=8000
SCHEDULER_PORT=7000

ADMIN_DIR="$BASE/servers/predictionio-admin-server-$VERSION"
API_DIR="$BASE/servers/predictionio-output-api-$VERSION"
SCHEDULER_DIR="$BASE/servers/predictionio-scheduler-$VERSION"

ADMIN_LOG="$LOGDIR/admin.log"
API_LOG="$LOGDIR/api.log"
SCHEDULER_LOG="$LOGDIR/scheduler.log"

ADMIN_ERR="$LOGDIR/admin.err"
API_ERR="$LOGDIR/api.err"
SCHEDULER_ERR="$LOGDIR/scheduler.err"

# Kill the whole shell when Ctrl+C is pressed
trap "exit 1" INT

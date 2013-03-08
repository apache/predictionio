# PredictionIO Shared Shell Code

# Play framework related
PLAY_OPTS=
PLAY_START_OPTS=-Dsbt.log.noformat=true

# Log related
LOGDIR="$BASE/logs"

ADMIN_LOG="$LOGDIR/admin.log"
API_LOG="$LOGDIR/api.log"
SCHEDULER_LOG="$LOGDIR/scheduler.log"

ADMIN_ERR="$LOGDIR/admin.err"
API_ERR="$LOGDIR/api.err"
SCHEDULER_ERR="$LOGDIR/scheduler.err"

# Kill the whole shell when Ctrl+C is pressed
trap "exit 1" INT

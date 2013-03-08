#!/usr/bin/env sh

# PredictionIO Startup Script

# Get the absolute path of the build script
SCRIPT="$0"
while [ -h "$SCRIPT" ] ; do
	SCRIPT=`readlink "$SCRIPT"`
done

# Get the base directory of the repo
DIR=`dirname $SCRIPT`/..
cd $DIR
BASE=`pwd`

. "$BASE/bin/common.sh"

mkdir -p "$LOGDIR"

# Admin server
echo "Trying to start admin server..."
echo "Trying to start admin server at: `date`" >>"$ADMIN_LOG"
$BASE/adminServer/target/start $PLAY_START_OPTS -Dhttp.port=9000 >>"$ADMIN_LOG" 2>>"$ADMIN_ERR" &

# API server
echo "Trying to start API server..."
echo "Trying to start API server at: `date`" >>"$API_LOG"
$BASE/output/api/target/start $PLAY_START_OPTS -Dhttp.port=8000 >>"$API_LOG" 2>>"$API_ERR" &

# Scheduler server
echo "Trying to start scheduler server..."
echo "Trying to start scheduler server at: `date`" >>"$SCHEDULER_LOG"
$BASE/scheduler/target/start $PLAY_START_OPTS -Dhttp.port=7000 >>"$SCHEDULER_LOG" 2>>"$SCHEDULER_ERR" &

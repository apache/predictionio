#!/usr/bin/env sh

# PredictionIO Shutdown Script

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
. "$BASE/bin/vendors.sh"

# Admin server
echo "Trying to stop admin server..."
echo "Trying to stop admin server at: `date`" >>"$ADMIN_LOG"
cd $BASE/adminServer
$PLAY $PLAY_OPTS stop >>"$ADMIN_LOG" 2>>"$ADMIN_ERR"

# API server
echo "Trying to stop API server..."
echo "Trying to stop API server at: `date`" >>"$API_LOG"
cd $BASE/output/api
$PLAY $PLAY_OPTS stop >>"$API_LOG" 2>>"$API_ERR"

# Scheduler server
echo "Trying to stop scheduler server..."
echo "Trying to stop scheduler server at: `date`" >>"$SCHEDULER_LOG"
cd $BASE/scheduler
$PLAY $PLAY_OPTS stop >>"$SCHEDULER_LOG" 2>>"$SCHEDULER_ERR"

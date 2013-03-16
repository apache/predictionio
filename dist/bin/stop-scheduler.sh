#!/usr/bin/env sh

# PredictionIO Scheduler Server Shutdown Script

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

stop_play "scheduler" $SCHEDULER_DIR $SCHEDULER_OUT

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

# Admin server
echo "Trying to stop admin server..."
echo "Trying to stop admin server at: `date`" >>"$ADMIN_LOG"
if [ -e $ADMIN_DIR/RUNNING_PID ] ; then
	kill -TERM `cat $ADMIN_DIR/RUNNING_PID`
	echo "Stopped"
else
	echo "Cannot find $ADMIN_DIR/RUNNING_PID"
	echo "Server may not be running"
fi

# API server
echo "Trying to stop API server..."
echo "Trying to stop API server at: `date`" >>"$API_LOG"
if [ -e $API_DIR/RUNNING_PID ] ; then
	kill -TERM `cat $API_DIR/RUNNING_PID`
	echo "Stopped"
else
	echo "Cannot find $API_DIR/RUNNING_PID"
	echo "Server may not be running"
fi

# Scheduler server
echo "Trying to stop scheduler server..."
echo "Trying to stop scheduler server at: `date`" >>"$SCHEDULER_LOG"
if [ -e $SCHEDULER_DIR/RUNNING_PID ] ; then
	kill -TERM `cat $SCHEDULER_DIR/RUNNING_PID`
	echo "Stopped"
else
	echo "Cannot find $SCHEDULER_DIR/RUNNING_PID"
	echo "Server may not be running"
fi

echo ""
echo "Note: You must stop any running MongoDB/Hadoop processes manually"

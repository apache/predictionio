#!/usr/bin/env sh

# PredictionIO Startup Script

set -e

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

mkdir -p "$LOGDIR"

SERVER_WAIT=1
SERVER_RETRY=20

$BASE/bin/conncheck

# Scheduler server
echo "Trying to start scheduler server... \c"
echo "Trying to start scheduler server at: `date`" >>"$SCHEDULER_OUT"
$BASE/bin/predictionio-scheduler $PLAY_START_OPTS -Dhttp.port=$SCHEDULER_PORT -Dlogger.file=$BASE/conf/scheduler-logger.xml -Dpidfile.path=$BASE/scheduler.pid >>"$SCHEDULER_OUT" 2>>"$SCHEDULER_ERR" &
SERVER_TRY=1
while [ $SERVER_TRY -le $SERVER_RETRY ] ; do
    sleep $SERVER_WAIT
    if [ $(curl --write-out %{http_code} --silent --output /dev/null "localhost:$SCHEDULER_PORT") -eq 200 ] ; then
        echo "started"
        SERVER_TRY=$SERVER_RETRY
    elif [ $SERVER_TRY -eq $SERVER_RETRY ] ; then
        echo "failed ($SCHEDULER_PORT unreachable)"
        exit 1
    fi
    SERVER_TRY=$((SERVER_TRY+1))
done

#!/usr/bin/env sh

# PredictionIO Admin Server Startup Script

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

# Admin server
echo "Trying to start admin server... \c"
echo "Trying to start admin server at: `date`" >>"$ADMIN_OUT"
$BASE/bin/predictionio-admin $PLAY_START_OPTS -Dhttp.port=$ADMIN_PORT -Dlogger.file=$BASE/conf/admin-logger.xml -Dpidfile.path=$BASE/admin.pid >>"$ADMIN_OUT" 2>>"$ADMIN_ERR" &
SERVER_TRY=1
while [ $SERVER_TRY -le $SERVER_RETRY ] ; do
    sleep $SERVER_WAIT
    if [ $(curl --write-out %{http_code} --silent --output /dev/null "localhost:$ADMIN_PORT") -eq 303 ] ; then
        echo "started"
        SERVER_TRY=$SERVER_RETRY
    elif [ $SERVER_TRY -eq $SERVER_RETRY ] ; then
        echo "failed ($ADMIN_PORT unreachable)"
        exit 1
    fi
    SERVER_TRY=$((SERVER_TRY+1))
done

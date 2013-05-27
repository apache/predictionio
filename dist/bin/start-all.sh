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

# MongoDB
if vendor_mongodb_exists ; then
	while true; do
		read -p "Found MongoDB in vendors area. Do you want to start it? [y/n] " yn
		case $yn in
			[Yy]* ) start_mongodb; break;;
			[Nn]* ) break;;
			* ) echo "Please answer 'y' or 'n'.";;
		esac
	done
fi

$BASE/bin/conncheck

# Admin server
echo "Trying to start admin server... \c"
echo "Trying to start admin server at: `date`" >>"$ADMIN_OUT"
$BASE/bin/start-admin.sh $PLAY_START_OPTS -Dhttp.port=$ADMIN_PORT -Dlogger.file=$BASE/conf/admin-logger.xml >>"$ADMIN_OUT" 2>>"$ADMIN_ERR" &
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

# API server
echo "Trying to start API server... \c"
echo "Trying to start API server at: `date`" >>"$API_OUT"
$BASE/bin/start-api.sh $PLAY_START_OPTS -Dhttp.port=$API_PORT -Dlogger.file=$BASE/conf/api-logger.xml >>"$API_OUT" 2>>"$API_ERR" &
SERVER_TRY=1
while [ $SERVER_TRY -le $SERVER_RETRY ] ; do
	sleep $SERVER_WAIT
	if [ $(curl --write-out %{http_code} --silent --output /dev/null "localhost:$API_PORT") -eq 200 ] ; then
	    echo "started"
	    SERVER_TRY=$SERVER_RETRY
	elif [ $SERVER_TRY -eq $SERVER_RETRY ] ; then
	    echo "failed ($API_PORT unreachable)"
	    exit 1
	fi
	SERVER_TRY=$((SERVER_TRY+1))
done

# Scheduler server
echo "Trying to start scheduler server... \c"
echo "Trying to start scheduler server at: `date`" >>"$SCHEDULER_OUT"
$BASE/bin/start-scheduler.sh $PLAY_START_OPTS -Dhttp.port=$SCHEDULER_PORT -Dlogger.file=$BASE/conf/scheduler-logger.xml >>"$SCHEDULER_OUT" 2>>"$SCHEDULER_ERR" &
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

# Apache Hadoop
if vendor_hadoop_exists ; then
	while true; do
		read -p "Found Hadoop in vendors area. Do you want to start it? [y/n] " yn
		case $yn in
			[Yy]* ) start_hadoop; break;;
			[Nn]* ) break;;
			* ) echo "Please answer 'y' or 'n'.";;
		esac
	done
fi

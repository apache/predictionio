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

$BASE/bin/conncheck

. "$BASE/bin/common.sh"
. "$BASE/bin/vendors.sh"

mkdir -p "$LOGDIR"

# Admin server
echo "Trying to start admin server..."
echo "Trying to start admin server at: `date`" >>"$ADMIN_OUT"
$BASE/bin/start-admin.sh $PLAY_START_OPTS -Dhttp.port=$ADMIN_PORT -Dlogger.file=$BASE/conf/admin-logger.xml >>"$ADMIN_OUT" 2>>"$ADMIN_ERR" &

# API server
echo "Trying to start API server..."
echo "Trying to start API server at: `date`" >>"$API_OUT"
$BASE/bin/start-api.sh $PLAY_START_OPTS -Dhttp.port=$API_PORT -Dlogger.file=$BASE/conf/api-logger.xml >>"$API_OUT" 2>>"$API_ERR" &

# Scheduler server
echo "Trying to start scheduler server..."
echo "Trying to start scheduler server at: `date`" >>"$SCHEDULER_OUT"
$BASE/bin/start-scheduler.sh $PLAY_START_OPTS -Dhttp.port=$SCHEDULER_PORT -Dlogger.file=$BASE/conf/scheduler-logger.xml >>"$SCHEDULER_OUT" 2>>"$SCHEDULER_ERR" &

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

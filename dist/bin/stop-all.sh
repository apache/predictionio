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
$BASE/bin/stop-admin.sh

# API server
$BASE/bin/stop-api.sh

# Scheduler server
$BASE/bin/stop-scheduler.sh

# Apache Hadoop
if vendor_hadoop_exists ; then
	echo ""
	while true; do
		read -p "Found Hadoop in vendors area. Do you want to stop it? [y/n] " yn
		case $yn in
			[Yy]* ) stop_hadoop; break;;
			[Nn]* ) break;;
			* ) echo "Please answer 'y' or 'n'.";;
		esac
	done
fi

echo ""
echo "Note: You must stop any running MongoDB processes manually."

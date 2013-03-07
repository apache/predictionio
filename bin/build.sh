#!/usr/bin/env sh

# PredictionIO Build Script

# Utilities
command_exists () {
	command -v "$1" >/dev/null 2>&1
}

install_play () {
	VENDORS_PATH=$1
	mkdir -p $VENDORS_PATH
	cd $VENDORS_PATH
	curl -o play-2.1.0.zip http://downloads.typesafe.com/play/2.1.0/play-2.1.0.zip
	unzip play-2.1.0.zip
}

# Get the absolute path of the build script
SCRIPT="$0"
while [ -h "$SCRIPT" ] ; do
	SCRIPT=`readlink "$SCRIPT"`
done

# Get the base directory of the repo
DIR=`dirname $SCRIPT`/..
cd $DIR
BASE=`pwd`
VENDORS_PATH="$BASE/vendors"

# Detect existing installations in search path
if command_exists "sbt" ; then
	echo "Using sbt in search path. No additional JVM optimization will be set."
	SBT=sbt
else
	echo >&2 "Unable to locate sbt. Aborting."
	exit 1
fi

if command_exists "play" ; then
	PLAY=play
else
	if [ -x $VENDORS_PATH/play-2.1.0/play ] ; then
		echo "Using play in vendors."
		PLAY=$VENDORS_PATH/play-2.1.0/play
	elif install_play "$VENDORS_PATH" ; then
		echo "Going to download and install Play Framework 2.1.0..."
		PLAY=$VENDORS_PATH/play-2.1.0/play
	else
		echo >&2 "Unable to locate play and automatic installation failed. Aborting."
		exit 1
	fi
fi

# Full rebuild?
if test "$REBUILD" = "1" ; then
	echo "Rebuild set."
	CLEAN=clean
else
	echo "Incremental build set."
	CLEAN=
fi

# Build commons
cd $BASE/commons
$SBT $CLEAN update +publish

# Build output
cd $BASE/output
$SBT $CLEAN update +publish

# Build process assembly
cd $BASE/process/hadoop/scala
$SBT $CLEAN update assembly

# Build MAP@k Top-k Items Collector
cd $BASE/process/hadoop/scala/engines/itemrec/evaluations/topkitems
$SBT $CLEAN update assembly

# Build user tool
cd $BASE/tools/users
$SBT $CLEAN update pack

# Build admin server
cd $BASE/adminServer
$PLAY $CLEAN update compile

# Build API server
cd $BASE/output/api
$PLAY $CLEAN update compile

# Build scheduler server
cd $BASE/scheduler
$PLAY $CLEAN update compile

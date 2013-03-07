#!/usr/bin/env sh

# PredictionIO Build Script

# Utilities
command_exists () {
	command -v "$1" >/dev/null 2>&1
}

install_sbt () {
	echo "Going to download and install sbt 0.12.2..."
	local VENDORS_PATH=$1/sbt-0.12.2
	mkdir -p $VENDORS_PATH
	cd $VENDORS_PATH
	curl -o sbt-launch.jar http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/0.12.2/sbt-launch.jar
	echo 'java -Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512M -jar `dirname $0`/sbt-launch.jar "$@"' > sbt
	chmod a+x sbt
}

install_play () {
	echo "Going to download and install Play Framework 2.1.0..."
	local VENDORS_PATH=$1
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

# Third party software
VENDORS_PATH="$BASE/vendors"
VENDOR_SBT="$VENDORS_PATH/sbt-0.12.2/sbt"
VENDOR_PLAY="$VENDORS_PATH/play-2.1.0/play"

# Detect existing installations in search path
if command_exists "sbt" ; then
	echo "Using sbt in search path. No additional JVM optimization will be set."
	SBT=sbt
elif [ -x "$VENDOR_SBT" ] ; then
	echo "Using sbt in vendors."
	SBT="$VENDOR_SBT"
elif install_sbt "$VENDORS_PATH" ; then
	SBT="$VENDOR_SBT"
else
	echo "Unable to locate sbt and automatic installation failed. Aborting." >&2
	exit 1
fi

if command_exists "play" ; then
	PLAY=play
elif [ -x "$VENDOR_PLAY" ] ; then
	echo "Using play in vendors."
	PLAY="$VENDOR_PLAY"
elif install_play "$VENDORS_PATH" ; then
	PLAY="$VENDOR_PLAY"
else
	echo "Unable to locate play and automatic installation failed. Aborting." >&2
	exit 1
fi

# Full rebuild?
if test "$REBUILD" = "1" ; then
	echo "Rebuild set."
	CLEAN=clean
else
	echo "Incremental build set. Use \"REBUILD=1 $0\" for clean rebuild."
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

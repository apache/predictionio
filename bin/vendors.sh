#!/usr/bin/env sh

# PredictionIO Third Party Software Utilities

# Utilities
command_exists () {
	command -v "$1" >/dev/null 2>&1
}

install_sbt () {
	echo "Going to download and install sbt 0.12.3..."
	local VENDORS_PATH=$1/sbt-0.12.3
	mkdir -p $VENDORS_PATH
	cd $VENDORS_PATH
	curl -o sbt-launch.jar http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/0.12.3/sbt-launch.jar
	echo 'java -Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512M -jar `dirname $0`/sbt-launch.jar "$@"' > sbt
	chmod a+x sbt
}

install_play () {
	echo "Going to download and install Play Framework 2.1.1..."
	local VENDORS_PATH=$1
	mkdir -p $VENDORS_PATH
	cd $VENDORS_PATH
	curl -o play-2.1.1.zip http://downloads.typesafe.com/play/2.1.1/play-2.1.1.zip
	unzip play-2.1.1.zip
}

install_mahout () {
	echo "Going to download and build Apache Mahout 0.8 Build 1993..."
	mkdir -p $LIB_MAHOUT
	cd $LIB_MAHOUT
	if [ ! -f mahout-core-0.8-SNAPSHOT.jar -o ! -f mahout-math-0.8-SNAPSHOT.jar ] ; then
		rm -rf $LIB_MAHOUT
		mkdir -p $LIB_MAHOUT
		cd $LIB_MAHOUT
		curl -o mahout-core-0.8-SNAPSHOT.jar http://download.prediction.io/mahout-snapshots/1993/mahout-core-0.8-SNAPSHOT.jar
		curl -o mahout-math-0.8-SNAPSHOT.jar http://download.prediction.io/mahout-snapshots/1993/mahout-math-0.8-SNAPSHOT.jar
	fi
	mkdir -p $VENDOR_MAHOUT
	cd $VENDOR_MAHOUT
	if [ ! -f mahout-core-0.8-SNAPSHOT-job.jar ] ; then
		curl -o mahout-core-0.8-SNAPSHOT-job.jar http://download.prediction.io/mahout-snapshots/1993/mahout-core-0.8-SNAPSHOT-job.jar
	fi
}

# Third party software
VENDORS_PATH="$BASE/vendors"
VENDOR_SBT="$VENDORS_PATH/sbt-0.12.3/sbt"
VENDOR_PLAY="$VENDORS_PATH/play-2.1.1/play"
VENDOR_MAHOUT="$VENDORS_PATH/mahout-0.8-snapshot"
LIB_MAHOUT="$BASE/process/engines/itemrec/algorithms/scala/mahout/commons/lib"

# Detect existing installations in search path
# Do not use existing sbt to enforce JVM settings
#if command_exists "sbt" ; then
#	echo "Using sbt in search path. No additional JVM optimization will be set."
#	SBT=sbt
if [ -x "$VENDOR_SBT" ] ; then
	echo "Using sbt in vendors."
	SBT="$VENDOR_SBT"
elif install_sbt "$VENDORS_PATH" ; then
	SBT="$VENDOR_SBT"
else
	echo "Unable to locate sbt and automatic installation failed. Aborting." >&2
	exit 1
fi

# Do not use existing Play due to potential compatibility issue
#if command_exists "play" ; then
#	PLAY=play
if [ -x "$VENDOR_PLAY" ] ; then
	echo "Using play in vendors."
	PLAY="$VENDOR_PLAY"
elif install_play "$VENDORS_PATH" ; then
	PLAY="$VENDOR_PLAY"
else
	echo "Unable to locate play and automatic installation failed. Aborting." >&2
	exit 1
fi

#if [ -r "$LIB_MAHOUT/mahout-core-0.8-SNAPSHOT.jar" -a -r "$LIB_MAHOUT/mahout-math-0.8-SNAPSHOT.jar" -a -r "$VENDOR_MAHOUT/mahout-core-0.8-SNAPSHOT-job.jar" ] ; then
#	echo "Using Apache Mahout 0.8 Build 1993 in vendors."
#elif install_mahout ; then
#	echo ""
#else
#	echo "Unable to locate Apache Mahout 0.8 Build 1993 and automatic installation failed. Aborting." >&2
#	exit 1
#fi

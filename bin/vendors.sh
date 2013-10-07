#!/usr/bin/env sh

# PredictionIO Third Party Software Utilities

# Utilities
command_exists () {
	command -v "$1" >/dev/null 2>&1
}

install_sbt () {
	echo "Going to download and install sbt 0.13.0..."
	local VENDORS_PATH=$1/sbt-0.13.0
	mkdir -p $VENDORS_PATH
	cd $VENDORS_PATH
	curl -O http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/0.13.0/sbt-launch.jar
	echo 'java -Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512M -jar `dirname $0`/sbt-launch.jar "$@"' > sbt
	chmod a+x sbt
}

install_play () {
	echo "Going to download and install Play Framework 2.2.0..."
	local VENDORS_PATH=$1
	mkdir -p $VENDORS_PATH
	cd $VENDORS_PATH
	curl -O http://downloads.typesafe.com/play/2.2.0/play-2.2.0.zip
	unzip play-2.2.0.zip
}

install_mahout () {
	echo "Going to download and install Apache Mahout 0.8..."
	mkdir -p $VENDORS_PATH
	cd $VENDORS_PATH
	echo "Retrieving Apache mirror list..."
	curl -o apache_mahout_mirrors.txt http://www.apache.org/dyn/closer.cgi/mahout/0.8/mahout-distribution-0.8.tar.gz
	MAHOUT_URL=$(cat apache_mahout_mirrors.txt | grep -m 1 "<strong>.*</strong>" | sed 's/.*<strong>//' | sed 's/<\/strong>.*//')
	echo "Found mirror: $MAHOUT_URL"
	curl -O $MAHOUT_URL
	tar zxvf mahout-distribution-0.8.tar.gz
}

# Third party software
VENDORS_PATH="$BASE/vendors"
VENDOR_SBT="$VENDORS_PATH/sbt-0.13.0/sbt"
VENDOR_PLAY="$VENDORS_PATH/play-2.2.0/play"
VENDOR_MAHOUT="$VENDORS_PATH/mahout-distribution-0.8"

# Detect existing installations in search path
# Do not use existing sbt to enforce JVM settings
#if command_exists "sbt" ; then
#	echo "Using sbt in search path. No additional JVM optimization will be set."
#	SBT=sbt
if [ -x "$VENDOR_SBT" ] ; then
	echo "Using sbt 0.13.0 in vendors."
	SBT="$VENDOR_SBT"
elif install_sbt "$VENDORS_PATH" ; then
	SBT="$VENDOR_SBT"
else
	echo "Unable to locate sbt 0.13.0 and automatic installation failed. Aborting." >&2
	exit 1
fi

# Do not use existing Play due to potential compatibility issue
#if command_exists "play" ; then
#	PLAY=play
if [ -x "$VENDOR_PLAY" ] ; then
	echo "Using Play Framework 2.2.0 in vendors."
	PLAY="$VENDOR_PLAY"
elif install_play "$VENDORS_PATH" ; then
	PLAY="$VENDOR_PLAY"
else
	echo "Unable to locate Play Framework 2.2.0 and automatic installation failed. Aborting." >&2
	exit 1
fi

if [ -r "$VENDOR_MAHOUT/mahout-core-0.8-job.jar" ] ; then
	echo "Using Apache Mahout 0.8 in vendors."
elif install_mahout ; then
	echo ""
else
	echo "Unable to locate Apache Mahout 0.8 and automatic installation failed. Aborting." >&2
	exit 1
fi

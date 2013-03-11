#!/usr/bin/env sh

# PredictionIO Package Script

# NOTE: Run this script after bin/build.sh to package things up

# This scripts package everything up into a deployable package that runs off a single configuration file

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

# Package admin server
echo "Going to package PredictionIO Admin Server..."
cd $ADMIN_DIR
$PLAY dist

# Package API server
echo "Going to package PredictionIO API Server..."
cd $API_DIR
$PLAY dist

# Package scheduler server
echo "Going to package PredictionIO Scheduler Server..."
cd $SCHEDULER_DIR
$PLAY dist

# Packaging
rm -rf "$PACKAGE_DIR"
mkdir -p "$PACKAGE_DIR/servers"
cd "$PACKAGE_DIR/servers"

unzip -q "$ADMIN_DIR/dist/predictionio-admin-server-$VERSION.zip"
unzip -q "$API_DIR/dist/predictionio-output-api-$VERSION.zip"
unzip -q "$SCHEDULER_DIR/dist/predictionio-scheduler-$VERSION.zip"

chmod a+x "$PACKAGE_DIR/servers/predictionio-admin-server-$VERSION/start"
chmod a+x "$PACKAGE_DIR/servers/predictionio-output-api-$VERSION/start"
chmod a+x "$PACKAGE_DIR/servers/predictionio-scheduler-$VERSION/start"

cp -R $DIST_DIR/bin $PACKAGE_DIR
cp $BASE/bin/quiet.sh $PACKAGE_DIR/bin
cp -R $DIST_DIR/conf $PACKAGE_DIR

mkdir -p "$PACKAGE_DIR/lib"
cp "$BASE/process/hadoop/scala/target/PredictionIO-Process-Hadoop-Scala-assembly-$VERSION.jar" "$PACKAGE_DIR/lib"
cp "$BASE/process/hadoop/scala/engines/itemrec/evaluations/topkitems/target/TopKItems-assembly-$VERSION.jar" "$PACKAGE_DIR/lib"

cd $DIST_DIR/target
rm "$PACKAGE_NAME.zip"
zip -q -r "$PACKAGE_NAME.zip" "$PACKAGE_NAME"

echo "Packaging finished at $DIST_DIR/target/$PACKAGE_NAME.zip"

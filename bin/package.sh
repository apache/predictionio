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
$PLAY stage

# Package API server
echo "Going to package PredictionIO API Server..."
cd $API_DIR
$PLAY stage

# Package scheduler server
echo "Going to package PredictionIO Scheduler Server..."
cd $SCHEDULER_DIR
$PLAY stage

# Packaging
rm -rf "$PACKAGE_DIR"
mkdir -p "$PACKAGE_DIR/bin"
mkdir -p "$PACKAGE_DIR/lib"

cp -n $ADMIN_DIR/target/universal/stage/bin/predictionio-admin $PACKAGE_DIR/bin
cp -n $ADMIN_DIR/target/universal/stage/lib/* $PACKAGE_DIR/lib
cp -n $API_DIR/target/universal/stage/bin/predictionio-api $PACKAGE_DIR/bin
cp -n $API_DIR/target/universal/stage/lib/* $PACKAGE_DIR/lib
cp -n $SCHEDULER_DIR/target/universal/stage/bin/predictionio-scheduler $PACKAGE_DIR/bin
cp -n $SCHEDULER_DIR/target/universal/stage/lib/* $PACKAGE_DIR/lib

cp -R $DIST_DIR/bin/* $PACKAGE_DIR/bin
cp $BASE/bin/quiet.sh $PACKAGE_DIR/bin
cp -R $DIST_DIR/conf $PACKAGE_DIR

cp "$BASE/process/engines/commons/evaluations/hadoop/scalding/target/scala-2.10/predictionio-process-commons-evaluations-hadoop-scalding-assembly-$VERSION.jar" "$PACKAGE_DIR/lib"
cp "$BASE/process/engines/commons/evaluations/scala/paramgen/target/scala-2.10/predictionio-process-commons-evaluations-paramgen-assembly-$VERSION.jar" "$PACKAGE_DIR/lib"
cp "$BASE/process/engines/commons/evaluations/scala/u2itrainingtestsplit/target/scala-2.10/predictionio-process-commons-evaluations-scala-u2itrainingtestsplittime-assembly-$VERSION.jar" "$PACKAGE_DIR/lib"
cp "$BASE/process/engines/itemrec/algorithms/hadoop/scalding/target/scala-2.10/predictionio-process-itemrec-algorithms-hadoop-scalding-assembly-$VERSION.jar" "$PACKAGE_DIR/lib"
cp "$BASE/process/engines/itemrec/algorithms/scala/mahout/target/scala-2.10/predictionio-process-itemrec-algorithms-scala-mahout-assembly-$VERSION.jar" "$PACKAGE_DIR/lib"
cp "$BASE/process/engines/itemrec/evaluations/hadoop/scalding/target/scala-2.10/predictionio-process-itemrec-evaluations-hadoop-scalding-assembly-$VERSION.jar" "$PACKAGE_DIR/lib"
cp "$BASE/process/engines/itemrec/evaluations/scala/topkitems/target/scala-2.10/predictionio-process-itemrec-evaluations-topkitems-assembly-$VERSION.jar" "$PACKAGE_DIR/lib"
cp "$BASE/process/engines/itemsim/algorithms/hadoop/scalding/target/scala-2.10/predictionio-process-itemsim-algorithms-hadoop-scalding-assembly-$VERSION.jar" "$PACKAGE_DIR/lib"
cp "$BASE/process/engines/itemsim/evaluations/hadoop/scalding/target/scala-2.10/predictionio-process-itemsim-evaluations-hadoop-scalding-assembly-$VERSION.jar" "$PACKAGE_DIR/lib"
cp "$BASE/process/engines/itemsim/evaluations/scala/topkitems/target/scala-2.10/predictionio-process-itemsim-evaluations-topkitems-assembly-$VERSION.jar" "$PACKAGE_DIR/lib"
cp -n $BASE/tools/conncheck/target/pack/lib/* $PACKAGE_DIR/lib
cp -n $BASE/tools/settingsinit/target/pack/lib/* $PACKAGE_DIR/lib
cp -n $BASE/tools/softwaremanager/target/pack/lib/* $PACKAGE_DIR/lib
cp -n $BASE/tools/users/target/pack/lib/* $PACKAGE_DIR/lib

mkdir -p $PACKAGE_DIR/vendors/mahout-distribution-0.8
cp $VENDOR_MAHOUT/mahout-core-0.8-job.jar $PACKAGE_DIR/vendors/mahout-distribution-0.8

cd $DIST_DIR/target
rm "$PACKAGE_NAME.zip"
zip -q -r "$PACKAGE_NAME.zip" "$PACKAGE_NAME"

echo "Packaging finished at $DIST_DIR/target/$PACKAGE_NAME.zip"

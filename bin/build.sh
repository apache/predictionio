#!/usr/bin/env sh

# PredictionIO Build Script

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

# Full rebuild?
if test "$REBUILD" = "1" ; then
	echo "Rebuild set."
	CLEAN=clean
else
	echo "Incremental build set. Use \"REBUILD=1 $0\" for clean rebuild."
	CLEAN=
fi

echo "Going to build PredictionIO..."
BASE_TARGETS="update publish"

# Build commons
#echo "Going to build PredictionIO Commons..."
#cd $BASE/commons
#$SBT $CLEAN update +publish

# Build output
#echo "Going to build PredictionIO Output..."
#cd $BASE/output
#$SBT $CLEAN update +publish

# Build process commons
#echo "Going to build PredictionIO Process Commons..."
#cd $BASE/process/commons/hadoop/scalding
#$SBT $CLEAN update +publish

if test "$SKIP_PROCESS" = "1" ; then
    echo "Skip building process assemblies."
else
    # Build process itemrec algo assembly
    echo "+ Assemble Process ItemRec Hadoop Scalding Algorithms"
    BASE_TARGETS="$BASE_TARGETS processItemRecAlgoHadoopScalding/assembly"
#    $SBT $CLEAN processItemRecAlgoHadoopScalding/assembly
#    cd $BASE/process/engines/itemrec/algorithms/hadoop/scalding
#    $SBT $CLEAN update assembly
#
#    echo "Going to build PredictionIO Process ItemRec Scala Mahout Algorithms Assembly..."
#    cd $BASE/process/engines/itemrec/algorithms/scala/mahout
#    $SBT $CLEAN update assembly
#
#    # Build process itemrec eval assembly
#    echo "Going to build PredictionIO Process ItemRec Evaluations Assembly..."
#    cd $BASE/process/engines/itemrec/evaluations/hadoop/scalding
#    $SBT $CLEAN update assembly
#
#    # Build process itemrec parameter generator
#    echo "Going to build PredictionIO Parameter Generator Assembly..."
#    cd $BASE/process/engines/itemrec/evaluations/scala/paramgen
#    $SBT $CLEAN update assembly
#
#    # Build process itemrec training test split assembly
#    echo "Going to build PredictionIO Training-Test Split Assembly..."
#    cd $BASE/process/engines/itemrec/evaluations/scala/trainingtestsplit
#    $SBT $CLEAN update assembly
#
#    # Build process itemrec Top-k Items Collector
#    echo "Going to build PredictionIO ItemRec Top-k Items Collector Assembly..."
#    cd $BASE/process/engines/itemrec/evaluations/scala/topkitems
#    $SBT $CLEAN update assembly
#
#    # Build process itemsim algo assembly
#    echo "Going to build PredictionIO Process ItemSim Algorithms Assembly..."
#    cd $BASE/process/engines/itemsim/algorithms/hadoop/scalding
#    $SBT $CLEAN update assembly
#
#    # Build process itemsim eval assembly
#    echo "Going to build PredictionIO Process ItemSim Evaluations Assembly..."
#    cd $BASE/process/engines/itemsim/evaluations/hadoop/scalding
#    $SBT $CLEAN update assembly
#
#    # Build process itemsim Top-k Items Collector
#    echo "Going to build PredictionIO ItemSim Top-k Items Collector Assembly..."
#    cd $BASE/process/engines/itemsim/evaluations/scala/topkitems
#    $SBT $CLEAN update assembly
fi

# Build connection check tool
echo "+ Pack Connection Check Tool"
BASE_TARGETS="$BASE_TARGETS toolsConncheck/pack"

# Build settings initialization tool
echo "+ Pack Settings Initialization Tool"
BASE_TARGETS="$BASE_TARGETS toolsSettingsInit/pack"

# Build software manager
echo "+ Pack Software Manager"
BASE_TARGETS="$BASE_TARGETS toolsSoftwareManager/pack"

# Build user tool
echo "+ Pack User Tool"
BASE_TARGETS="$BASE_TARGETS toolsUsers/pack"

$SBT $CLEAN $BASE_TARGETS

# Build admin server
echo "Going to build PredictionIO Admin Server..."
cd $BASE/servers/admin
$PLAY $CLEAN update compile

# Build API server
echo "Going to build PredictionIO API Server..."
cd $BASE/servers/api
$PLAY $CLEAN update compile

# Build scheduler server
echo "Going to build PredictionIO Scheduler Server..."
cd $BASE/servers/scheduler
$PLAY $CLEAN update compile

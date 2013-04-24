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

# Build commons
echo "Going to build PredictionIO Commons..."
cd $BASE/commons
$SBT $CLEAN update +publish

# Build output
echo "Going to build PredictionIO Output..."
cd $BASE/output
$SBT $CLEAN update +publish

# Build process commons
echo "Going to build PredictionIO Process Commons..."
cd $BASE/process/commons/hadoop/scalding
$SBT $CLEAN update +publish

# Build process itemrec algo assembly
echo "Going to build PredictionIO Process ItemRec Hadoop Scalding Algorithms Assembly..."
cd $BASE/process/engines/itemrec/algorithms/hadoop/scalding
$SBT $CLEAN update assembly

echo "Going to build PredictionIO Process ItemRec Scala Mahout Algorithms Assembly..."
cd $BASE/process/engines/itemrec/algorithms/scala/mahout
$SBT $CLEAN update assembly

# Build process itemrec eval assembly
echo "Going to build PredictionIO Process ItemRec Evaluations Assembly..."
cd $BASE/process/engines/itemrec/evaluations/hadoop/scalding
$SBT $CLEAN update assembly

# Build process itemrec training test split assembly
echo "Going to build PredictionIO Training-Test Split Assembly..."
cd $BASE/process/engines/itemrec/evaluations/scala/trainingtestsplit
$SBT $CLEAN update assembly

# Build process itemrec Top-k Items Collector
echo "Going to build PredictionIO Top-k Items Collector..."
cd $BASE/process/engines/itemrec/evaluations/scala/topkitems
$SBT $CLEAN update assembly

# Build process itemsim algo assembly
echo "Going to build PredictionIO Process ItemRec Algorithms Assembly..."
cd $BASE/process/engines/itemsim/algorithms/hadoop/scalding
$SBT $CLEAN update assembly

# Build connection check tool
echo "Going to build PredictionIO Connection Check Tool..."
cd $BASE/tools/conncheck
$SBT $CLEAN update pack

# Build user tool
echo "Going to build PredictionIO User Tool..."
cd $BASE/tools/users
$SBT $CLEAN update pack

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

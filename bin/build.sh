#!/usr/bin/env sh

# PredictionIO Build Script

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

# Build process assembly
echo "Going to build PredictionIO Process Assembly..."
cd $BASE/process/hadoop/scala
$SBT $CLEAN update assembly

# Build MAP@k Top-k Items Collector
echo "Going to build PredictionIO MAP@k Top-k Items Collector..."
cd $BASE/process/hadoop/scala/engines/itemrec/evaluations/topkitems
$SBT $CLEAN update assembly

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

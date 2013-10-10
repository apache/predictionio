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

if test "$SKIP_PROCESS" = "1" ; then
    echo "Skip building process assemblies."
else
    echo "+ Assemble Process ItemRec Algorithms Hadoop Scalding"
    BASE_TARGETS="$BASE_TARGETS processItemRecAlgoHadoopScalding/assembly"

    echo "+ Assemble Process ItemRec Algorithms Scala Mahout"
    BASE_TARGETS="$BASE_TARGETS processItemRecAlgoScalaMahout/assembly"

    echo "+ Assemble Process ItemRec Evaluations Hadoop Scalding"
    BASE_TARGETS="$BASE_TARGETS processItemRecEvalHadoopScalding/assembly"

    echo "+ Assemble Process ItemRec Evaluations Scala Parameter Generator"
    BASE_TARGETS="$BASE_TARGETS processItemRecEvalScalaParamGen/assembly"

    echo "+ Assemble Process ItemRec Evaluations Scala Training-Test Splitter"
    BASE_TARGETS="$BASE_TARGETS processItemRecEvalScalaTrainingTestSplit/assembly"

    echo "+ Assemble Process ItemRec Evaluations Scala Top-k Items Collector"
    BASE_TARGETS="$BASE_TARGETS processItemRecEvalScalaTopKItems/assembly"

    echo "+ Assemble Process ItemSim Algorithms Hadoop Scalding"
    BASE_TARGETS="$BASE_TARGETS processItemSimAlgoHadoopScalding/assembly"

    echo "+ Assemble Process ItemSim Evaluations Hadoop Scalding"
    BASE_TARGETS="$BASE_TARGETS processItemSimEvalHadoopScalding/assembly"

    echo "+ Assemble Process ItemSim Evaluations Scala Top-k Items Collector"
    BASE_TARGETS="$BASE_TARGETS processItemSimEvalScalaTopKItems/assembly"
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

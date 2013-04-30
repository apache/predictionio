#!/usr/bin/env sh

# PredictionIO Software Installation

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

# Initialize settings
echo "Initializing PredictionIO Settings Database..."
$BASE/bin/settingsinit conf/init.json

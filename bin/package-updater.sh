#!/usr/bin/env sh

# PredictionIO Updater Package Script

# NOTE: Run this script after bin/build.sh to package things up as an updater

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

UPDATER_NAME="updater-$VERSION"

cd "$BASE/tools/softwaremanager/target"
rm -rf $UPDATER_NAME
rm -f "$UPDATER_NAME.zip"
cp -R pack $UPDATER_NAME
zip -q -r "$UPDATER_NAME.zip" "$UPDATER_NAME"

echo "Packaging finished at $BASE/tools/softwaremanager/target/$UPDATER_NAME.zip"

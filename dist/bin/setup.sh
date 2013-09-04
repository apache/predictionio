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
. "$BASE/bin/vendors.sh"

# MongoDB
if vendor_mongodb_exists ; then
	while true; do
		echo "Note: MongoDB connectivity is required to run setup."
		read -p "Found MongoDB in vendors area. Do you want to start it? [y/n] " yn
		case $yn in
			[Yy]* ) start_mongodb; break;;
			[Nn]* ) break;;
			* ) echo "Please answer 'y' or 'n'.";;
		esac
	done
fi

$BASE/bin/conncheck

# Initialize settings
echo "Initializing PredictionIO Settings Database..."
$BASE/bin/settingsinit conf/init.json

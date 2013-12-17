#!/usr/bin/env sh

# PredictionIO Startup Script

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

mkdir -p "$LOGDIR"

SERVER_WAIT=1
SERVER_RETRY=20

# MongoDB
if vendor_mongodb_exists ; then
    while true; do
        read -p "Found MongoDB in vendors area. Do you want to start it? [y/n] " yn
        case $yn in
            [Yy]* ) start_mongodb; break;;
            [Nn]* ) break;;
            * ) echo "Please answer 'y' or 'n'.";;
        esac
    done
fi

$BASE/bin/conncheck

$BASE/bin/start-admin.sh
$BASE/bin/start-api.sh
$BASE/bin/start-scheduler.sh

# Apache Hadoop
if vendor_hadoop_exists ; then
    while true; do
        read -p "Found Hadoop in vendors area. Do you want to start it? [y/n] " yn
        case $yn in
            [Yy]* ) start_hadoop; break;;
            [Nn]* ) break;;
            * ) echo "Please answer 'y' or 'n'.";;
        esac
    done
fi

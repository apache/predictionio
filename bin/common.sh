# PredictionIO Shared Shell Code

# This script should be sourced with $BASE set to the base of the repository

VERSION=0.7.3-SNAPSHOT

# Play framework related
PLAY_OPTS=
PLAY_START_OPTS=-Dsbt.log.noformat=true

# Log related
LOGDIR="$BASE/logs"

# Play apps related
ADMIN_DIR="$BASE/servers/admin"
API_DIR="$BASE/servers/api"
SCHEDULER_DIR="$BASE/servers/scheduler"

ADMIN_LOG="$LOGDIR/admin.log"
API_LOG="$LOGDIR/api.log"
SCHEDULER_LOG="$LOGDIR/scheduler.log"

ADMIN_ERR="$LOGDIR/admin.err"
API_ERR="$LOGDIR/api.err"
SCHEDULER_ERR="$LOGDIR/scheduler.err"

# Packaging related
PACKAGE_NAME="PredictionIO-$VERSION"
PACKAGE_NAME_LINUX32="PredictionIO-linux-i686-$VERSION"
PACKAGE_NAME_LINUX64="PredictionIO-linux-x86_64-$VERSION"
DIST_DIR="$BASE/dist"
PACKAGE_DIR="$DIST_DIR/target/$PACKAGE_NAME"
PACKAGE_DIR_LINUX32="$DIST_DIR/target/$PACKAGE_NAME_LINUX32"
PACKAGE_DIR_LINUX64="$DIST_DIR/target/$PACKAGE_NAME_LINUX64"

# Kill the whole shell when Ctrl+C is pressed
trap "exit 1" INT

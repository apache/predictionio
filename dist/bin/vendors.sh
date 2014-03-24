#!/usr/bin/env sh

# PredictionIO Third Party Software Utilities

# Third party software
VENDORS_PATH="$BASE/vendors"
VENDOR_HADOOP_PATH="$VENDORS_PATH/hadoop-1.2.1"
VENDOR_MONGODB_PATH="$VENDORS_PATH/mongodb-linux-x86_64-2.4.9"

VENDOR_MONGODB_VERSION="2.4.9"

VENDOR_HADOOP_NAME="Apache Hadoop 1.2.1"
VENDOR_MONGODB_NAME="MongoDB $VENDOR_MONGODB_VERSION"

# Utilities
command_exists () {
	command -v "$1" >/dev/null 2>&1
}

ostype () {
	case $OSTYPE in
		linux*)
			echo "linux";;
		darwin*)
			echo "osx";;
	esac
}

hosttype () {
	case $HOSTTYPE in
		i[3456]86)
			echo "i686";;
		x86_64)
			echo "x86_64";;
	esac
}

OS=$(ostype)
ARCH=$(hosttype)

process_exists () {
	echo $(ps -ef | grep "$1" | grep -v "grep" | wc -l)
}

install_mongodb () {
	local FN="mongodb-$OS-$ARCH-$VENDOR_MONGODB_VERSION.tgz"
	local URL="http://fastdl.mongodb.org/$OS/$FN"
	echo "Going to download and install $VENDOR_MONGODB_NAME ($URL)..."
	local VENDORS_PATH=$1
	mkdir -p $VENDORS_PATH
	cd $VENDORS_PATH
	curl -O $URL
	tar zxvf $FN
}

install_hadoop () {
	while true; do
		read -p "Please supply the absolute path to your Java installation: " JAVA_HOME
		if [ $JAVA_HOME ] && [ -d $JAVA_HOME ] && [ -x "$JAVA_HOME/bin/java" ] ; then
			echo "Using directory $JAVA_HOME as your Java installation..."
			break
		else
			echo "Please provide a valid Java installation directory"
		fi
	done
	echo "Going to download and install $VENDOR_HADOOP_NAME..."
	local VENDORS_PATH=$1
	mkdir -p $VENDORS_PATH
	cd $VENDORS_PATH
	curl -O http://archive.apache.org/dist/hadoop/common/hadoop-1.2.1/hadoop-1.2.1-bin.tar.gz
	tar zxvf hadoop-1.2.1-bin.tar.gz
	echo "Configuring Hadoop in pseudo-distributed mode..."
	cp ../conf/hadoop/* $VENDOR_HADOOP_PATH/conf
	echo "export JAVA_HOME=$JAVA_HOME" >> $VENDOR_HADOOP_PATH/conf/hadoop-env.sh
	echo "Configuring PredictionIO to use Hadoop in vendors area..."
	echo "io.prediction.commons.settings.hadoop.home=$VENDOR_HADOOP_PATH" >> ../conf/predictionio.conf
	echo "Trying to format HDFS..."
	$VENDOR_HADOOP_PATH/bin/hadoop namenode -format
}

start_mongodb () {
	echo "Going to start MongoDB..."
	mkdir -p "$VENDORS_PATH/mongodb/data"
	mkdir -p "$VENDORS_PATH/mongodb/logs"
	$VENDOR_MONGODB_PATH/bin/mongod --config conf/mongodb/mongodb.conf >/dev/null 2>&1 &
}

start_hadoop () {
	echo "Going to start Hadoop..."
	$VENDOR_HADOOP_PATH/bin/start-all.sh
}

stop_hadoop () {
	echo "Going to stop Hadoop..."
	$VENDOR_HADOOP_PATH/bin/stop-all.sh
}

vendor_mongodb_exists () {
	[ -e "$VENDOR_MONGODB_PATH/bin/mongod" ]
}

vendor_hadoop_exists () {
	[ -e "$VENDOR_HADOOP_PATH/bin/hadoop" ]
}

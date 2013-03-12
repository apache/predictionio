#!/usr/bin/env sh

# PredictionIO Third Party Software Utilities

# Third party software
VENDORS_PATH="$BASE/vendors"
VENDOR_HADOOP_PATH="$VENDORS_PATH/hadoop-1.0.4"
VENDOR_MAHOUT_PATH="$VENDORS_PATH/mahout-distribution-0.7"
VENDOR_MONGODB_PATH="$VENDORS_PATH/mongodb-linux-x86_64-2.2.3"

VENDOR_HADOOP_NAME="Apache Hadoop 1.0.4"
VENDOR_MAHOUT_NAME="Apache Mahout 0.7"
VENDOR_MONGODB_NAME="MongoDB 2.2.3 (64-bit Linux)"

# Utilities
command_exists () {
	command -v "$1" >/dev/null 2>&1
}

process_exists () {
	echo $(ps -ef | grep "$1" | grep -v "grep" | wc -l)
}

install_mongodb () {
	echo "Going to download and install $VENDOR_MONGODB_NAME..."
	local VENDORS_PATH=$1
	mkdir -p $VENDORS_PATH
	cd $VENDORS_PATH
	curl -o mongodb-linux-x86_64-2.2.3.tgz http://fastdl.mongodb.org/linux/mongodb-linux-x86_64-2.2.3.tgz
	tar zxvf mongodb-linux-x86_64-2.2.3.tgz
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
	echo "Retrieving Apache mirror list..."
	curl -o apache_hadoop_mirrors.txt http://www.apache.org/dyn/closer.cgi/hadoop/common/hadoop-1.0.4/hadoop-1.0.4.tar.gz
	HADOOP_URL=$(cat apache_hadoop_mirrors.txt | grep -m 1 "<strong>.*</strong>" | sed 's/.*<strong>//' | sed 's/<\/strong>.*//')
	echo "Found mirror: $HADOOP_URL"
	curl -o hadoop-1.0.4.tar.gz $HADOOP_URL
	tar zxvf hadoop-1.0.4.tar.gz
	echo "Configuring Hadoop in pseudo-distributed mode..."
	cp ../conf/hadoop/* $VENDOR_HADOOP_PATH/conf
	echo "export JAVA_HOME=$JAVA_HOME" >> $VENDOR_HADOOP_PATH/conf/hadoop-env.sh
	echo "Configuring PredictionIO to use Hadoop in vendors area..."
	echo "io.prediction.commons.settings.hadoop.home=$VENDOR_HADOOP_PATH" >> ../conf/predictionio.conf
	echo "Trying to format HDFS..."
	$VENDOR_HADOOP_PATH/bin/hadoop namenode -format
}

install_mahout () {
	echo "Going to download and install $VENDOR_MAHOUT_NAME..."
	local VENDORS_PATH=$1
	mkdir -p $VENDORS_PATH
	cd $VENDORS_PATH
	echo "Retrieving Apache mirror list..."
	curl -o apache_mahout_mirrors.txt http://www.apache.org/dyn/closer.cgi/mahout/0.7/mahout-distribution-0.7.tar.gz
	MAHOUT_URL=$(cat apache_mahout_mirrors.txt | grep -m 1 "<strong>.*</strong>" | sed 's/.*<strong>//' | sed 's/<\/strong>.*//')
	echo "Found mirror: $MAHOUT_URL"
	curl -o mahout-distribution-0.7.tar.gz $MAHOUT_URL
	tar zxvf mahout-distribution-0.7.tar.gz
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

vendor_mongodb_exists () {
	[ -e "$VENDOR_MONGODB_PATH/bin/mongod" ]
}

vendor_hadoop_exists () {
	[ -e "$VENDOR_HADOOP_PATH/bin/hadoop" ]
}

vendor_mahout_exists () {
	[ -e "$VENDOR_MAHOUT_PATH/mahout-core-0.7-job.jar" ]
}

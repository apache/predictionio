#!/usr/bin/env bash

# Copyright 2014 TappingStone, Inc.
#
# This script will install PredictionIO onto your computer!
#
# Documentation: http://docs.prediction.io
#
# License: http://www.apache.org/licenses/LICENSE-2.0

OS=`uname`
PIO_VERSION=0.8.4
SPARK_VERSION=1.2.0
ELASTICSEARCH_VERSION=1.4.2
HBASE_VERSION=0.98.6
PIO_DIR=$HOME/PredictionIO
USER_PROFILE=$HOME/.profile
PIO_FILE=PredictionIO-$PIO_VERSION.tar.gz
TEMP_DIR=/tmp

echo -e "\033[1;32mWelcome to PredictionIO $PIO_VERSION!\033[0m"

if [[ "$OS" = "Darwin" ]]; then
  echo "Mac OS detected!"
  SED_CMD="sed -i ''"
elif [[ "$OS" = "Linux" ]]; then
  echo -e "Linux OS detected!"
  SED_CMD="sed -i"
else
  echo -e "\033[1;31mYour OS $OS is not yet supported for automatic install :(\033[0m"
  echo -e "\033[1;31mPlease do a manual install!\033[0m"
  exit 1
fi

# Installation Paths
while [[ ! $response =~ ^([yY][eE][sS]|[yY])$ ]]; do
echo -e "\033[1mWhere would you like to install PredictionIO?\033[0m"
echo -n "Installation path ($PIO_DIR): "
read pio_dir
pio_dir=${pio_dir:-$PIO_DIR}

echo -n "Vendor path ($pio_dir/vendors): "
read vendors_dir
vendors_dir=${vendors_dir:-$pio_dir/vendors}

spark_dir=$vendors_dir/spark-$SPARK_VERSION
elasticsearch_dir=$vendors_dir/elasticsearch-$ELASTICSEARCH_VERSION
hbase_dir=$vendors_dir/hbase-$HBASE_VERSION
zookeeper_dir=$vendors_dir/zookeeper

echo "--------------------------------------------------------------------------------"
echo -e "\033[1;32mOK, looks good!\033[0m"
echo "You are going to install PredictionIO to: $pio_dir"
echo -e "Vendor applications will go in: $vendors_dir\n"
echo "Spark: $spark_dir"
echo "Elasticsearch: $elasticsearch_dir"
echo "HBase: $hbase_dir"
echo "ZooKeeper: $zookeeper_dir"
echo "--------------------------------------------------------------------------------"
echo -ne "\033[1mIs this correct?\033[0m [Y/n] "
read response
response=${response:-Y}
done

# Java
if [[ "$OS" = "Darwin" ]]; then
  echo -e "\033[1;36mStarting Java install...\033[0m"

  JAVA_VERSION=`echo "$(java -version 2>&1)" | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }'`
  JAVA_HOME=`/usr/libexec/java_home`

  echo "Your Java version is: $JAVA_VERSION"
  echo "JAVA_HOME is now set to: $JAVA_HOME"
  echo -e "\033[1;32mJava done!\033[0m"
elif [[ "$OS" = "Linux" ]]; then
  # Java
  echo -e "\033[1;36mStarting Java install...\033[0m"

  echo -e "\033[33mThis script requires superuser access!\033[0m"
  echo -e "\033[33mYou will be prompted for your password by sudo:\033[0m"

  sudo apt-get install openjdk-7-jdk -y

  JAVA_HOME=$(readlink -f /usr/bin/javac | sed "s:/bin/javac::")

  echo "JAVA_HOME is now set to: $JAVA_HOME"
  echo -e "\033[1;32mJava install done!\033[0m"
fi

# PredictionIO
echo -e "\033[1;36mStarting PredictionIO setup in:\033[0m $pio_dir"
cd $TEMP_DIR
if [[ ! -e $PIO_FILE ]]; then
  echo "Downloading PredictionIO..."
  curl -O http://download.prediction.io/$PIO_FILE
fi
tar zxf $PIO_FILE
rm -rf $pio_dir
mv PredictionIO-$PIO_VERSION $pio_dir

chown -R $USER $pio_dir

echo "Updating ~/.profile to include: $pio_dir"
PATH=$PATH:$pio_dir/bin
echo "export PATH=\$PATH:$pio_dir/bin" >> $USER_PROFILE

echo -e "\033[1;32mPredictionIO setup done!\033[0m"

mkdir $vendors_dir

# Spark
echo -e "\033[1;36mStarting Spark setup in:\033[0m $spark_dir"
if [[ ! -e spark-$SPARK_VERSION-bin-hadoop2.4.tgz ]]; then
  echo "Downloading Spark..."
  curl -O http://d3kbcqa49mib13.cloudfront.net/spark-$SPARK_VERSION-bin-hadoop2.4.tgz
fi
tar xf spark-$SPARK_VERSION-bin-hadoop2.4.tgz
rm -rf $spark_dir
mv spark-$SPARK_VERSION-bin-hadoop2.4 $spark_dir

echo "Updating: $pio_dir/conf/pio-env.sh"
$SED_CMD "s|SPARK_HOME=/path_to_apache_spark|SPARK_HOME=$spark_dir|g" $pio_dir/conf/pio-env.sh

echo -e "\033[1;32mSpark setup done!\033[0m"

# Elasticsearch
echo -e "\033[1;36mStarting Elasticsearch setup in:\033[0m $elasticsearch_dir"
if [[ ! -e elasticsearch-$ELASTICSEARCH_VERSION.tar.gz ]]; then
  echo "Downloading Elasticsearch..."
  curl -O https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-$ELASTICSEARCH_VERSION.tar.gz
fi
tar zxf elasticsearch-$ELASTICSEARCH_VERSION.tar.gz
rm -rf $elasticsearch_dir
mv elasticsearch-$ELASTICSEARCH_VERSION $elasticsearch_dir

echo "Updating: $elasticsearch_dir/config/elasticsearch.yml"
echo 'network.host: 127.0.0.1' >> $elasticsearch_dir/config/elasticsearch.yml

echo -e "\033[1;32mElasticsearch setup done!\033[0m"

# HBase
echo -e "\033[1;36mStarting HBase setup in:\033[0m $hbase_dir"
if [[ ! -e hbase-$HBASE_VERSION-hadoop2-bin.tar.gz ]]; then
  echo "Downloading HBase..."
  curl -O http://archive.apache.org/dist/hbase/hbase-$HBASE_VERSION/hbase-$HBASE_VERSION-hadoop2-bin.tar.gz
fi
tar zxf hbase-$HBASE_VERSION-hadoop2-bin.tar.gz
rm -rf $hbase_dir
mv hbase-$HBASE_VERSION-hadoop2 $hbase_dir

echo "Creating default site in: $hbase_dir/conf/hbase-site.xml"
cat <<EOT > $hbase_dir/conf/hbase-site.xml
<configuration>
  <property>
    <name>hbase.rootdir</name>
    <value>file://$hbase_dir</value>
  </property>
  <property>
    <name>hbase.zookeeper.property.dataDir</name>
    <value>$zookeeper_dir</value>
  </property>
</configuration>
EOT

echo "Updating: $hbase_dir/conf/hbase-env.sh to include $JAVA_HOME"
$SED_CMD "s|# export JAVA_HOME=/usr/java/jdk1.6.0/|export JAVA_HOME=$JAVA_HOME|" $hbase_dir/conf/hbase-env.sh

echo -e "\033[1;32mHBase setup done!\033[0m"

echo "Updating permissions on: $vendors_dir"

chown -R $USER $vendors_dir

$elasticsearch_dir/bin/elasticsearch -d
$hbase_dir/bin/start-hbase.sh

echo -e "\033[1;32mElasticserach and HBase started!\033[0m"

echo "--------------------------------------------------------------------------------"
echo -e "\033[1;32mInstallation of PredictionIO $PIO_VERSION complete!\033[0m"
echo -e "\033[1;33mIMPORTANT: You still have to start the eventserver manually:\033[0m"
echo -e "Run: '\033[1mpio eventserver --ip 0.0.0.0\033[0m'"
echo -e "Check the eventserver status with: '\033[1mcurl -i -X GET http://localhost:7070\033[0m'"
echo -e "Use: '\033[1mpio [train|deploy|...]\033[0m' commands"
echo -e "Please report any problems to: \033[1;34msupport@prediction.io\033[0m"
echo -e "\033[1;34mDocumentation at: http://docs.prediction.io\033[0m"
echo "--------------------------------------------------------------------------------"

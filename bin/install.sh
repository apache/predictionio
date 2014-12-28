#!/bin/sh
#
# Copyright 2014 TappingStone, Inc.
#
# This script will install PredictionIO onto your computer!
#
# Documentation: http://docs.prediction.io
#
# License: http://www.apache.org/licenses/LICENSE-2.0
#
#

OS=`uname`
PIO_VERSION=0.8.5
SPARK_VERSION=1.2.0
ELASTICSEARCH_VERSION=1.3.3
HBASE_VERSION=0.98.6

if [ $OS = "Darwin" ]
then
  echo "Installing on Mac"
  SED_CMD="sed -i ''"
elif [ $OS = "Linux" ]
then
  echo "Installing on Linux"
  SED_CMD="sed -i"
else
  echo "Platform not recognized! Aborting!"
  exit 1
fi

INSTALL_DIR=$HOME
USER_PROFILE=$HOME/.profile
TEMP_DIR=/tmp
PIO_DIR=$INSTALL_DIR/PredictionIO
PIO_FILE=PredictionIO-$PIO_VERSION.tar.gz
VENDORS_DIR=$PIO_DIR/vendors
SPARK_DIR=$VENDORS_DIR/spark-$SPARK_VERSION
ELASTICSEARCH_DIR=$VENDORS_DIR/elasticsearch-$ELASTICSEARCH_VERSION
HBASE_DIR=$VENDORS_DIR/hbase-$HBASE_VERSION
ZOOKEEPER_DIR=$VENDORS_DIR/zookeeper

# Java
if [ $OS = "Darwin" ]
  then
  echo "Starting Java on Mac..."

  JAVA_VERSION=`echo "$(java -version 2>&1)" | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }'`
  JAVA_HOME=`/usr/libexec/java_home`

  echo "Your Java version is: $JAVA_VERSION"
  echo "JAVA_HOME is now set to: $JAVA_HOME"
  echo "Java done!"

elif [ $OS = "Linux" ]
  then
  echo "This script requires superuser access."
  echo "You will be prompted for your password by sudo."

  # Java
  echo "Starting Java install on Linux..."
  sudo apt-get install openjdk-7-jdk -y

  JAVA_HOME=$(readlink -f /usr/bin/javac | sed "s:/bin/javac::")

  echo "JAVA_HOME is now set to: $JAVA_HOME"
  echo "Java install done!"
fi


# PredictionIO
echo "Starting PredictionIO setup in: $PIO_DIR"
cd $TEMP_DIR
if [ ! -e $PIO_FILE ]; then
  echo "Downloading PredictionIO..."
  curl -O http://download.prediction.io/$PIO_FILE
fi
tar zxf $PIO_FILE
rm -rf $PIO_DIR
mv PredictionIO-$PIO_VERSION $PIO_DIR

chown -R $USER $PIO_DIR

echo "Updating ~/.profile to include: $PIO_DIR"
PATH=$PATH:$PIO_DIR/bin
echo "export PATH=\$PATH:$PIO_DIR/bin" >> $USER_PROFILE

echo "PredictionIO setup done!"

mkdir $VENDORS_DIR

# Spark
echo "Starting Spark setup in: $SPARK_DIR"
if [ ! -e spark-$SPARK_VERSION-bin-hadoop2.4.tgz ]; then
  echo "Downloading Spark..."
  curl -O http://d3kbcqa49mib13.cloudfront.net/spark-$SPARK_VERSION-bin-hadoop2.4.tgz
fi
tar xf spark-$SPARK_VERSION-bin-hadoop2.4.tgz
rm -rf $SPARK_DIR
mv spark-$SPARK_VERSION-bin-hadoop2.4 $SPARK_DIR

echo "Updating: $PIO_DIR/conf/pio-env.sh"
$SED_CMD "s|SPARK_HOME=/path_to_apache_spark|SPARK_HOME=$SPARK_DIR|g" $PIO_DIR/conf/pio-env.sh

echo "Spark setup done!"

# Elasticsearch
echo "Starting Elasticsearch setup in $ELASTICSEARCH_DIR"
if [ ! -e elasticsearch-$ELASTICSEARCH_VERSION.tar.gz ]; then
  echo "Downloading Elasticsearch..."
  curl -O https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-$ELASTICSEARCH_VERSION.tar.gz
fi
tar zxf elasticsearch-$ELASTICSEARCH_VERSION.tar.gz
rm -rf $ELASTICSEARCH_DIR
mv elasticsearch-$ELASTICSEARCH_VERSION $ELASTICSEARCH_DIR


echo "Updating: $ELASTICSEARCH_DIR/config/elasticsearch.yml"
echo 'network.host: 127.0.0.1' >> $ELASTICSEARCH_DIR/config/elasticsearch.yml

echo "Elasticsearch setup done!"

# HBase
echo "Starting HBase setup in $HBASE_DIR"
if [ ! -e hbase-$HBASE_VERSION-hadoop2-bin.tar.gz ]; then
  echo "Downloading HBase..."
  curl -O http://archive.apache.org/dist/hbase/hbase-$HBASE_VERSION/hbase-$HBASE_VERSION-hadoop2-bin.tar.gz
fi
tar zxf hbase-$HBASE_VERSION-hadoop2-bin.tar.gz
rm -rf $HBASE_DIR
mv hbase-$HBASE_VERSION-hadoop2 $HBASE_DIR

echo "Creating default site in: $HBASE_DIR/conf/hbase-site.xml"
cat <<EOT > $HBASE_DIR/conf/hbase-site.xml
<configuration>
  <property>
    <name>hbase.rootdir</name>
    <value>file://$HBASE_DIR</value>
  </property>
  <property>
    <name>hbase.zookeeper.property.dataDir</name>
    <value>$ZOOKEEPER_DIR</value>
  </property>
</configuration>
EOT

echo "Updating: $HBASE_DIR/conf/hbase-env.sh to include $JAVA_HOME"
$SED_CMD "s|# export JAVA_HOME=/usr/java/jdk1.6.0/|export JAVA_HOME=$JAVA_HOME|" $HBASE_DIR/conf/hbase-env.sh

echo "HBase setup done!"

echo "Updating permissions on: $VENDORS_DIR"

chown -R $USER $VENDORS_DIR

echo "Starting Elasticserach and HBase!"

$ELASTICSEARCH_DIR/bin/elasticsearch -d
$HBASE_DIR/bin/start-hbase.sh

echo "Installation completed!"
echo "################################################################################"
echo "IMPORTANT: You still have to start the eventserver manually:"
echo "Run 'pio eventserver --ip 0.0.0.0'"
echo "Check the eventserver status with 'curl -i -X GET http://localhost:7070'"
echo "Use 'pio [train|deploy|...] commands"
echo "Please report any problems to support@prediction.io"
echo "Documentation at http://docs.prediction.io"
echo "################################################################################"

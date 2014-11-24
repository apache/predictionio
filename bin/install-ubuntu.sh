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

INSTALL_DIR=/home/$USER
TEMP_DIR=/tmp
PIO_VERSION=0.8.3-SNAPSHOT
PIO_DIR=$INSTALL_DIR/PredictionIO
PIO_FILE=PredictionIO-$PIO_VERSION.tar.gz
VENDORS_DIR=$PIO_DIR/vendors
SPARK_DIR=$VENDORS_DIR/spark-1.1.0
ELASTICSEARCH_DIR=$VENDORS_DIR/elasticsearch-1.3.2
HBASE_DIR=$VENDORS_DIR/hbase-0.98.6
ZOOKEEPER_DIR=$VENDORS_DIR/zookeeper

echo "This script requires superuser access."
echo "You will be prompted for your password by sudo."

# Java
echo "Starting Java install..."
sudo apt-get install openjdk-7-jdk -y

JAVA_HOME=$(readlink -f /usr/bin/javac | sed "s:/bin/javac::")

echo "JAVA_HOME is now set to: $JAVA_HOME"
echo "Java install done!"

# PredictionIO
echo "Starting PredictionIO setup in: $PIO_DIR"
cd $TEMP_DIR
if [ ! -e $PIO_FILE ]; then
  echo "Downloading PredictionIO..."
  wget http://download.prediction.io/$PIO_FILE
fi
tar zxf $PIO_FILE
rm -rf $PIO_DIR
mv PredictionIO-$PIO_VERSION $PIO_DIR

chown -R $USER:$USER $PIO_DIR

echo "Updating ~/.profile to include: $PIO_DIR"
PATH=$PATH:$PIO_DIR/bin
echo "export PATH=\$PATH:$PIO_DIR/bin" >> /home/$USER/.profile

echo "PredictionIO setup done!"

mkdir $VENDORS_DIR

# Spark
echo "Starting Spark setup in: $SPARK_DIR"
if [ ! -e spark-1.1.0-bin-hadoop2.4.tgz ]; then
  echo "Downloading Spark..."
  wget http://d3kbcqa49mib13.cloudfront.net/spark-1.1.0-bin-hadoop2.4.tgz
fi
tar xf spark-1.1.0-bin-hadoop2.4.tgz
rm -rf $SPARK_DIR
mv spark-1.1.0-bin-hadoop2.4 $SPARK_DIR

echo "Updating: $PIO_DIR/conf/pio-env.sh"
sed -i "s|SPARK_HOME=/path_to_apache_spark|SPARK_HOME=$SPARK_DIR|g" $PIO_DIR/conf/pio-env.sh

echo "Spark setup done!"

# Elasticsearch
echo "Starting Elasticsearch setup in $ELASTICSEARCH_DIR"
if [ ! -e elasticsearch-1.3.2.tar.gz ]; then
  echo "Downloading Elasticsearch..."
  wget https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.3.2.tar.gz
fi
tar zxf elasticsearch-1.3.2.tar.gz
rm -rf $ELASTICSEARCH_DIR
mv elasticsearch-1.3.2 $ELASTICSEARCH_DIR


echo "Updating: $ELASTICSEARCH_DIR/config/elasticsearch.yml"
echo 'network.host: 127.0.0.1' >> $ELASTICSEARCH_DIR/config/elasticsearch.yml

echo "Elasticsearch setup done!"

# HBase
echo "Starting HBase setup in $HBASE_DIR"
if [ ! -e hbase-0.98.6-hadoop2-bin.tar.gz ]; then
  echo "Downloading HBase..."
  wget http://archive.apache.org/dist/hbase/hbase-0.98.6/hbase-0.98.6-hadoop2-bin.tar.gz
fi
tar zxf hbase-0.98.6-hadoop2-bin.tar.gz
rm -rf $HBASE_DIR
mv hbase-0.98.6-hadoop2 $HBASE_DIR

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
sed -i "s|# export JAVA_HOME=/usr/java/jdk1.6.0/|export JAVA_HOME=$JAVA_HOME|" $HBASE_DIR/conf/hbase-env.sh

echo "HBase setup done!"

echo "Updating permissions on: $VENDORS_DIR"

chown -R $USER:$USER $VENDORS_DIR

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

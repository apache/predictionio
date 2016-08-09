#!/bin/bash -

set -e

echo '== Setting up Postgres... =='
service postgresql start
runuser postgres -c 'createuser -s root'
createdb root

psql -c "create user pio with password 'pio'" && createdb pio

echo '== Starting SSH... =='
service ssh start
ssh-keygen -b 2048 -t rsa -q -f /root/.ssh/id_rsa -N ""
cat /root/.ssh/id_rsa.pub >> /root/.ssh/authorized_keys

echo '== Starting HBase... =='
$HBASE_HOME/bin/start-hbase.sh

echo '== Starting standalone Spark cluster... =='
$SPARK_HOME/sbin/start-all.sh

echo '== Starting Elasticsearch... =='
$ELASTICSEARCH_HOME/bin/elasticsearch -d -p $PIO_HOME/es.pid

echo '== Copying distribution to PIO_HOME... =='
DISTRIBUTION_TAR=`find /pio_host -maxdepth 1 -name PredictionIO*SNAPSHOT.tar.gz | head -1`
tar zxvfC $DISTRIBUTION_TAR /
DIR_NAME=/`basename $DISTRIBUTION_TAR`
DIR_NAME=${DIR_NAME%.tar.gz}
mv $DIR_NAME/* $PIO_HOME/
mv /pio-env.sh $PIO_HOME/conf/pio-env.sh

echo '== Copying tests to a separate directory =='
mkdir /tests
cp -r /pio_host/tests/pio_tests /tests/pio_tests
export PYTHONPATH=/tests:$PYTHONPATH

# after initialization run given command
eval $@

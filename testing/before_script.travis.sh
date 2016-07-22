#!/bin/bash -

set -e

if [[ $BUILD_TYPE == Unit ]]; then

  # Download spark, hbase
  mkdir vendors
  wget http://d3kbcqa49mib13.cloudfront.net/spark-1.3.0-bin-hadoop2.4.tgz
  tar zxfC spark-1.3.0-bin-hadoop2.4.tgz vendors
  wget http://archive.apache.org/dist/hbase/hbase-1.0.0/hbase-1.0.0-bin.tar.gz
  tar zxfC hbase-1.0.0-bin.tar.gz vendors

  # Prepare pio environment variables
  set -a
  source conf/pio-env.sh.travis
  set +a

  # Create postgres database for PredictionIO
  psql -c 'create database predictionio;' -U postgres
  ./bin/travis/pio-start-travis

else # Integration Tests
  ./make-distribution.sh
fi

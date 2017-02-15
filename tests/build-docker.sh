#!/bin/bash -x

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ ! -f $DIR/docker-files/spark-1.4.0-bin-hadoop2.6.tgz ]; then
  wget http://d3kbcqa49mib13.cloudfront.net/spark-1.4.0-bin-hadoop2.6.tgz
  mv spark-1.4.0-bin-hadoop2.6.tgz $DIR/docker-files/
fi

if [ ! -f $DIR/docker-files/postgresql-9.4-1204.jdbc41.jar ]; then
  wget https://jdbc.postgresql.org/download/postgresql-9.4-1204.jdbc41.jar
  mv postgresql-9.4-1204.jdbc41.jar $DIR/docker-files/
fi

docker build -t predictionio/pio-testing $DIR

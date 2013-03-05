#!/bin/sh

# PredictionIO Build Script

# Get the base directory of the repo
cd `dirname $0`/..
base=`pwd`

# Build commons
cd $base/commons
sbt clean update +publish

# Build output
cd $base/output
sbt clean update +publish

# Build process assembly
cd $base/process/hadoop/scala
sbt clean update assembly

# Build MAP@k Top-k Items Collector
cd $base/process/hadoop/scala/engines/itemrec/evaluations/topkitems
sbt clean update assembly

# Build user tool
cd $base/tools/users
sbt clean update pack

# Build admin server
cd $base/adminServer
play clean update compile

# Build API server
cd $base/output/api
play clean update compile

# Build scheduler server
cd $base/scheduler
play clean update compile

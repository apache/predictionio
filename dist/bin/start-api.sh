#!/usr/bin/env sh

# PredictionIO Admin Server Startup Script

# Get the absolute path of the build script
SCRIPT="$0"
while [ -h "$SCRIPT" ] ; do
	SCRIPT=`readlink "$SCRIPT"`
done

# Get the base directory of the repo
DIR=`dirname $SCRIPT`/..
cd $DIR
BASE=`pwd`

. "$BASE/bin/common.sh"

LIB_DIR="$BASE/lib"

JARS="$LIB_DIR/predictionio-api_2.10-${VERSION}.jar"
JARS="${JARS}:$LIB_DIR/akka-actor_2.10.jar"
JARS="${JARS}:$LIB_DIR/akka-slf4j_2.10.jar"
JARS="${JARS}:$LIB_DIR/async-http-client.jar"
JARS="${JARS}:$LIB_DIR/casbah-commons_2.10-2.5.0.jar"
JARS="${JARS}:$LIB_DIR/casbah-core_2.10-2.5.0.jar"
JARS="${JARS}:$LIB_DIR/casbah-gridfs_2.10-2.5.0.jar"
JARS="${JARS}:$LIB_DIR/casbah-query_2.10-2.5.0.jar"
JARS="${JARS}:$LIB_DIR/commons-codec-1.7.jar"
JARS="${JARS}:$LIB_DIR/commons-lang3.jar"
JARS="${JARS}:$LIB_DIR/commons-logging.jar"
JARS="${JARS}:$LIB_DIR/config.jar"
JARS="${JARS}:$LIB_DIR/ehcache-core.jar"
JARS="${JARS}:$LIB_DIR/hamcrest-core-1.3.jar"
JARS="${JARS}:$LIB_DIR/httpclient.jar"
JARS="${JARS}:$LIB_DIR/httpcore.jar"
JARS="${JARS}:$LIB_DIR/jackson-core-asl.jar"
JARS="${JARS}:$LIB_DIR/jackson-mapper-asl.jar"
JARS="${JARS}:$LIB_DIR/javassist.jar"
JARS="${JARS}:$LIB_DIR/jcl-over-slf4j.jar"
JARS="${JARS}:$LIB_DIR/joda-convert.jar"
JARS="${JARS}:$LIB_DIR/joda-time.jar"
JARS="${JARS}:$LIB_DIR/jta.jar"
JARS="${JARS}:$LIB_DIR/jul-to-slf4j.jar"
JARS="${JARS}:$LIB_DIR/junit-4.11.jar"
JARS="${JARS}:$LIB_DIR/logback-classic.jar"
JARS="${JARS}:$LIB_DIR/logback-core.jar"
JARS="${JARS}:$LIB_DIR/mongo-java-driver-2.10.1.jar"
JARS="${JARS}:$LIB_DIR/netty.jar"
JARS="${JARS}:$LIB_DIR/nscala-time_2.10-0.2.0.jar"
JARS="${JARS}:$LIB_DIR/play_2.10.jar"
JARS="${JARS}:$LIB_DIR/play-exceptions.jar"
JARS="${JARS}:$LIB_DIR/play-iteratees_2.10.jar"
JARS="${JARS}:$LIB_DIR/predictionio-commons_2.10-${VERSION}.jar"
JARS="${JARS}:$LIB_DIR/predictionio-output_2.10-${VERSION}.jar"
JARS="${JARS}:$LIB_DIR/sbt-link.jar"
JARS="${JARS}:$LIB_DIR/scala-arm_2.10.jar"
JARS="${JARS}:$LIB_DIR/scala-io-core_2.10.jar"
JARS="${JARS}:$LIB_DIR/scala-io-file_2.10.jar"
JARS="${JARS}:$LIB_DIR/scala-library.jar"
JARS="${JARS}:$LIB_DIR/scala-reflect.jar"
JARS="${JARS}:$LIB_DIR/scala-stm_2.10.0.jar"
JARS="${JARS}:$LIB_DIR/scalaz-concurrent_2.10.jar"
JARS="${JARS}:$LIB_DIR/scalaz-core_2.10.jar"
JARS="${JARS}:$LIB_DIR/scalaz-effect_2.10.jar"
JARS="${JARS}:$LIB_DIR/signpost-commonshttp4.jar"
JARS="${JARS}:$LIB_DIR/signpost-core.jar"
JARS="${JARS}:$LIB_DIR/slf4j-api.jar"
JARS="${JARS}:$LIB_DIR/specs2_2.10.jar"
JARS="${JARS}:$LIB_DIR/templates_2.10.jar"

mkdir -p $API_DIR
exec java $@ -cp "$JARS" play.core.server.NettyServer $API_DIR

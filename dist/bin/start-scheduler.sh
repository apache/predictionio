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

JARS="${JARS}:$LIB_DIR/akka-actor_2.10.jar"
JARS="${JARS}:$LIB_DIR/akka-slf4j_2.10.jar"
JARS="${JARS}:$LIB_DIR/antlr.jar"
JARS="${JARS}:$LIB_DIR/asm-4.0.jar"
JARS="${JARS}:$LIB_DIR/asm-commons-4.0.jar"
JARS="${JARS}:$LIB_DIR/asm-commons.jar"
JARS="${JARS}:$LIB_DIR/asm-tree-4.0.jar"
JARS="${JARS}:$LIB_DIR/asm-tree.jar"
JARS="${JARS}:$LIB_DIR/asm-util.jar"
JARS="${JARS}:$LIB_DIR/asm.jar"
JARS="${JARS}:$LIB_DIR/async-http-client.jar"
JARS="${JARS}:$LIB_DIR/bijection-core_2.10-0.3.0.jar"
JARS="${JARS}:$LIB_DIR/c3p0-0.9.1.1.jar"
JARS="${JARS}:$LIB_DIR/casbah-commons_2.10-2.6.2.jar"
JARS="${JARS}:$LIB_DIR/casbah-core_2.10-2.6.2.jar"
JARS="${JARS}:$LIB_DIR/casbah-gridfs_2.10-2.6.2.jar"
JARS="${JARS}:$LIB_DIR/casbah-query_2.10-2.6.2.jar"
JARS="${JARS}:$LIB_DIR/chill_2.10-0.2.2.jar"
JARS="${JARS}:$LIB_DIR/classutil_2.10-1.0.1.jar"
JARS="${JARS}:$LIB_DIR/commons-codec-1.7.jar"
JARS="${JARS}:$LIB_DIR/commons-io-2.4.jar"
JARS="${JARS}:$LIB_DIR/commons-lang3.jar"
JARS="${JARS}:$LIB_DIR/commons-logging.jar"
JARS="${JARS}:$LIB_DIR/config.jar"
JARS="${JARS}:$LIB_DIR/ehcache-core.jar"
JARS="${JARS}:$LIB_DIR/grizzled-scala_2.10-1.1.2.jar"
JARS="${JARS}:$LIB_DIR/grizzled-slf4j_2.10-1.0.1.jar"
JARS="${JARS}:$LIB_DIR/httpclient.jar"
JARS="${JARS}:$LIB_DIR/httpcore.jar"
JARS="${JARS}:$LIB_DIR/jackson-core-asl.jar"
JARS="${JARS}:$LIB_DIR/jackson-mapper-asl.jar"
JARS="${JARS}:$LIB_DIR/javassist.jar"
JARS="${JARS}:$LIB_DIR/jcl-over-slf4j.jar"
JARS="${JARS}:$LIB_DIR/jline-2.6.jar"
JARS="${JARS}:$LIB_DIR/joda-convert.jar"
JARS="${JARS}:$LIB_DIR/joda-time.jar"
JARS="${JARS}:$LIB_DIR/jta.jar"
JARS="${JARS}:$LIB_DIR/jul-to-slf4j.jar"
JARS="${JARS}:$LIB_DIR/kryo-2.17.jar"
JARS="${JARS}:$LIB_DIR/logback-classic.jar"
JARS="${JARS}:$LIB_DIR/logback-core.jar"
JARS="${JARS}:$LIB_DIR/minlog-1.2.jar"
JARS="${JARS}:$LIB_DIR/mongo-java-driver-2.11.2.jar"
JARS="${JARS}:$LIB_DIR/mysql-connector-java-5.1.22.jar"
JARS="${JARS}:$LIB_DIR/netty.jar"
JARS="${JARS}:$LIB_DIR/nscala-time_2.10-0.2.0.jar"
JARS="${JARS}:$LIB_DIR/objenesis-1.2.jar"
JARS="${JARS}:$LIB_DIR/play-exceptions.jar"
JARS="${JARS}:$LIB_DIR/play-iteratees_2.10.jar"
JARS="${JARS}:$LIB_DIR/play_2.10.jar"
JARS="${JARS}:$LIB_DIR/predictionio-commons_2.10-0.5.1.jar"
JARS="${JARS}:$LIB_DIR/predictionio-scheduler_2.10-0.5.1.jar"
JARS="${JARS}:$LIB_DIR/quartz-2.1.7.jar"
JARS="${JARS}:$LIB_DIR/reflectasm-1.07-shaded.jar"
JARS="${JARS}:$LIB_DIR/sbt-link.jar"
JARS="${JARS}:$LIB_DIR/scala-arm_2.10.jar"
JARS="${JARS}:$LIB_DIR/scala-io-core_2.10.jar"
JARS="${JARS}:$LIB_DIR/scala-io-file_2.10.jar"
JARS="${JARS}:$LIB_DIR/scala-library.jar"
JARS="${JARS}:$LIB_DIR/scala-reflect.jar"
JARS="${JARS}:$LIB_DIR/scala-stm_2.10.0.jar"
JARS="${JARS}:$LIB_DIR/scalasti_2.10-1.0.0.jar"
JARS="${JARS}:$LIB_DIR/signpost-commonshttp4.jar"
JARS="${JARS}:$LIB_DIR/signpost-core.jar"
JARS="${JARS}:$LIB_DIR/slf4j-api.jar"
JARS="${JARS}:$LIB_DIR/stringtemplate.jar"
JARS="${JARS}:$LIB_DIR/templates_2.10.jar"

mkdir -p $SCHEDULER_DIR
exec java $@ -cp "$JARS" play.core.server.NettyServer $SCHEDULER_DIR

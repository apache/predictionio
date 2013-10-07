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

JARS="${JARS}:$LIB_DIR/ch.qos.logback.logback-classic-1.0.13.jar"
JARS="${JARS}:$LIB_DIR/ch.qos.logback.logback-core-1.0.13.jar"
JARS="${JARS}:$LIB_DIR/com.esotericsoftware.kryo.kryo-2.21.jar"
JARS="${JARS}:$LIB_DIR/com.esotericsoftware.minlog.minlog-1.2.jar"
JARS="${JARS}:$LIB_DIR/com.esotericsoftware.reflectasm.reflectasm-1.07.jar"
JARS="${JARS}:$LIB_DIR/com.fasterxml.jackson.core.jackson-annotations-2.2.2.jar"
JARS="${JARS}:$LIB_DIR/com.fasterxml.jackson.core.jackson-core-2.2.2.jar"
JARS="${JARS}:$LIB_DIR/com.fasterxml.jackson.core.jackson-databind-2.2.2.jar"
JARS="${JARS}:$LIB_DIR/com.github.nscala-time.nscala-time_2.10-0.2.0.jar"
JARS="${JARS}:$LIB_DIR/com.github.scala-incubator.io.scala-io-core_2.10-0.4.2.jar"
JARS="${JARS}:$LIB_DIR/com.github.scala-incubator.io.scala-io-file_2.10-0.4.2.jar"
JARS="${JARS}:$LIB_DIR/com.jsuereth.scala-arm_2.10-1.3.jar"
JARS="${JARS}:$LIB_DIR/com.ning.async-http-client-1.7.18.jar"
JARS="${JARS}:$LIB_DIR/com.twitter.bijection-core_2.10-0.4.0.jar"
JARS="${JARS}:$LIB_DIR/com.twitter.chill_2.10-0.2.3.jar"
JARS="${JARS}:$LIB_DIR/com.typesafe.akka.akka-actor_2.10-2.2.0.jar"
JARS="${JARS}:$LIB_DIR/com.typesafe.akka.akka-slf4j_2.10-2.2.0.jar"
JARS="${JARS}:$LIB_DIR/com.typesafe.config-1.0.2.jar"
JARS="${JARS}:$LIB_DIR/com.typesafe.netty.netty-http-pipelining-1.1.2.jar"
JARS="${JARS}:$LIB_DIR/com.typesafe.play.play-datacommons_2.10-2.2.0.jar"
JARS="${JARS}:$LIB_DIR/com.typesafe.play.play-exceptions-2.2.0.jar"
JARS="${JARS}:$LIB_DIR/com.typesafe.play.play-functional_2.10-2.2.0.jar"
JARS="${JARS}:$LIB_DIR/com.typesafe.play.play-iteratees_2.10-2.2.0.jar"
JARS="${JARS}:$LIB_DIR/com.typesafe.play.play-json_2.10-2.2.0.jar"
JARS="${JARS}:$LIB_DIR/com.typesafe.play.play_2.10-2.2.0.jar"
JARS="${JARS}:$LIB_DIR/com.typesafe.play.sbt-link-2.2.0.jar"
JARS="${JARS}:$LIB_DIR/com.typesafe.play.templates_2.10-2.2.0.jar"
JARS="${JARS}:$LIB_DIR/commons-codec.commons-codec-1.8.jar"
JARS="${JARS}:$LIB_DIR/commons-logging.commons-logging-1.1.1.jar"
JARS="${JARS}:$LIB_DIR/io.netty.netty-3.7.0.Final.jar"
JARS="${JARS}:$LIB_DIR/io.prediction.predictionio-admin-${VERSION}.jar"
JARS="${JARS}:$LIB_DIR/io.prediction.predictionio-commons_2.10-${VERSION}.jar"
JARS="${JARS}:$LIB_DIR/io.prediction.predictionio-output_2.10-${VERSION}.jar"
JARS="${JARS}:$LIB_DIR/javax.transaction.jta-1.1.jar"
JARS="${JARS}:$LIB_DIR/joda-time.joda-time-2.2.jar"
JARS="${JARS}:$LIB_DIR/oauth.signpost.signpost-commonshttp4-1.2.1.2.jar"
JARS="${JARS}:$LIB_DIR/oauth.signpost.signpost-core-1.2.1.2.jar"
JARS="${JARS}:$LIB_DIR/org.apache.commons.commons-lang3-3.1.jar"
JARS="${JARS}:$LIB_DIR/org.apache.httpcomponents.httpclient-4.0.1.jar"
JARS="${JARS}:$LIB_DIR/org.apache.httpcomponents.httpcore-4.0.1.jar"
JARS="${JARS}:$LIB_DIR/org.javassist.javassist-3.18.0-GA.jar"
JARS="${JARS}:$LIB_DIR/org.joda.joda-convert-1.3.1.jar"
JARS="${JARS}:$LIB_DIR/org.mongodb.casbah-commons_2.10-2.6.2.jar"
JARS="${JARS}:$LIB_DIR/org.mongodb.casbah-core_2.10-2.6.2.jar"
JARS="${JARS}:$LIB_DIR/org.mongodb.casbah-gridfs_2.10-2.6.2.jar"
JARS="${JARS}:$LIB_DIR/org.mongodb.casbah-query_2.10-2.6.2.jar"
JARS="${JARS}:$LIB_DIR/org.mongodb.mongo-java-driver-2.11.2.jar"
JARS="${JARS}:$LIB_DIR/org.objenesis.objenesis-1.2.jar"
JARS="${JARS}:$LIB_DIR/org.ow2.asm.asm-4.0.jar"
JARS="${JARS}:$LIB_DIR/org.ow2.asm.asm-commons-4.0.jar"
JARS="${JARS}:$LIB_DIR/org.ow2.asm.asm-tree-4.0.jar"
JARS="${JARS}:$LIB_DIR/org.scala-lang.scala-library-2.10.2.jar"
JARS="${JARS}:$LIB_DIR/org.scala-lang.scala-reflect-2.10.2.jar"
JARS="${JARS}:$LIB_DIR/org.scala-stm.scala-stm_2.10-0.7.jar"
JARS="${JARS}:$LIB_DIR/org.slf4j.jcl-over-slf4j-1.7.5.jar"
JARS="${JARS}:$LIB_DIR/org.slf4j.jul-to-slf4j-1.7.5.jar"
JARS="${JARS}:$LIB_DIR/org.slf4j.slf4j-api-1.7.5.jar"
JARS="${JARS}:$LIB_DIR/xerces.xercesImpl-2.11.0.jar"
JARS="${JARS}:$LIB_DIR/xml-apis.xml-apis-1.4.01.jar"

mkdir -p $ADMIN_DIR
exec java $@ -cp "$JARS" play.core.server.NettyServer $ADMIN_DIR

---
title: Machine Learning Analytics with Zeppelin
---

[Apache Zeppelin](http://zeppelin-project.org/) is an interactive computational
environment built on Apache Spark like the IPython Notebook. With
[PredictionIO](https://prediction.io) and [Spark
SQL](https://spark.apache.org/sql/), you can easily analyze your collected
events when you are developing or tuning your engine.

## Prerequisites

The following instructions assume that you have the command `sbt` accessible in
your shell's search path. Alternatively, you can use the `sbt` command that
comes with PredictionIO at `$PIO_HOME/sbt/sbt`.

## Export Events to Apache Parquet

PredictionIO supports exporting your events to [Apache
Parquet](http://parquet.incubator.apache.org/), a columnar storage format that
allows you to query quickly.

Let's export the data we imported in [Recommendation Engine Template Quick
Start](/templates/recommendation/quickstart/#import-sample-data), and assume the
App ID is 1.

```
$ $PIO_HOME/bin/pio export --appid 1 --output /tmp/movies --format parquet
```

After the command has finished successfully, you should see something similar to
the following.

```
root
 |-- creationTime: string (nullable = true)
 |-- entityId: string (nullable = true)
 |-- entityType: string (nullable = true)
 |-- event: string (nullable = true)
 |-- eventId: string (nullable = true)
 |-- eventTime: string (nullable = true)
 |-- properties: struct (nullable = true)
 |    |-- rating: double (nullable = true)
 |-- targetEntityId: string (nullable = true)
 |-- targetEntityType: string (nullable = true)
```

## Building Zeppelin for Apache Spark 1.2+

Start by cloning Zeppelin and check out a working commit. Zeppelin is being
actively developed and checking out a working commit is very important.

```
$ git clone https://github.com/NFLabs/zeppelin.git
$ git checkout 4a0d6bcfbfc19131efc6442e562d698be6c62561
```

Build Zeppelin with Hadoop 2.4 and Spark 1.2 profiles.

```
$ cd zeppelin
$ mvn -DskipTests -Phadoop-2.4,spark-1.2 -Dhadoop.version=2.4.0 clean package
```

Now you should have working Zeppelin binaries.

## Building Library for Zeppelin

In order to use Zeppelin with the latest Apache Parquet, we will need to build
a library assembly and have Zeppelin load it when it starts up. For your
convenience, you can simply clone the SBT project from our repository.

```
$ git clone https://github.com/PredictionIO/zeppelin-libs.git
$ cd zeppelin-libs
$ sbt assembly
```

After the assembly process finishes, you should have a big JAR file at
`target/scala-2.10/zeppelin-libs-assembly-0.1-SNAPSHOT.jar`. Go back to the
main Zeppelin source tree (the one that you have built), edit
`conf/zeppelin-env.sh` and add the following. Make sure you replace
`path_to_zeppelin-libs` with the real path.

```
ZEPPELIN_TOOL="path_to_zeppelin-libs/target/scala-2.10/zeppelin-libs-assembly-0.1-SNAPSHOT.jar"
export ADD_JARS=$ZEPPELIN_TOOL
export ZEPPELIN_CLASSPATH=$ZEPPELIN_TOOL
```

## Preparing Zeppelin

First, start Zeppelin.

```
bin/zeppelin-daemon.sh start
```

By default, you should be able to access Zeppelin via web browser at
http://localhost:10000. Create a new notebook and put the following in the first
cell.

```scala
sqlc.parquetFile("/tmp/movies").registerTempTable("events")
```

![Preparing Zeppelin](/images/datacollection/zeppelin-01.png)

## Performing Analysis with Zeppelin

If all steps above ran successfully, you should have a ready-to-use analytics
environment by now. Let's try a few examples to see if everything is functional.

In the second cell, put in this piece of code and run it.

```
%sql
SELECT entityType, event, targetEntityType, COUNT(*) AS c FROM events
GROUP BY entityType, event, targetEntityType
```

![Summary of Events](/images/datacollection/zeppelin-02.png)

We can also easily plot a pie chart.

```
%sql
SELECT event, COUNT(*) AS c FROM events GROUP BY event
```

![Summary of Event in Pie Chart](/images/datacollection/zeppelin-03.png)

And see a breakdown of rating values.

```
%sql
SELECT properties.rating AS r, COUNT(*) AS c FROM events
WHERE properties.rating IS NOT NULL GROUP BY properties.rating ORDER BY r
```

![Breakdown of Rating Values](/images/datacollection/zeppelin-04.png)

Happy analyzing!

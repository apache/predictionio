---
title: Frequently Asked Questions
---
## Using PredictionIO
### Q: How to increase Spark driver program and worker executor memory size?
In general, the PredictionIO `bin/pio` scripts wraps around Spark's `spark-submit`
script. You can specify a lot of Spark configurations (i.e. executor memory, cores, master
url, etc.) with it. You can supply these as pass-through arguments at the end of
`bin/pio` command.

For example, the follow command set the Spark master to `spark://localhost:7077`
(the default url of standalone cluster) and set the executor memory to 24G for `pio train`.

```
$ pio train -- --master spark://localhost:7077 --executor-memory 24G
```

### Q: How do I increase the JVM heap size of the Event Server?

```
$ JAVA_OPTS=-Xmx16g bin/pio eventserver --ip 0.0.0.0 --port 7071
````

### Q: How do I check to see if various dependencies, such as ElasticSearch, are running?

You can run `$ pio status` from the terminal and it will return the status of various components that PredictionIO depends on.

### Q: How to resolve "Exception in thread "main" org.apache.spark.SparkException: Job aborted due to stage failure: Serialized task 165:35 was 110539813 bytes, which exceeds max allowed: spark.akka.frameSize (10485760 bytes) - reserved (204800 bytes). Consider increasing spark.akka.frameSize or using broadcast variables for large values."?

A likely reason is the local algorithm model is larger than the default frame size.
You can specify a larger value as a pass-thru argument to spark-submit when you `pio train`.
The following command increase the frameSize to 1024MB.

```
$ pio train -- --conf spark.akka.frameSize=1024
```

## Building PredictionIO
### Q: How to resolve "Error: Could not find or load main class io.prediction.tools.Console" after ./make_distribution.sh?

```
$ bin/pio app
Error: Could not find or load main class io.prediction.tools.Console
```

When PredictionIO bumps a version, it creates another JAR file with the new
version number.

Delete everything but the latest `pio-assembly-<VERSION>.jar` in
`$PIO_HOME/assembly` directory. For example:

```
PredictionIO$ cd assembly/
PredictionIO/assembly$ ls -al
total 197776
drwxr-xr-x  2 yipjustin yipjustin      4096 Nov 12 00:08 .
drwxr-xr-x 17 yipjustin yipjustin      4096 Nov 12 00:09 ..
-rw-r--r--  1 yipjustin yipjustin 101184982 Nov  5 06:05 pio-assembly-0.8.1-SNAPSHOT.jar
-rw-r--r--  1 yipjustin yipjustin 101324859 Nov 12 00:09 pio-assembly-0.8.2.jar

PredictionIO/assembly$ rm pio-assembly-0.8.1-SNAPSHOT.jar
```

### Q: How to resolve ".......[error] (data/compile:compile) java.lang.AssertionError: assertion failed: java.lang.AutoCloseable" when ./make_distribution.sh?

PredictionIO only support Java 7 or later. Please make sure you have the
correct Java version with the command:

```
$ javac -version
```

### Q: What's the difference between P- and L- prefixed classes and functions?

PredictionIO v0.8 is built on the top of Spark, a massively scalable programming framework. A spark algorithm is different from conventional single machine algorithm in a way that spark algorithms use the [RDD](http://spark.apache.org/docs/1.0.1/programming-guide.html#resilient-distributed-datasets-rdds) abstraction as its primary data type.

PredictionIO framework natively support both RDD-based algorithms and traditional single-machine algorithms. For controllers prefixed by "P" (i.e. PJavaDataSource, PJavaAlgorithm), their data include RDD abstraction; For "L" controllers, they are traditional single machine algorithms.



If you have other questions, you can search or post on the [user
group](https://groups.google.com/forum/#!forum/predictionio-user) or [email the
core team](mailto:support@prediction.io) directly.

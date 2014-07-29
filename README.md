Building
========

Run the following to build PredictionIO.
```
$ sbt/sbt package
$ sbt/sbt engines/assemblyPackageDependency
$ sbt/sbt tools/assembly
```

Spark Setup
===========

Download [Spark's pre-built **"For Hadoop 2 (HDP2, CDH5)"** package](http://spark.apache.org/downloads.html). Unzip the file.

Set the `$SPARK_HOME` shell variable to the path of the unzipped Spark directory:
```
$ export SPARK_HOME=<your spark directory>
```

For example,
```
$ export SPARK_HOME=/Users/abc/Downloads/spark-1.0.1-bin-hadoop2
```


Storage Setup
=============

PredictionIO relies on a data store to store its metadata. At the moment,
PredictionIO's storage layer supports both
[Elasticsearch](http://www.elasticsearch.org/) and
[MongoDB](http://www.mongodb.org/). Make sure you have one of these running and
functioning properly on your computer.

1. Copy ``conf/pio-env.sh.template`` to ``conf/pio-env.sh``.

2. If you use Elasticsearch (default), change the following to fit your setup.
   ```
   PIO_STORAGE_SOURCES_ELASTICSEARCH_TYPE=elasticsearch
   PIO_STORAGE_SOURCES_ELASTICSEARCH_HOSTS=localhost
   PIO_STORAGE_SOURCES_ELASTICSEARCH_PORTS=9300
   ```
   If you use MongoDB, add and modify the following to fit your setup.
   ```
   PIO_STORAGE_SOURCES_MONGODB_TYPE=mongodb
   PIO_STORAGE_SOURCES_MONGODB_HOSTS=localhost
   PIO_STORAGE_SOURCES_MONGODB_PORTS=27017
   ```

3. The following points the storage repositories to their respective backend
   data sources. By default, they point to Elasticsearch.
   ```
   PIO_STORAGE_REPOSITORIES_METADATA_SOURCE=ELASTICSEARCH
   PIO_STORAGE_REPOSITORIES_APPDATA_SOURCE=ELASTICSEARCH
   ```
   If you use MongoDB, change them to something like this.
   ```
   PIO_STORAGE_REPOSITORIES_METADATA_SOURCE=MONGODB
   PIO_STORAGE_REPOSITORIES_APPDATA_SOURCE=MONGODB
   ```

4. Save ``conf/pio-env.sh`` and you are done!


Tutorials
=========

The purpose of the tutorials 1-4 is to help you to get familiar with each components of the PredictionIO framework.

- [Tutorial 1 - Develop and Integrate Algorithm with PredictionIO
](engines/src/main/java/recommendations/tutorial1-develop.md)
- [Tutorial 2 - Test Engine Components
](engines/src/main/java/recommendations/tutorial2-runner.md)
- [Tutorial 3 - Evaluation](engines/src/main/java/recommendations/tutorial3-evaluation.md)
- [Tutorial 4 - Multiple Algorithms Engine](engines/src/main/java/recommendations/tutorial4-multialgo.md)

More interesting tutorials:
- [Stock Prediction Engine with customizable algorithms](engines/src/main/scala/stock/README.md)
- [Linear Regression Engine](engines/src/main/scala/regression/local/README.md)
- [Distributed Recommendation Engine with RDD-based Model using MLlib's ALS](engines/src/main/scala/recommendations/README.md)


API Documentation
=================

1.  Run this command.
    ```
    $ sbt/sbt unidoc
    ```

2.  Point your web browser at `target/scala-2.10/unidoc/index.html` for
    ScalaDoc, or `target/javaunidoc/index.html` for Javadoc.

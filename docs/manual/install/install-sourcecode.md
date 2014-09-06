---
layout: docs
title: Installing PredictionIO from source code
---

# Installing PredictionIO from Source Code

Building
========

Run the following to download and build PredictionIO from its source code.

```
$ git clone https://github.com/PredictionIO/PredictionIO.git
$ cd PredictionIO
$ ./make-distribution.sh
```

You should see something like the following when it finishes building
successfully.

```
...
a imagine/bin/run-eval
a imagine/bin/run-server
a imagine/bin/run-train
PredictionIO binary distribution created at imagine.tar.gz
```


Installing Dependencies
=======================

Spark Setup
-----------

Apache Spark is the default processing engine for PredictionIO.  Download [Spark's pre-built **"For Hadoop 2 (HDP2, CDH5)"**
package](http://spark.apache.org/downloads.html). Unzip the file. Set the `$SPARK_HOME` shell variable to the path of the unzipped Spark directory:

```
$ wget http://d3kbcqa49mib13.cloudfront.net/spark-1.0.2-bin-hadoop2.tgz
$ tar zxvf spark-1.0.2-bin-hadoop2.tgz
$ export SPARK_HOME=<your spark directory>
```

For example,

```
$ export SPARK_HOME=/Users/abc/Downloads/spark-1.0.1-bin-hadoop2
```

Storage Setup
-------------

By default, PredictionIO uses ElasticSearch at localhost as the data store to store its metadata. Simply install and run [ElasticSearch](http://www.elasticsearch.org/), which looks like this:

```
$ wget https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.3.2.tar.gz
$ tar zxvf elasticsearch-1.3.2.tar.gz
$ cd elasticsearch-1.3.2
$ bin/elasticsearch
```

You may change the settings or even use another data store such as [MongoDB](http://www.mongodb.org/). For details, please read [Changing the Data Store](config-datastore.html).


Now you have installed everything you need to run PredictionIO!
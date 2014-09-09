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
$ wget http://d3kbcqa49mib13.cloudfront.net/{{ site.spark_download_filename }}.tgz
$ tar zxvf {{ site.spark_download_filename }}.tgz
$ export SPARK_HOME=<your spark directory>
```

For example,

```
$ export SPARK_HOME=/Users/abc/Downloads/{{ site.spark_download_filename }}
```

Storage Setup
-------------

By default, PredictionIO uses ElasticSearch at localhost as the data store to store its metadata. Simply install and run [ElasticSearch](http://www.elasticsearch.org/), which looks like this:

```
$ wget https://download.elasticsearch.org/elasticsearch/elasticsearch/{{ site.elasticsearch_download_filename }}.tar.gz
$ tar zxvf {{ site.elasticsearch_download_filename }}.tar.gz
$ cd {{ site.elasticsearch_download_filename }}
$ bin/elasticsearch
```

You may change the settings or even use another data store such as [MongoDB](http://www.mongodb.org/). For details, please read [Changing the Data Store](config-datastore.html).


Now you have installed everything you need to run PredictionIO!
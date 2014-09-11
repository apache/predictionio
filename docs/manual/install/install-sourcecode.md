---
layout: docs
title: Installing PredictionIO from source code
---

# Installing PredictionIO from Source Code

## Building

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
a imagine/bin/load-pio-env.sh
a imagine/bin/pio
a imagine/bin/pio-class
PredictionIO binary distribution created at PredictionIO-0.8.0.tar.gz
```


## Installing Dependencies

### Spark Setup

Apache Spark is the default processing engine for PredictionIO.  Download
[Spark's pre-built **"For Hadoop 2 (HDP2, CDH5)"**
package](http://spark.apache.org/downloads.html). Unzip the file. Set the
`$SPARK_HOME` shell variable to the path of the unzipped Spark directory:

```
$ wget http://d3kbcqa49mib13.cloudfront.net/{{ site.spark_download_filename }}.tgz
$ tar zxvf {{ site.spark_download_filename }}.tgz
$ export SPARK_HOME=<your spark directory>
```

For example,

```
$ export SPARK_HOME=/Users/abc/Downloads/{{ site.spark_download_filename }}
```

### Storage Setup

#### Elasticsearch Setup

By default, PredictionIO uses Elasticsearch at localhost as the data store to
store its metadata. Simply install and run
[Elasticsearch](http://www.elasticsearch.org/), which looks like this:

```
$ wget https://download.elasticsearch.org/elasticsearch/elasticsearch/{{ site.elasticsearch_download_filename }}.tar.gz
$ tar zxvf {{ site.elasticsearch_download_filename }}.tar.gz
$ cd {{ site.elasticsearch_download_filename }}
$ bin/elasticsearch
```

You may change the settings or even use another data store such as
[MongoDB](http://www.mongodb.org/). For details, please read [Changing the Data
Store](config-datastore.html).

#### <a name="hbase"></a>HBase Setup

By default, PredictionIO's Data API uses HBase at localhost as the data store
for event data.

```
$ wget http://www.apache.org/dyn/closer.cgi/hbase/{{ site.hbase_basename }}/{{ site.hbase_basename }}-{{ site.hbase_variant }}.tar.gz
$ tar zxvf {{ site.hbase_basename }}-{{ site.hbase_variant }}.tar.gz
$ cd {{ site.hbase_basename }}-{{ site.hbase_variant }}
```

You will need to at least add a minimal configuration to HBase to start it in
standalone mode. Details can be found
[here](http://hbase.apache.org/book/quickstart.html). Here, we are showing a
sample minimal configuration. Edit `conf/hbase-site.xml` and put the following
in. You may replace `/Users/abc` with your own home directory.

```
<configuration>
  <property>
    <name>hbase.rootdir</name>
    <value>file:///Users/abc/hbase</value>
  </property>
  <property>
    <name>hbase.zookeeper.property.dataDir</name>
    <value>/Users/abc/zookeeper</value>
  </property>
</configuration>
```

Now you may start HBase.

```
$ bin/start-hbase.sh
```

Now you have installed everything you need to run PredictionIO!

Next: [Loading Data](/dataapi.html)

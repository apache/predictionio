---
layout: docs
title: Systems 
---

# System Overview and Dependencies
 
 
During  the [installation](../start/), you have installled the following programs:

* Apache Hadoop 2.4.0 (required only if YARN and HDFS are needed)
* Apache HBase 0.98.6
* Apache Spark 1.1.0 for Hadoop 2.4
* Elasticsearch 1.3.0

This section explains how they are used in PredictionIO. 

|

![PredictionIO Systems]({{site.baseurl}}/images/0.8-engine-data-pipeline.png)

|

**HBase**: EventServer uses HBase as the data store. It stores the imported Events. If you are not using the PredictionIO EventServer, you do not need to install HBase. 

**Apache Spark**: Spark is a large-scale data processing engine that powers the algorithm, training, and serving processing. 

**HDFS**: The output of training has two parts - a model and meta-data. The model is then stored in HDFS or a local file system. 

**ElasticSearch**: It stores the model meta-data. 
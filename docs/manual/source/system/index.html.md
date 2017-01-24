---
title: System Architecture and Dependencies
---

<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

During the [installation](/install), you have installed the latest stable versions of the following software:

* Apache Hadoop up to 2.7.2 (required only if YARN and HDFS are needed)
* Apache HBase up to 1.2.4
* Apache Spark up to 1.6.3 for Hadoop 2.6 (not Spark 2.x version)
* Elasticsearch up to 1.7.5 (not the Elasticsearch 2.x version)

This section explains general rules-of-thumb for how they are used in PredictionIO. The actual implementation of the Template will define how much of this applies. PredictionIO is flexible about much of this configuration but its Templates generally fit the Lambda model for integrating real-time serving with background periodic model updates.

![PredictionIO Systems](/images/pio-architecture.svg)

**HBase**: Event Server uses Apache HBase (or JDBC DB for small data) as the data store. It stores imported
events. If you are not using the PredictionIO Event Server, you do not need to
install HBase.

**Apache Spark**: Spark is a large-scale data processing engine that powers the data preparation and input to the algorithm, training, and sometimes the serving processing. PredictionIO allows for different engines to be used in training but many algorithms come from Spark's MLlib.

**HDFS**: is a distributed filesystem from Hadoop. It allows storage to be shared among clustered machines. It is used to stage data for batch import into PIO, for export of Event Server datasets, and for storage of some models (see your template for details).


The output of training has two parts: a model and its meta-data. The
model is then stored in HDFS, a local file system, or Elasticsearch. See the details of your algorithm.

**Elasticsearch**: stores metadata such as model versions, engine versions, access key and app id mappings, evaluation results, etc. For some templates it may store the model.

---
title: Installation PredictionIO
---

To get started, install PredictionIO Server and choose a SDK for your
application.

## Prerequisites

It is **very important** to meet the minimum version of the following
technologies that power PredictionIO.

* Apache Hadoop 2.4.0 (required only if YARN and HDFS are needed)
* Apache HBase 0.98.6
* Apache Spark 1.2.0 for Hadoop 2.4
* Elasticsearch 1.3.0
* Java SE Development Kit 7

If you are running on a single machine, we recommend a minimum of 2GB memory.

INFO: If you are using Linux, Apache Spark local mode, which is the default
operation mode without further configuration, may not work. In that case,
configure your Apache Spark to run in [standalone cluster
mode](http://spark.apache.org/docs/latest/spark-standalone.html).

## Installing PredictionIO Server

PredictionIO runs on a Java virtual machine, so it runs on most platforms.
Choose your platform below:

* [Installing PredictionIO on Linux / Mac OS X](install-linux.html)
* [Installing PredictionIO from Source Code](install-sourcecode.html)
* [Launching PredictionIO on Amazon Web Services](launch-aws.html)

You may also use one of the community-contributed packages to install
PredictionIO:

* [Installing PredictionIO with
  Docker](/community/projects.html#docker-installation-for-predictionio)
* [Installing PredictionIO with Vagrant
  (VirtualBox)](/community/projects.html#vagrant-installation-for-predictionio)

[//]: # (* *(coming soon)* Installing PredictionIO with Homebrew)



WARNING: **0.8.2 contains schema changes from the previous versions, if you have
installed the previous versions, you may need to clear both HBase and
Elasticsearch. See more [here](/resources/upgrade/).**


[//]: # (## Production Deployment)

[//]: # (For production environment setup, please refer to [Production)
[//]: # (Deployment](/production/deploy.html) guide.)

#### [Next: Recommendation Engine Quick Start](/templates/recommendation/quickstart/)

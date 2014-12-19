---
title: Changes and Upgrades
---

This page highlights major changes in each version and upgrad tools. 


##Upgrade from 0.8.2 to 0.8.3

0.8.3 disallows entity types **pio_user** and **pio_item**. These types are used by default for most SDKs. They are deprecated in 0.8.3, and SDKs helper functions have been updated to use **user** and **item** instead.

This script performs the migration by copying one appId to another. You can either point the engine to the new appId, or can migrate the data back to the old one using hbase import / export tool.

Suppose we are migrating <old_app_id>.

```
$ set -a
$ source conf/pio-env.sh
$ set +a
$ bin/pio app new NewApp
... you will see <new_app_id> 
$ sbt/sbt "data/run-main io.prediction.data.storage.hbase.upgrade.Upgrade_0_8_3 <old_app_id> <new_app_id>"
... Done.
```

*new_app_id* must be empty when you upgrade. You can check the status of an app using:

```
$ sbt/sbt "data/run-main io.prediction.data.storage.hbase.upgrade.CheckDistribution <new_app_id>"
```
If it shows that it is non-empty, you can clean it with

```
$ bin/pio app data-delete <new_app_name>
```

## Schema Changes in 0.8.2

0.8.2 contains HBase and Elasticsearch schema changes from previous versions. If you are upgrading from a pre-0.8.2 version, you need to first clear HBase and ElasticSearch. These will clear out all data
in Elasticsearch and HBase. Please be extra cautious.

DANGER: **ALL EXISTING DATA WILL BE LOST!**


### Clearing Elasticsearch

With Elasticsearch running, do

```
$ curl -X DELETE http://localhost:9200/_all
```

For details see http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/indices-delete-index.html.

### Clearing HBase

```
$ $HBASE_HOME/bin/hbase shell
...
> disable_all 'predictionio.*'
...
> drop_all 'predictionio.*'
...
```

For details see http://wiki.apache.org/hadoop/Hbase/Shell.

## Experimental upgrade tool (Upgrade HBase schema from 0.8.0/0.8.1 to 0.8.2) 

Create an app to store the data

```
$ bin/pio app new <my app>
```

Replace by the returned app ID: ( is the original app ID used in 0.8.0/0.8.2.)

```
$ set -a
$ source conf/pio-env.sh
$ set +a
$ sbt/sbt "data/run-main io.prediction.data.storage.hbase.upgrade.Upgrade <from app ID>" "<to app ID>"
```

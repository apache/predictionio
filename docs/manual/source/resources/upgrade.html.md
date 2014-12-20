---
title: Upgrade Instructions
---

This page highlights major changes in each version and upgrade tools. 

##Upgrade to 0.8.4

**engine.json** has slightly changed its format in 0.8.4 in order to make engine more flexible. If you are upgrading to 0.8.4, engine.json needs to have the ```params``` field for *datasource*, *preparator*, and *serving*. Here is the sample engine.json from templates/scala-parallel-recommendation-custom-preparator that demonstrate the change for *datasource* (line 7).


```
In 0.8.3 
{
  "id": "default",
  "description": "Default settings",
  "engineFactory": "org.template.recommendation.RecommendationEngine",
  "datasource": {
    "appId": 1
  },
  "algorithms": [
    {
      "name": "als",
      "params": {
        "rank": 10,
        "numIterations": 20,
        "lambda": 0.01
      }
    }
  ]
}
```



```
In 0.8.4
{
  "id": "default",
  "description": "Default settings",
  "engineFactory": "org.template.recommendation.RecommendationEngine",
  "datasource": {
    "params" : {
      "appId": 1
    }
  },
  "algorithms": [
    {
      "name": "als",
      "params": {
        "rank": 10,
        "numIterations": 20,
        "lambda": 0.01
      }
    }
  ]
```



##Upgrade from 0.8.2 to 0.8.3

0.8.3 disallows entity types **pio_user** and **pio_item**. These types are used by default for most SDKs. They are deprecated in 0.8.3, and SDKs helper functions have been updated to use **user** and **item** instead.

If you are upgrading to 0.8.3, you can follow these steps to migrate your data. 

##### 1. Create a new app 

```
$ pio app new <my app name>
```
Please take note of the <new app id> generated for the new app.

##### 2. Run the upgrade command 

```
$ pio upgrade 0.8.2 0.8.3 <old app id> <new app id>
```

It will run a script that creates a new app with the new app id and migreate the data to the new app. 

##### 3. Update **engine.json** to use the new app id. **Engine.json** is located under your engine project directory. 

```
  "datasource": {
    "appId": <new app id>
  },
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

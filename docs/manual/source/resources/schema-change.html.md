---
title: Upgrading to 0.8.2
---

# Schema Changes in 0.8.2

0.8.2 contains HBase and Elasticsearch schema changes from previous versions. In
order to use 0.8.2, you need to first clear them. These will clear out all data
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

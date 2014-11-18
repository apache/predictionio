---
layout: docs
title: Recommendation Quick Start
---

#Upgrade to 0.8.2 

0.8.2 contains HBase and ElasticSearch schema changes from the previous versions. In order to use 0.8.2, you need to first clear them. 
These will clear out all the data in ElasticSearch and HBase, please extra cautious. 

----

# __PRECAUTION: ALL EXISTING DATA WILL BE LOST__

----

**To clear Elasticsearch**

http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/indices-delete-index.html
i.e. with elasticsearch running,

```
$ curl -X DELETE http://localhost:9200/_all
```

**To clear HBase**

http://wiki.apache.org/hadoop/Hbase/Shell

```
$ $HBASE_HOME/bin/hbase shell
...
> disable_all 'predictionio.*'
...
> drop_all 'predictionio.*'
...
```

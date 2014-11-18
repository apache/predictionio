---
layout: docs
title: Recommendation Quick Start
---

#Upgrade to 0.8.2 

0.8.2 contains HBase and ElasticSearch schema changes from the previous versions. In order to use 0.8.2, you need to first clear them. 
These will clear out all the data in ElasticSearch and HBase, please extra cautious. 


**To clear Elasticsearch**

http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/indices-delete-index.html
i.e. "curl -X DELETE http://localhost:9200/_all" with elasticsearch running

**To clear HBase**

http://wiki.apache.org/hadoop/Hbase/Shell
i.e. Run HBase shell, then "disable_all '.*', followed by "drop_all '.*'" (no double quotes)

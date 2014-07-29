Running MLlib ALS Recommendation Engine
=======================================

```
$ bin/register-engine engines/src/main/scala/recommendations/manifest.json

$ bin/run-train \
--engineId org.apache.spark.mllib.recommendation.engine \
--engineVersion 0.8.0-SNAPSHOT \
--jsonBasePath engines/src/main/scala/recommendations/params

$ bin/run-server --runId RUN_ID_HERE

$ curl -H "Content-Type: application/json" -d '[1,4]' http://localhost:8000
```

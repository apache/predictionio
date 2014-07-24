Running MLlib ALS Recommendation Engine
=======================================

```
bin/register-engine engines/src/main/scala/recommendations/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
bin/run-workflow --sparkHome $SPARK_HOME --engineId org.apache.spark.mllib.recommendation.engine --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/scala/recommendations/params
bin/run-server io.prediction.tools.RunServer --runId RUN_ID_HERE
curl -H "Content-Type: application/json" -d '[1,4]' http://localhost:8000
```

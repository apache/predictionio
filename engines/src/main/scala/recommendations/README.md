Running MLlib ALS Recommendation Engine
=======================================

```
bin/pio-class io.prediction.tools.RegisterEngine engines/src/main/scala/recommendations/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
bin/pio-class io.prediction.tools.RunWorkflow --sparkHome $SPARK_HOME --engineId org.apache.spark.mllib.recommendation.engine --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/scala/recommendations/params
bin/pio-class io.prediction.tools.RunServer --runId RUN_ID_HERE
curl -H "Content-Type: application/json" -d '[1,4]' http://localhost:8000
```

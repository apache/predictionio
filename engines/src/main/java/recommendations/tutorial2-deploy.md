# Tutorial 2 - Deploy Engine

## Step 1. Create Engine Factory class

PredictionIO framework requires the Engine has a EngineFactory class which returns an engine definition for deployment.

The EngineFactory implements io.prediction.controller.IEngineFactory interface. You can simply use the JavaSimpleEngineBuilder as shown in EngineFactory.java:

```java
public class EngineFactory implements IEngineFactory {
  public JavaSimpleEngine<TrainingData, EmptyParams, Query, Float, Object> apply() {
    return new JavaSimpleEngineBuilder<
      TrainingData, EmptyParams, Query, Float, Object> ()
      .dataSourceClass(DataSource.class)
      .addAlgorithmClass("MyRecommendationAlgo", Algorithm.class)
      .servingClass()
      .build();
  }
}
```

To deploy engine, we need a serving layer. For SimpleEngine with single algorithm, we can use default serving class by simply call the method servingClass().

Note that the algorithm Name "MyRecommendationAlgo" is specified which will be used later when specify the parameters for this algorithm.

## Step 2. Compile and register engine:

Execute the following commands:

```
sbt/sbt package
sbt/sbt engines/assemblyPackageDependency
```

Next we need to register the engine into PredictionIO.

A engine Manifest manifest.json is needed to describe the Engine (in jsons/manifest.json):

```json
{
  "id": "io.prediction.engines.java.recommendations",
  "version": "0.8.0-SNAPSHOT",
  "name": "Recommendations Engine",
  "engineFactory": "io.prediction.engines.java.recommendations.EngineFactory"
}
```

The engineFactory is the class name of the EngineFactory created in step 1.

Execute the following command to register engine:

```
bin/register-engine engines/src/main/java/recommendations/jsons/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
```

The register-engine command takes the engine manifest file and the required jar files as arguments. You only need to register engine once unless you have changed the jar path or updated the engine manifest.

## Step 3. Define parameters for the engine

If your controller components (Eg. DataSource, Algorithm) take parameters. You need to define json files for them.

In this recommendation engine tutorial, the DataSource has a parameter which is the file path of the ratings file. The json is defined as following (Jsons/DataSourceParams.json):

```json
{ "filePath" : "engines/src/main/java/recommendations/testdata/ratings.csv" }
```

Note that the key name of the parameter should match the field name of the DataSourceParams class.

The following is defined for the algorithms (jsons/algoParams.json):

```json
[
  { "name": "MyRecommendationAlgo",
    "params" :  { "threshold" : 0.1 }
  }
]
```

The "name" is the name of the algorithm specified in the EngineFactory in step 1. The "params" is the json value of the algorithm parameters.

If you algorithm takes no parameter, you still need to put empty JSON {}. For example,

```json
[
  { "name": "MyRecommendationAlgo",
    "params" :  { }
  }
]
```

## Step 4. Run work flow and Server

Next is to use RunWorkFlow to build and save the algorithm Model for serving real time requests.

Execute the following commands:

```
bin/run-workflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.java.recommendations --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/java/recommendations/jsons
```

When it finishes, you should see the following at the end of terminal output:

```
14/07/24 16:55:13 INFO SparkContext: Job finished: collect at DebugWorkflow.scala:553, took 0.014837 s
14/07/24 16:55:13 INFO APIDebugWorkflow$: Metrics is null. Stop here
14/07/24 16:55:13 INFO APIDebugWorkflow$: Run information saved with ID: 201407240001
```

Note that there is ID returned at the end. In this example, it's 201407240001. Exceute run-server command with this ID:

```
bin/run-server --runId 201407240001
```

This will create a server that by default binds to http://localhost:8000. You can visit that page in your web browser to check its status.

## Step 5. Real-time prediction query

Now you can retrive prediction request by sending a HTTP request to the server with the Query as JSON payload. Note that the key name (uid and iid in this example) must match the field name of the defined Query class.

For example, retrieve predicted preference for item ID 3 by user ID 1. Run the following in terminal:

```
curl -H "Content-Type: application/json" -d '{ "uid" : 1, "iid" : 3}' http://localhost:8000
```

You should see the predicted preference value returned:

```
2.667687177658081
```

Next: [Tutorial 3 - Evaluation](tutorial3-evaluation.md)

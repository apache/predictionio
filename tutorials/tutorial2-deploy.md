# Tutorial 2 - Deploy Engine

This tutorial continues from Tutorial1.

## Step 1. Create Engine Factory class

EngineFactory class is used to return an Engine Instance for deployment or evaluation.

Create a EngineFactory.java:

```java
// code
```

Note that the algorithm Name "MyRecommendationAlgo" is specified.


## Step 3. Compile

Execute:

```
sbt/sbt package
sbt/sbt engines/assemblyPackageDependency
sbt/sbt tools/assembly
```

## Step 2. Register Engine

Engine Manifest:
```json
{ Engine manifest }
```

Execute command to register engine
```
bin/pio-class io.prediction.tools.RegisterEngine engines/src/main/java/recommendations/jsons/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
```

You only need to register engine once unless you have changed the jar path or engine class path.

## Step 3. Create parameters JSON files

If your controller components take parameters. You need to define json file for it.

In this tutorial, DataSource has a parameter which is the file path of the ratings file.

Create DataSourceParams.json

```json
{ "filePath" :  "" }
```

You also need to define algorithmsParams.json to specify the algorithm you want to run.

Create algorithm
```json
[
  { "name": "MyRecommendationAlgo",
    "params" :  {}
  }
]

```

Since the algorithm doesn't take parameter, the params is empty {}.


## Step 4. Run work flow

Execute
```
bin/pio-class io.prediction.tools.RunWorkflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.java.recommendations --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/java/recommendations/jsons
```

Note that if you want to change the value of parameters, you can simply change the value in JSON file and re-run without re-compilation.


## Step 5. Deploy Engine

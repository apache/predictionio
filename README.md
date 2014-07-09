Running Evaluation
==================

The following is a regression example:

- Make sure MongoDB is running at localhost:27017.
- You only need to run RegisterEngine once unless you updated your engine's manifest.

```
sbt/sbt package
sbt/sbt engines/assemblyPackageDependency
sbt/sbt "core/runMain io.prediction.tools.RegisterEngine ../engines/src/main/scala/regression/examples/manifest.json"
sbt/sbt "core/runMain io.prediction.tools.RunEvaluationWorkflow --sparkHome $SPARK_HOME io.prediction.engines.regression 0.8.0-SNAPSHOT --jsonDir ../engines/src/main/scala/regression/examples"
```

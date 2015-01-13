---
title: Engine Development
---

PredictionIO provides the following debug features to help you develop and debug engines.

## Using pio train with --stop-after-read and --stop-after-prepare options

By default `pio train` runs through the whole training processes including DataSource, Preparator and Algorithm.

To speed up the development and debug cycle, sometimes you may want to stop the process right after DataSource to verify the the TrainingData output only (for example, you modify DataSource and want to debug it). You can run `pio train` with `--stop-after-read` option:

```
pio train --stop-after-read
```

This would stop the training process after TrainingData is generated.

For example, if you are running with Recommendation Template, you should see the the training process stops after the TrainingData is printed.

```
2015-01-12 17:01:26,531 INFO  workflow.CoreWorkflow$ - TrainingData:
2015-01-12 17:01:26,532 INFO  workflow.CoreWorkflow$ - ratings: [1501] (List(Rating(3,0,4.0), Rating(3,1,4.0))...)
...
2015-01-12 17:01:27,717 INFO  workflow.CoreWorkflow$ - Training has stopped after reading from data source and is incomplete.
```

Similarly, you can stop the training after the Preparator stage by using --stop-after-prepare option and it would stop after PreparedData is generated:

```
pio train --stop-after-prepare
```

## Using SanityCheck

If you overrides `toString()` method in the data classes (TrainingData, PreparedData, and Model), PredictionIO will print the data to the console output for debugging purpose.

In addition, you can extend a trait `SanityCheck` and implement the method `sanityCheck()` with your error checking code. The `sanityCheck()` is called when the data is generated.

For example, one frequent error with the Recommendation Template is that the TrainingData is empty because the DataSource is not reading data correctly. You can add the check of empty data inside the `sanityCheck()` function. You can easily add other checking logic into the `sanityCheck()` function based on your own needs.

Modify DataSource.scala in the Recommendation Template:

```scala
import io.prediction.controller.SanityCheck // ADDED

class TrainingData(
  val ratings: RDD[Rating]
) extends Serializable with SanityCheck { // MODIFIED
  override def toString = {
    s"ratings: [${ratings.count()}] (${ratings.take(2).toList}...)"
  }

  // ADDED
  override def sanityCheck(): Unit = {
    // add your other checking here
    require(!ratings.take(1).isEmpty, s"ratings cannot be empty!")
  }
}
```

You may also use together with --stop-after-read flag to debug the DataSource:

```
pio build
pio train --stop-after-read
```

If your data is empty, you should see the following error thrown by the `sanityCheck()` function:

```
2015-01-12 17:46:14,026 INFO  workflow.CoreWorkflow$ - Performing data sanity check on training data.
2015-01-12 17:46:14,027 INFO  workflow.CoreWorkflow$ - org.template.recommendation.TrainingData supports data sanity check. Performing check.
Exception in thread "main" java.lang.IllegalArgumentException: requirement failed: ratings cannot be empty!
	at scala.Predef$.require(Predef.scala:233)
	at org.template.recommendation.TrainingData.sanityCheck(DataSource.scala:73)
	at io.prediction.workflow.CoreWorkflow$$anonfun$runTypelessContext$7.apply(Workflow.scala:474)
	at io.prediction.workflow.CoreWorkflow$$anonfun$runTypelessContext$7.apply(Workflow.scala:465)
	at scala.collection.immutable.Map$Map1.foreach(Map.scala:109)
  ...
```

You can specify the `--skip-sanity-check` option to turn off sanityCheck:

```
pio train --stop-after-read --skip-sanity-check
```

You should see the checking is skipped such as the following output:

```
2015-01-12 17:54:01,049 INFO  workflow.CoreWorkflow$ - Data sanity checking is off.
2015-01-12 17:54:01,049 INFO  workflow.CoreWorkflow$ - Data Source
...
2015-01-12 17:54:05,320 INFO  workflow.CoreWorkflow$ - Training has stopped after reading from data source and is incomplete.
```

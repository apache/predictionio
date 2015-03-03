---
title: Engine Development - Troubleshoot
---

PredictionIO provides the following features to help you debug engines during development cycle.

## Stop Training between Stages

By default `pio train` runs through the whole training process including
[DataSource, Preparator and Algorithm](/templates/recommendation/dase/). To
speed up the development and debug cycle, you can stop the process after each
stage to verify it has completed correctly.

If you have modified DataSource and want to confirm the TrainingData is
generated as expected, you can run `pio train` with `--stop-after-read` option:

```
pio train --stop-after-read
```

This would stop the training process after the TrainingData is generated.

For example, if you are running [Recommendation
Template](/templates/recommendation/quickstart/), you should see the the
training process stops after the TrainingData is printed.

```
[INFO] [CoreWorkflow$] TrainingData:
[INFO] [CoreWorkflow$] ratings: [1501] (List(Rating(3,0,4.0), Rating(3,1,4.0))...)
...
[INFO] [CoreWorkflow$] Training interrupted by io.prediction.workflow.StopAfterReadInterruption.
```

Similarly, you can stop the training after the Preparator phase by using
--stop-after-prepare option and it would stop after PreparedData is generated:

```
pio train --stop-after-prepare
```

##  Sanity Check

If you overrides `toString()` method in the data classes (TrainingData,
PreparedData, and Model), PredictionIO will print the data to the console output
for debugging purpose.

In addition, you can extend a trait `SanityCheck` and implement the method
`sanityCheck()` with your error checking code. The `sanityCheck()` is called
when the data is generated.

For example, one frequent error with the Recommendation Template is that the
TrainingData is empty because the DataSource is not reading data correctly. You
can add the check of empty data inside the `sanityCheck()` function. You can
easily add other checking logic into the `sanityCheck()` function based on your
own needs.

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

If your data is empty, you should see the following error thrown by the
`sanityCheck()` function:

```
[INFO] [CoreWorkflow$] Performing data sanity check on training data.
[INFO] [CoreWorkflow$] org.template.recommendation.TrainingData supports data sanity check. Performing check.
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
[INFO] [CoreWorkflow$] Data sanity checking is off.
[INFO] [CoreWorkflow$] Data Source
...
[INFO] [CoreWorkflow$] Training interrupted by io.prediction.workflow.StopAfterReadInterruption.
```

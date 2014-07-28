# Tutorial 3 - Evaluation

In this tutorial, we will demonstrate how to implement an evaluation *Metrics* component to run *Offline Evaluation* for the *Engine*. We will continue to use the Item Recommendation Engine developed in Tutorial1 as example and implement a *Metrics* which computes the Root Mean Square Error.

## Step 1 - Training and Test Set Split

To run *Offline Evaluation*, we need *Training and Test Set* data. We will modify the `DataSource.java` to do random split of the rating data to generate the *Test Set*. For demonstration purpose, we have created a separated file under directory `tutorial3/`.

Recall that the `io.prediction.controller.java.LJavaDataSource` takes the following type parameters:

```java
abstract class LJavaDataSource[DSP <: Params, DP, TD, Q, A]
```
- `DSP`: *DataSource Parameters* class.
- `DP`: *Data Parameters* class. It is used to describe the generated *Training Data* and the Test Data *Query and Actual*, which is used by *Metrics* during evaluation.
- `TD`: *Training Data* class.
- `Q`: Input *Query* class.
- `A`: *Actual* result class.

The *Actual* result is used by *Metrics* to compare with *Prediciton* outputs to compute the score. In this tutorial, the *Actual* result is also the rating value, which is `Float` type.
Since we don't have any *Data Parameters* defined, we can simply use `Object`.

You can find the implementation in `tutorial3/DataSource.java`:

```java
public class DataSource extends LJavaDataSource<
  DataSourceParams, Object, TrainingData, Query, Float> {
  //...
  @Override
  public Iterable<Tuple3<Object, TrainingData, Iterable<Tuple2<Query, Float>>>> read() {
    // ...
  }
}
```

As explained in earlier tutorials, the `read()` method should read data from the source (Eg. database or text file, etc) and return the *Training Data* (`TD`) and *Test Data* (`Iterable[(Q, A)]`) with a *Data Parameters* (`DP`) associated with this *Training and Test Data Set*.

Note that the `read()` method's return type is `Iterable` because it could return one or more of *Training and Test Data Set*. For example, we may want to evaluate the engine with multiple iterations of random training and test data split. In this case, each set corresponds to each split.

Note that the *Test Data* is actually an `Iterable` of input *Query* and *Actual* result. During evaluation, PredictionIO sends the *Query* to the engine and retrieve *Prediction* output, which will be evaluated against the *Actual* result by the *Metrics*.


## Step 2 - Metrics

We will implement a Root Mean Square Error (RMSE) metric. You can find the implementation in `Metrics.java`. The *Metrics* extends `io.prediction.controller.java.JavaMetrics`, which requires the following type parameters:

```java
abstract class JavaMetrics[MP <: Params, DP, Q, P, A, MU, MR, MMR <: AnyRef]
```
- `MP`: *Metrics Parameters* class.
- `DP`: *Data Parameters* class.
- `Q`: Input *Query* class.
- `P`: *Prediction* output class.
- `A`: *Actual* result class.
- `MU`: *Metric Unit* class.
- `MR`: *Metric Result* class.
- `MMR`: *Multiple Metric Result* class.

and overrides the following methods:

```java
abstract def computeUnit(query: Q, predicted: P, actual: A): MU

abstract def computeSet(dataParams: DP, metricUnits: Iterable[MU]): MR

abstract def computeMultipleSets(input: Iterable[(DP, MR)]): MMR
```




The method `computeUnit()` computes the *Metric Unit (MU)* for each *Prediction* and *Actual* results of the input *Query*.

For this RMSE Metrics, `computeUnit()` returns the square error of each predicted rating and actual rating.

```java
@Override
public Double computeUnit(Query query, Float predicted, Float actual) {
  logger.info("Q: " + query.toString() + " P: " + predicted + " A: " + actual);
  // return squared error
  double error = predicted - actual;
  return (error * error);
}
```

The method `computeSet()` takes all of the *Metric Unit (MU)* of the same set to compute the *Metric Result (MR)* for this set.

For this RMSE metrics, `computeSet()` calculates the square root mean of all square error of the same set and then return it.

```java
@Override
public Double computeSet(Object dataParams, Iterable<Double> metricUnits) {
  double sum = 0.0;
  int count = 0;
  for (double squareError : metricUnits) {
    sum += squareError;
    count += 1;
  }
  return Math.sqrt(sum / count);
}
```

The method `computeMultipleSets()` takes the *Metric Results* of all sets to do a final computation and returns *Multiple Metric Result*. PredictionIO will display this final *Miltiple Metric Result* in the terminal.

In this tutorial, it simply combines all *Metric Results* as String and return it.

```java
@Override
public String computeMultipleSets(
  Iterable<Tuple2<Object, Double>> input) {
  return Arrays.toString(IteratorUtils.toArray(input.iterator()));
}
```

## Step 3 - Run Evaluation

To run evaluation with metric, simply add the `Metrics` class to the `runEngin()` in `JavaAPIDebugWorkflow.runEngine()` (as shown in `Runner3.java`).

Because our `Metrics` class doesn't take parameter, `EmptyParams` class is used.

```java
JavaAPIDebugWorkflow.runEngine(
  "MyEngine",
  new HashMap<String, String>(),
  3, // verbose
  (new EvaluationEngineFactory()).apply(),
  engineParams,
  Metrics.class, // Add the Metrics class
  new EmptyParams() // Metrics Parameters
);

```

Execute the following command:

```
bin/pio-run io.prediction.engines.java.recommendations.tutorial3.Runner3 data/test/ratings.csv
```

You should see the following output, the Metric Result (RMSE score) of each iteration is printed at the end.

```
14/07/28 00:37:26 INFO SparkContext: Job finished: collect at DebugWorkflow.scala:624, took 0.087665 s
14/07/28 00:37:26 INFO APIDebugWorkflow$: DataSourceParams: io.prediction.engines.java.recommendations.tutorial1.DataSourceParams@3b9ec8ff
14/07/28 00:37:26 INFO APIDebugWorkflow$: PreparatorParams: Empty
14/07/28 00:37:26 INFO APIDebugWorkflow$: Algo: 0 Name: MyRecommendationAlgo Params: io.prediction.engines.java.recommendations.tutorial1.AlgoParams@193fc2a2
14/07/28 00:37:26 INFO APIDebugWorkflow$: ServingParams: Empty
14/07/28 00:37:26 INFO APIDebugWorkflow$: MetricsParams: Empty
14/07/28 00:37:26 INFO APIDebugWorkflow$: [(null,1.0), (null,3.8078865529319543), (null,1.5811388300841898)]
14/07/28 00:37:26 INFO APIDebugWorkflow$: APIDebugWorkflow.run completed.
```

## Step 4 - Running with MovieLens 100K data set:

Run the following to fetch the data set. The ml-100k will be downloaded into the `data/` directory.

```
engines/src/main/java/recommendations/fetch.sh
```

Re-run Runner3 with the ml-100k data set:

```
bin/pio-run io.prediction.engines.java.recommendations.tutorial3.Runner3 data/ml-100k/u.data
```

You should see the following when it completes. The RMSE metric scores of each iteration is printed at the end:

```
14/07/28 00:42:35 INFO SparkContext: Job finished: collect at DebugWorkflow.scala:624, took 14.240937 s
14/07/28 00:42:35 INFO TaskSchedulerImpl: Removed TaskSet 18.0, whose tasks have all completed, from pool
14/07/28 00:42:35 INFO APIDebugWorkflow$: DataSourceParams: io.prediction.engines.java.recommendations.tutorial1.DataSourceParams@22c8ab44
14/07/28 00:42:35 INFO APIDebugWorkflow$: PreparatorParams: Empty
14/07/28 00:42:35 INFO APIDebugWorkflow$: Algo: 0 Name: MyRecommendationAlgo Params: io.prediction.engines.java.recommendations.tutorial1.AlgoParams@1e85c076
14/07/28 00:42:35 INFO APIDebugWorkflow$: ServingParams: Empty
14/07/28 00:42:35 INFO APIDebugWorkflow$: MetricsParams: Empty
14/07/28 00:42:35 INFO APIDebugWorkflow$: [(null,1.060572691974822), (null,1.0521347222088266), (null,1.0583554835015063)]
14/07/28 00:42:35 INFO APIDebugWorkflow$: APIDebugWorkflow.run completed.
```

Up to this point, you should be familiar with basic components of PredictionIO (*DataSource, Algorithm and Metrics*) and know how to develop your algorithms and prediction engines, deploy them and serve real time prediction queries.

In next tutuorial, we will demonstrate how to use *Preparator* to do pre-processing of *Training Data* for the *Algorithm*, incorporate multiple *Algorithms* into the *Engine* and create a custom *Serving* component.

Next: [Tutorial 4 - Multiple Algorithms Engine](tutorial4-multialgo.md)

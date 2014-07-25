# Tutorial 3 - Evaluation

## Step 1 - Training and Test set split

To run offline evaluation of the engine, we need to have training and test set data. We need to modify the DataSource.java to do the data split and generate the test set. For demonstration purpose, we have created separated file EvaluationDataSource.java to handle this.


Recall that the LJavaDataSource takes the following type parameters:

```java
abstract class LJavaDataSource[DSP <: Params, DP, TD, Q, A]
```

* DSP: Data Source Parameters.
* DP: Data Params which describes the generated Training Data and the test data Query and Actual.
* TD: Training Data.
* Q: Input Query to the engine
* A: Actual result we want to evaluate the Prediction output against.

In this tutorial, the Actual result is also the rating value, which is Float type. We don't have any Data Params defined, we can use EmptyParams:

```java
public class EvaluationDataSource extends LJavaDataSource<
  DataSourceParams, EmptyParams, TrainingData, Query, Float> {
  //...
}
```

The method read() of LJavaDataSource returns an Iterable of evaluation data set which consists of Data Params, Training Data, and Test Data.

The DataSource could generate multiple evaluation data sets and each set will be evaluated. For example, we may want to evaluate the engine with multiple iterations of random training and test split. In this case, each set corresponds to each split.

Note that the Test Data is actually an Iterable of Query and Actual. During evaluation, PredictionIO sends the Query to the engine and retrieve Prediction output, which will be evaluated against the Actual result by the Metrics.

```java
abstract def read(): Iterable[(DP, TD, Iterable[(Q, A)])]
```

In this tutorial, a random data split of rating data is implemented as shown in EvaluationDataSource.java:

```java
@Override
public Iterable<Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Float>>>> read() {
  // code committed ...

  Random rand = new Random(0); // seed

  List<Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Float>>>> data = new
    ArrayList<Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Float>>>>();

  for (int i = 0; i < iterations; i++) {
    Collections.shuffle(ratings, new Random(rand.nextInt()));

    // create a new ArrayList because subList() returns view and not serialzable
    List<TrainingData.Rating> trainingRatings =
      new ArrayList<TrainingData.Rating>(ratings.subList(0, trainingEndIndex));
    List<TrainingData.Rating> testRatings = ratings.subList(trainingEndIndex, testEndIndex);
    TrainingData td = new TrainingData(trainingRatings);
    List<Tuple2<Query, Float>> qaList = prepareValidation(testRatings);

    data.add(new Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Float>>>(
      new EmptyParams(), td, qaList));
  }

  return data;
}
```

## Step 2 - Metrics

In this tutorial, we will implement a root means square error (RMSE) metric. The Metrics extends the io.prediction.controller.java.JavaMetrics, which have three methods need to be override.

```java
abstract def computeUnit(query: Q, predicted: P, actual: A): MU

abstract def computeSet(dataParams: DP, metricUnits: Iterable[MU]): MR

abstract def computeMultipleSets(input: Iterable[(DP, MR)]): MMR
```

computeUnit() computes the Metric Unit (MU) for each Prediction and Actual based on Query.

For the RMSE Metrics, computeUnit() returns square error of each predicted rating (Prediction) and actual rating (Actual).

```java
@Override
public Double computeUnit(Query query, Float predicted, Float actual) {
  logger.info("Q: " + query.toString() + " P: " + predicted + " A: " + actual);
  // return squared error
  double error = predicted - actual;
  return (error * error);
}
```

ComputeSet() gathers the Iterable of Metric Unit (MU) of the same set and compute the Metric Result (MR) for this set.

```java
@Override
public Double computeSet(EmptyParams dataParams, Iterable<Double> metricUnits) {
  double sum = 0.0;
  int count = 0;
  for (double squareError : metricUnits) {
    sum += squareError;
    count += 1;
  }
  return Math.sqrt(sum / count);
}
```

In some cases, we want to do anther computation using the metric results of all sets. computeMultipleSets() is to gather the Metric Result of the sets and do a final computation.

In this tutorial, it simply combines the array of Metric Results as String and return it. PredictionIO will display this final result in the terminal.

```java
@Override
public String computeMultipleSets(
  Iterable<Tuple2<EmptyParams, Double>> input) {
  return Arrays.toString(IteratorUtils.toArray(input.iterator()));
}
```

## Step 3 - Run Evaluation

To run evaluation with metric. Simply add the Metric class to the runEngin() in Runner (as shown in Runner3.java).

Since the metic doesn't take parameter. A new EmptyParams instance is used.

```java
JavaAPIDebugWorkflow.runEngine(
  "MyEngine",
  new HashMap<String, String>(),
  3, // verbose
  (new EvaluationEngineFactory()).apply(),
  engineParams,
  Metrics.class,
  new EmptyParams()
);

```

Execute the following command:

```
bin/pio-run io.prediction.engines.java.recommendations.Runner3 data/test/ratings.csv
```

You should see the following output, the Metric Result (RMSE score) of each iteration is printed at the end.

```
14/07/24 21:46:58 INFO SparkContext: Job finished: collect at DebugWorkflow.scala:624, took 0.096702 s
14/07/24 21:46:58 INFO APIDebugWorkflow$: DataSourceParams: io.prediction.engines.java.recommendations.DataSourceParams@2e32ef03
14/07/24 21:46:58 INFO APIDebugWorkflow$: PreparatorParams: Empty
14/07/24 21:46:58 INFO APIDebugWorkflow$: Algo: 0 Name: MyRecommendationAlgo Params: io.prediction.engines.java.recommendations.AlgoParams@302684c7
14/07/24 21:46:58 INFO APIDebugWorkflow$: ServingParams: Empty
14/07/24 21:46:58 INFO APIDebugWorkflow$: MetricsParams: Empty
14/07/24 21:46:58 INFO APIDebugWorkflow$: [(Empty,1.1281266588308874), (Empty,3.8078865529319543), (Empty,2.0)]
14/07/24 21:46:58 INFO APIDebugWorkflow$: APIDebugWorkflow.run completed.
```

## Step 4 - Running with MovieLens 100K data set:

Run the following to fetch the data set. The ml-100k will be downloaded into the data/ directory.

```
engines/src/main/java/recommendations/fetch.sh
```

Re-run Runner3 with the ml-100k data set:

```
bin/pio-run io.prediction.engines.java.recommendations.Runner3 data/ml-100k/u.data
```

You should see the following when it completes. The RMSE metric scores of each iteration is printed at the end:

```
14/07/25 15:15:16 INFO SparkContext: Job finished: collect at DebugWorkflow.scala:624, took 15.047465 s
14/07/25 15:15:16 INFO APIDebugWorkflow$: DataSourceParams: io.prediction.engines.java.recommendations.DataSourceParams@27855781
14/07/25 15:15:16 INFO APIDebugWorkflow$: PreparatorParams: Empty
14/07/25 15:15:16 INFO APIDebugWorkflow$: Algo: 0 Name: MyRecommendationAlgo Params: io.prediction.engines.java.recommendations.AlgoParams@794f7330
14/07/25 15:15:16 INFO APIDebugWorkflow$: ServingParams: Empty
14/07/25 15:15:16 INFO APIDebugWorkflow$: MetricsParams: Empty
14/07/25 15:15:16 INFO APIDebugWorkflow$: [(Empty,1.0130495024565813), (Empty,1.0137187039177467), (Empty,1.0248640242912115)]
14/07/25 15:15:16 INFO APIDebugWorkflow$: APIDebugWorkflow.run completed.
```

Up to this point, you should be familiar with basic components (DataSource, Algorithm and Metrics) of PredictionIO and know how to develop your algorithms and prediction engines, deploy them and serve real time prediction queries. Moreover, you could develop your metrics to run offline evaluations.

As mentioned in earlier tutorials, each engine can contain multiple algorithms. Also, we haven't explained much about the Prepartor and Serving layer. Next we will demonstrate how to use Prepartor, multiple algorithms and Serving layer to improve the prediction engines.

Next: [Tutorial 4 - Multiple Algorithms Engine](tutorial4-multialgo.md)

# Tutorial 1 - Develop and Integrate Algorithm with PredictionIO

In this tutorial, we will build a simple **Java single machine recommendation algorithm ** to demonstrate how you could develop your own algorithm and prediction engine with PredictionIO. Movie-lens 100k data set will be used.

We will implement two basic controller components: DataSource and Algorithm.

## Step 1. Define the data class type

A Prediction Engine needs Training Data (TD), and make Prediction (P) based on the input Query (Q).

Let's define the data as following:

- Training Data (TD): List of user ID, item ID and ratings, as defined in Training.java.

```java
public class TrainingData implements Serializable {
  public List<Rating> ratings;

  public TrainingData(List<Rating> ratings) {
    this.ratings = ratings;
  }

  public static class Rating implements Serializable {
    public int uid; // user ID
    public int iid; // item ID
    public float rating;

    public Rating(int uid, int iid, float rating) {
      this.uid = uid;
      this.iid = iid;
      this.rating = rating;
    }
}
```

- Input Query (Q): user ID and item ID, as defined in the Query.java.

```java
// code
```

- Prediction output (P): predicted preference value. Primitive class Float can be used.

```java
  Float
```

If the data is simple field, you may use primitive class such as Integer, Float (such as Predcition output in this example). If your data contain multiple fields, you may define your own class (such as Query in this example). The requirement is that the data class must implement the Serializable interface.


## Step 2. Implement DataSource

DataSource is responsible for reading data from the source (Eg. Database or text file, etc) and prepare the Training Data for the Engine.

In this example, the DataSource needs one parameter which specifies the path of file containing the rating data. We can define the DataSource parameter class as following (in DataSourceParams.java):

```java
public class DataSourceParams implements Params {
  public String filePath; // file path

  public DataSourceParams(String path) {
    this.filePath = path;
  }
}
```

Each controller component (Eg. DataSource, Algorithm, etc) is restricted to having empty constructor or constructor which takes exactly one argument which is subclass of io.prediction.controller.Params, as shown above.

The DataSource component needs to extend io.prediction.controller.java.LJavaDataSource and specify the type of the data.

**LJavaDataSource** stands for Local Java Data Source, meaning that it's single machine Java Data Source.

```java
public class DataSource extends LJavaDataSource<
  DataSourceParams, EmptyParams, TrainingData, Query, Object> {
    // ...
  }
```

You need to specify the types of the data in LJavaDataSource. You can ignore the EmptyParams and Object for now. They are used for evaluation which will be explained in later tutorials.

The only function you need to implement is LJavaDataSource's read() method (as defined in DataSource.java). It reads comma or tab delimited rating file and return TrainingData which will be used by the Algorithm for training:

```java
@Override
public Iterable<Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Object>>>> read() {

  File ratingFile = new File(params.filePath);
  Scanner sc = null;

  try {
    sc = new Scanner(ratingFile);
  } catch (FileNotFoundException e) {
    logger.error("Caught FileNotFoundException " + e.getMessage());
    System.exit(1);
  }

  List<TrainingData.Rating> ratings = new ArrayList<TrainingData.Rating>();

  while (sc.hasNext()) {
    String line = sc.nextLine();
    String[] tokens = line.split("[\t,]");
    try {
      TrainingData.Rating rating = new TrainingData.Rating(
        Integer.parseInt(tokens[0]),
        Integer.parseInt(tokens[1]),
        Float.parseFloat(tokens[2]));
      ratings.add(rating);
    } catch (Exception e) {
      logger.error("Can't parse rating file. Caught Exception: " + e.getMessage());
      System.exit(1);
    }
  }

  List<Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Object>>>> data =
    new ArrayList<Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Object>>>>();

  data.add(new Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Object>>>(
    new EmptyParams(),
    new TrainingData(ratings),
    new ArrayList<Tuple2<Query, Object>>()
  ));

  return data;
}
```



## Step 3. Implement Algorithm

In this example, a simple item based collaborative filtering algorithm is implemented for
demonstration purpose. The algorithm will take a threshold as parameter and discard any item pairs
with similarity lower than this threshold.  The algorithm param is defined in AlgoParams.java:

```java
public class AlgoParams implements Params {
  public double threshold;

  public AlgoParams(double threshold) {
    this.threshold = threshold;
  }
}
```

This algorithm reads a list of Rating as defined in TrainingData and generate
a Model which  consists of ItemSimilarity matrix and user History matrix, which will later be used for generating Prediction.

The Model is defined in Model.java:

```java
public class Model implements Serializable {
  public Map<Integer, RealVector> itemSimilarity;
  public Map<Integer, RealVector> userHistory;

  public Model(Map<Integer, RealVector> itemSimilarity,
    Map<Integer, RealVector> userHistory) {
    this.itemSimilarity = itemSimilarity;
    this.userHistory = userHistory;
  }
}
```

The Algorithm class must extend io.prediction.controller.java.LJavaAlgorithm.
Similarily, LJavaAlgorithm stands for Local (Single machine) Java Algorithm.


```java
public class Algorithm extends
  LJavaAlgorithm<AlgoParams, TrainingData, Model, Query, Float> {
  // ...
}
```

You need to implement two functions of LJavaAlgorithm:

* train

```java
@Override
public Model train(TrainingData data) {
  // ...
}
```

* predict

```java
@Override
public Float predict(Model model, Query query) {
  // ...
}
```

## Step 4. Test run DataSource

Before you deploy algorithm with PredictionIO workflow for deployment, you may wanna run some tests to go through the data pipeline step by step to make sure things are working fine.

A very simple testdata/ratings.csv is provided for quick testing. Each row of the file represents user ID, item ID, and the rating value.

```
1,1,2
1,2,3
1,3,4 ...
```

You can use JavaSimpleEngineBuilder to create an engine instance and JavaEngineParamsBuilder to build the parameters for the engine. Because we only want to test the Data Source component, only dataSourceClass and dataSourceParams are specified and leaving other components empty (Eg. Algorthim).

```java
public class Runner1 {

  // During development, one can build a semi-engine, only add the first few layers. In this
  // particular example, we only add until dataSource layer
  private static class HalfBakedEngineFactory implements IEngineFactory {
    public JavaSimpleEngine<TrainingData, EmptyParams, Query, Float, Object> apply() {
      return new JavaSimpleEngineBuilder<
        TrainingData, EmptyParams, Query, Float, Object> ()
        .dataSourceClass(DataSource.class)
        .build();
    }
  }

  public static void runComponents() {
    JavaEngineParams engineParams = new JavaEngineParamsBuilder()
      .dataSourceParams(new DataSourceParams(
        "engines/src/main/java/recommendations/testdata/ratings.csv"))
      .build();

    JavaAPIDebugWorkflow.runEngine(
      "MyEngine",
      new HashMap<String, String>(),
      3, // verbose
      (new HalfBakedEngineFactory()).apply(),
      engineParams,
      null,
      new EmptyParams()
    );
  }

  public static void main(String[] args) {
    runComponents();
  }
}
```

Execute the following command to run:

```
bin/pio-run io.prediction.engines.java.recommendations.Runner1
```

If it runs successfully, you should see the following console output at the end. It prints out the TrainigData the DataSource has generated.

```
14/07/24 14:43:12 INFO SparkContext: Job finished: collect at DebugWorkflow.scala:409, took 0.022313 s
14/07/24 14:43:12 INFO APIDebugWorkflow$: Data Set 0
14/07/24 14:43:12 INFO APIDebugWorkflow$: Params: Empty
14/07/24 14:43:12 INFO APIDebugWorkflow$: TrainingData:
14/07/24 14:43:12 INFO APIDebugWorkflow$: [TrainingData: [(1,1,2.0), (1,2,3.0), (1,3,4.0), (2,3,4.0), (2,4,1.0), (3,2,2.0), (3,3,1.0), (3,4,3.0), (4,1,5.0), (4,2,3.0), (4,4,2.0)]]
14/07/24 14:43:12 INFO APIDebugWorkflow$: TestingData: (count=0)
14/07/24 14:43:12 INFO APIDebugWorkflow$: Data source complete
14/07/24 14:43:12 INFO APIDebugWorkflow$: Preparator is null. Stop here
```

## Step 5. Test run Algorithm

Next step is to test the Algorithm component. You could simply add algorithmClass and algorithmParams in the JavaSimpleEngineBuilder and JavaEngineParamsBuilder. As shown in the following (in Runner2.java):

An algorithm named **"MyRecommendationAlgo"** is added to JavaSimpleEngineBuilder.
The method addAlgorithmClass() takes the name of the algorithm and the class of the algorithm as arguments:

```java
return new JavaSimpleEngineBuilder<
  TrainingData, EmptyParams, Query, Float, Object> ()
  .dataSourceClass(DataSource.class)
  .addAlgorithmClass("MyRecommendationAlgo", Algorithm.class)
  .build();
```
Similarly, the method addAlgorithmParams() takes the same name of the algorithm as specified in addAlgorithmClass() and the parameters for this algorithm. The engine will create a algorithm instance using this parameter:

```java
JavaEngineParams engineParams = new JavaEngineParamsBuilder()
  .dataSourceParams(new DataSourceParams(
    "engines/src/main/java/recommendations/testdata/ratings.csv"))
  .addAlgorithmParams("MyRecommendationAlgo", new AlgoParams(0.1))
  .build();
```

Note that an engine can contain multiple algorithm classes and you can specify multiple algoithm parameters for the same or different algorithm classes (This will be demonstrated in later turtorial).

Execute the following command to run:

```
bin/pio-run io.prediction.engines.java.recommendations.Runner2
```

You should see the Model generated by the Algorithm at the end of the console output:

```
14/07/24 15:38:08 INFO TaskSetManager: Finished TID 15 in 31 ms on localhost (progress: 4/4)
14/07/24 15:38:08 INFO TaskSchedulerImpl: Removed TaskSet 3.0, whose tasks have all completed, from pool
14/07/24 15:38:08 INFO DAGScheduler: Stage 3 (collect at DebugWorkflow.scala:66) finished in 0.035 s
14/07/24 15:38:08 INFO SparkContext: Job finished: collect at DebugWorkflow.scala:66, took 0.047056 s
14/07/24 15:38:08 INFO APIDebugWorkflow$: [Model: [itemSimilarity: {1={0; 0.8313979616; 0.2586032735; 0.496291667}, 2={0.8313979616; 0; 0.5195887333; 0.6837634588}, 3={0.2586032735; 0.5195887333; 0; 0.3256694736}, 4={0.496291667; 0.6837634588; 0.3256694736; 0}}]
[userHistory: {1={2; 3; 4; 0}, 2={0; 0; 4; 1}, 3={0; 2; 1; 3}, 4={5; 3; 0; 2}}]]
14/07/24 15:38:08 INFO APIDebugWorkflow$: Serving is null. Stop here
```

After some basic testing, we are ready to deploy this engine to serve real time request!

Next: [Tutorial 2 - Deploy Engine](tutorial2-deploy.md)

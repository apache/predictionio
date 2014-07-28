# Tutorial 1 - Develop and Integrate Algorithm with PredictionIO

## Overview

These series of tutorials will walk through each components of **PredictionIO Framework**. We will demonstrate how to develop your machine learning algorithms and prediction engines, deploy them and serve real time prediction queries, develop your metrics to run offline evaluations, and improve prediction engine by using multiple algoritms. We will build a simple **Java single machine recommendation engine** which predicts item's rating value rated by the user. [MovieLens 100k](http://grouplens.org/datasets/movielens/) data set will be used as an example.

## Concept

A prediction **Engine** consists of the following controller components: **DataSource**, **Preparator**, **Alogithm**, and **Serving**. Another component **Metrics** is used to evaluate the
*Engine*.

- *DataSource* is responsible for reading data from the source (Eg. database or text file, etc) and prepare the **Training Data (TD)**.
- *Preparator* takes the *Training Data* and generates **Prepared Data (PD)** for the *Algorithm*
- *Alogithm* takes the *Prepared data* to trains a **Model (M)** which is used make **Prediction (P)** outputs based on input **Query (Q)**.
- *Serving* serves the input *Query* with *Algorithm*'s *Predcition* outputs.

An **Engine Factory** is a factory which returns an *Engine* with the above components defined.

To evaluate a prediction *Engine*:
- *DataSource* can also generate *Test Data* which is a list of input *Query* and **Actual (A)** result.
- *Metrics* computes the **Metric Result (MR)** by comparing the *Prediction* output with the *Actual* result. PredictionIO feeds the input *Query* to the *Engine* to get the *Prediction* outputs which are compared with *Acvtual* results.

As you can see, the controller components (*DataSource, Preparator, Alogithm, Serving and Metrics*) are the building blocks of the data pipeline and the data types (*Training Data*, *Prepared Data*, *Model*, *Query*, *Prediction* and *Actual*) defines the types of data being passed between each component.

Note that if the *Algorithm* can directly use *Training Data* without any pre-processing, the *Prepared Data* can be the same as *Training Data* and a default **Identity Preparator** can be used, which simply passes the *Training Data* as *Prepared Data*.

Also, if there is only one *Algorithm* in the *Engine*, and there is no special post-processing on the *Prediction* outputs, a default **First Serving** can be use, which simply uses the *Algorithm*'s *Prediction* output to serve the query.

In this first tutorial, we will demonstrate how to build an simple Item Recommendation Engine with the *DataSource* and *Algorithm* components. You can find all sources code of this tutorial in the directory `engines/src/main/java/recommendations/tutorial1/`.

## Step 1. Define the data class type

For this Item Recommendation Engine, the data class types are defined as following:

- *Training Data (TD)*: List of user ID, item ID and ratings, as defined in `Training.java`.

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

- *Input Query (Q)*: User ID and item ID, as defined in the `Query.java`.

```java
public class Query implements Serializable {
  public int uid; // user ID
  public int iid; // item ID

  public Query(int uid, int iid) {
    this.uid = uid;
    this.iid = iid;
  }
}

```

- *Prediction output (P)*: Predicted preference value. Primitive class `Float` can be used.


- *Prepared Data (PD)*: Because the algorithm can directly use the `TrainingData`, the same `TrainingData` is used and no need to define *Prepared Data* separately.


- *Model (M)*: Because it's data type returned by the *Algorithm* component, We will define this when we implement the algorithm.

- *Actual (A)*: in this tutorial, we are not going to do evaluation which will be explained in later tutorials. We can simply use `Object` type for it.

As you can see, if the data is simple field, you may use primitive class type such as `Integer`, `Float`. If your data contain multiple fields, you may define your own class (such as `Query` in this tutorial). The requirement is that the data class must implement the `Serializable` interface.


## Step 2. Implement DataSource

The *DataSource* component is responsible for reading data from the source (Eg. database or text file, etc) and prepare the *Training Data (TD)*.

In this tutorial, the *DataSource* component needs one parameter which specifies the path of file containing the rating data.

Note that each controller component (*DataSource, Preparator, Alogithm, Serving and Metrics*) is restricted to having empty constructor or constructor which takes exactly one argument which must implement the `io.prediction.controller.Params` interface.

We can define the DataSource parameter class as following (in `DataSourceParams.java`):

```java
public class DataSourceParams implements Params {
  public String filePath; // file path

  public DataSourceParams(String path) {
    this.filePath = path;
  }
}
```

The *DataSource* component must extend `io.prediction.controller.java.LJavaDataSource`:

```java
abstract class LJavaDataSource[DSP <: Params, DP, TD, Q, A]
```

`LJavaDataSource` stands for *Local Java DataSource*, meaning that it is a Java *DataSource* component which can be run in single machine, which requires the following type parameters:

- `DSP`: *DataSource Parameters* class, which is the `DataSourceParams` class we just defined above.
- `DP`: *Data Parameters* class. It is used to describe the generated *Training Data* and the Test Data *Query and Actual*, which is used by *Metrics* during evaluation. Because we are not going to demonstrate evaluation in this first tutorial. `Object` type can be used.
- `TD`: *Training Data* class, which is the `TrainingData` class defined in step 1.
- `Q`: Input *Query* class, which is the `Query` class defined in step 1.
- `A`: *Actual* result class, which is the `Object` class defined in step 1.

```java
public class DataSource extends LJavaDataSource<
  DataSourceParams, Object, TrainingData, Query, Object> {
    // ...
  }
```

The only function you need to implement is `LJavaDataSource`'s `read()` method.

```java
abstract def read(): Iterable[(DP, TD, Iterable[(Q, A)])]
```

The `read()` method should read data from the source (Eg. database or text file, etc) and return the *Training Data* (`TD`) and *Test Data* (`Iterable[(Q, A)]`) with a *Data Parameters* (`DP`) associated with this *Training and Test Data Set*.

Note that the `read()` method's return type is `Iterable` because it could return one or more of *Training and Test Data Set*. For example, we may want to evaluate the engine with multiple iterations of random training and test split. In this case, each set corresponds to each split.

Because we are going to only demonstrate deploying *Engine* in this first tutorial, the `read()` will only return one set of *Training Data* and the *Test Data* will simply an empty list.

You could find the implementation of `read()` in `DataSource.java`. It reads comma or tab delimited rating file and return `TrainingData`.


## Step 3. Implement Algorithm

In this tutorial, a simple item based collaborative filtering algorithm is implemented for demonstration purpose. This algorithm computes the item similarity score between each item and returns a *Model* which consists of the item similarity scores and users' rating history. The item similarity scores and users' rating history will be used to compute the predicted rating value of an item by the user.

This algorithm takes a threshold as parameter and discard any item pairs
with similarity lower than this threshold. The algorithm parameters class is defined in `AlgoParams.java`:

```java
public class AlgoParams implements Params {
  public double threshold;

  public AlgoParams(double threshold) {
    this.threshold = threshold;
  }
}
```

The *Model* generated by the algorithm is defined in `Model.java`:

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

The *Algorithm* component must extend `io.prediction.controller.java.LJavaAlgorithm`.

```java
abstract class LJavaAlgorithm[AP <: Params, PD, M, Q, P]
```
Similar to `LJavaDataSource`, `LJavaAlgorithm` stands for *Local Java Algorithm*, meaning that it is a Java *Algorithm* component which can be run in single machine, which requires the following type parameters:

- `AP`: *Algorithm Parameters* class, which is the `AlgoParams` class we just defined above.
- `PD`: *Prepared Data* class, which is the same as the `TrainingData` class as described in step 1.
- `M`: *Model* class, which is the `Model` class defined above.
- `Q`: Input *Query* class, which is the `Query` class defined in step 1.
- `P`: *Prediction* output class, which is the `Float` class defined in step 1.

```java
public class Algorithm extends
  LJavaAlgorithm<AlgoParams, TrainingData, Model, Query, Float> {
  // ...
}
```

You need to implement two methods of `LJavaAlgorithm`:

- `train` method:

```java
abstract def train(pd: PD): M
```

The `train` method produces a *Model* of type `M` from *Prepared Data* of type `PD`.


- `predict` method:

```java
abstract def predict(model: M, query: Q): P
```

The `predict` method produces a *Prediction* of type `P` from a *Query* of type `Q` and trained *Model* of type `M`.

You could find the implementation of these methods in `Algorithm.java`.


## Step 4. Implement Engine Factory

PredictionIO framework requires an *Engine Factory* which returns an *Engine* with the controller components defined.

The *Engine Factory* must implement the `io.prediction.controller.IEngineFactory` interface and implements the `apply()` method (as shown in `EngineFactory.java`):

```java
public class EngineFactory implements IEngineFactory {
  public JavaSimpleEngine<TrainingData, Object, Query, Float, Object> apply() {
    return new JavaSimpleEngineBuilder<
      TrainingData, Object, Query, Float, Object> ()
      .dataSourceClass(DataSource.class)
      .preparatorClass() // Use default Preparator
      .addAlgorithmClass("MyRecommendationAlgo", Algorithm.class)
      .servingClass() // Use default Serving
      .build();
  }
}
```

To build an *Engine*, we need to define the class of each component. An `JavaEngineBuilder` is used for this purpose. In this tutorial, because the *Prepared Data* is the same as *Training Data*, we can use `JavaSimpleEngineBuilder`.

As you can see, we specifies the class the `DataSource` and `Algorithm` we just implemented in above steps.

To deploy engine, we also need a serving layer. For `JavaSimpleEngine` with single algorithm, we can use the default *Serving* component by simply calling the method `servingClass()` without specifying any class name. Building a custom *Serving* components will be explained in later tutorials.

Note that an *Engine* can contain different algorithms. This will be demonstrated in later tutorials.

Note that the `addAlgorithmClass()` requires the name of algorithm ( "MyRecommendationAlgo" in this case) which will be used later when we specify the parameters for this algorithm.


## Step 5. Compile and Register Engine

Now, we have implemented all the necessary blocks to deploy this Item Recommendation Engine. Let's compile the code!

Execute the following commands:

```
sbt/sbt package
sbt/sbt engines/assemblyPackageDependency
```

Next, we need to register this Item Recommendation Engine into PredictionIO.

An engine Manifest manifest.json is needed to describe the Engine (defined in `manifest.json`):

```json
{
  "id": "io.prediction.engines.java.recommendations.tutorial1.EngineFactory",
  "version": "0.8.0-SNAPSHOT",
  "name": "Simple Recommendations Engine",
  "engineFactory": "io.prediction.engines.java.recommendations.tutorial1.EngineFactory"
}
```

The `engineFactory` is the class name of the `EngineFactory` class created above. The `id` and `version` will be referenced later when we run the engine.

Execute the following command to register the engine:

```
bin/register-engine engines/src/main/java/recommendations/tutorial1/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
```

The `register-engine` command takes the engine manifest file and the required jar files as arguments. Note that you need to register the engine again if you have modified and re-cmopiled the codes.

## Step 6. Define Parameters for the Engine

Our `DataSource` and `Algorithm` class requires parameters. We can use JSON files to specify these parameters. Also, we need to specify the name of the algorithm within the *Engine* we want to deploy with.

In this tutorial, the `DataSourceParams` has a parameter which is the file path of the ratings file. The JSON is defined as following (`params/dataSourceParams.json`):

```json
{ "filePath" :  "data/test/ratings.csv" }
```

Note that the key name (`filePath`) must be the same as the corresponding field name defined in the `DataSourceParams` class.

For algorithms, we need to define a JSON array (`params/algoParams.json`):

```json
[
  { "name": "MyRecommendationAlgo",
    "params" :  { "threshold" : 0.2 }
  }
]
```

The key `name` is the name of the algorithm which should match the one defined in the `EngineFactory` class in above step. The `params` defines the parameters for this algorithm.

Note that if your algorithm takes no parameter, you still need to put empty JSON `{}`. For example:

```json
[
  { "name": "MyAnotherRecommendationAlgo",
    "params" :  { }
  }
]
```

## Step 7. Run WorkFlow and Server

Now, we have everything in place. Let's run it!

We use `bin/run-workflow` to run the *Engine*, which builds and saves the algorithm *Model* for serving real time requests.

Execute the following commands:

```
bin/run-workflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.java.recommendations.tutorial1.EngineFactory --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/java/recommendations/tutorial1/params
```

The `--engineId` and `--engineVersion` corresponds to the `id` and `version` defined in the engine's  `manifest.json`. The `--jsonBasePath` is the base directory of parameters JSON files.

When it finishes, you should see the following at the end of terminal output:

```
14/07/24 16:55:13 INFO SparkContext: Job finished: collect at DebugWorkflow.scala:553, took 0.014837 s
14/07/24 16:55:13 INFO APIDebugWorkflow$: Metrics is null. Stop here
14/07/24 16:55:13 INFO APIDebugWorkflow$: Run information saved with ID: 201407240001
```

Note that there is `ID` returned at the end. In this example, it's `201407240001`.

Next, execute the `bin/run-server` command with the returned `ID`:

```
bin/run-server --runId 201407240001
```

This will create a server that by default binds to http://localhost:8000. You can visit that page in your web browser to check its status.

Now you can retrive prediction result by sending a HTTP request to the server with the `Query` as JSON payload. Remember that our `Query` class is defined with `uid` and `iid` fields. The JSON key name must be the same as the field names of the `Query` class (`uid` and `iid` in this example).

For example, retrieve the predicted preference for item ID 3 by user ID 1. Run the following in terminal:

```
curl -H "Content-Type: application/json" -d '{ "uid" : 1, "iid" : 3}' http://localhost:8000
```

You should see the predicted preference value returned:

```
2.667687177658081
```

Congratulations! Now you have built a prediction engine which uses the trained model to serve real-time queries and returns prediction results!

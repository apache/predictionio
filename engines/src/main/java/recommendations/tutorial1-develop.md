# Tutorial 1 - Develop and Integrate Algorithm with PredictionIO

In this tutorial, we will build a simple **Java single machine recommendation algorithm ** to demonstrate how you could develop your own algorithm and prediction engine with PredictionIO. Movie-lens 100k data set will be used.

The purpose of this tutorial is to help you to get familiar with the PredictionIO framework.

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
  DataSourceParams, EmptyParams, TrainingData, Query, Object>
```

You need to specify the types of the data in LJavaDataSource. You can ignore the EmptyParams and Object for now. They are used for evaluation which will be explained in later tutorials.

You need to override a DataSouce's read() function (as defined in DataSource.java):

```java
// code
```

## Step 3. Implement Algorithm

In this example, a simple item based collaborative filtering algorithm is implemented.
This algorithm reads a list of ratings value as defined in Training Data and generate a Model.

Create a Model.java and copy the following code:

```java
// code
```

The Algorithm class must extend io.prediction.controller.java.LJavaAlgorithm and override two functions: train() and predict().

In this example, the algorithm doesn't take any parameters (EmptyParams is used).

Create Algorithm.java and copy the following code:

```java
// code
```


## Step 4. Test run DataSource

Create testdata/ratings.csv:

```
test data
```

Create Runner.java and copy following:

```java
// code
```

Compile the code:

```
sbt/sbt package
sbt/sbt "project engines" assemblyPackageDependency
```

Execute the following:

```
bin/pio-run io.prediction.engines.java.recommendations.Runner1


$SPARK_HOME/bin/spark-submit --jars  engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar,engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar --deploy-mode "client" --class "io.prediction.engines.java.recommendations.Runner"  core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar
```

## Step 5. Test run Algorithm

Modify Runner.java to add Algorithm class:

```java
// code
```

Execute:

```
// command
```

You should see the following model printed

```
// sample output
```

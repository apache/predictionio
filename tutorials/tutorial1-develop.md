# Tutorial 1 - Develop and Integrate Algorithm with PredictionIO

In this tutorial, we will build a simple **Java single machine recommendation algorithm ** to demonstrate how your could develop your own algorithm and integrate it with PredictionIO. Movie-lens 100k data set will be used.

This prediction Engine will take user ID and item ID as input and return the predicted preference value of the item by the user. In this example, we will implement two minimal controller components required by PredictionIO framework: DataSource and Algorithm.

## Step 1. Define the data class type

A Prediction Engine needs Training Data (TD), and make Prediction (P) based on the input Query (Q).

If this data is simple field, you may primitive class such as Integer, Float. If your data has multiple fields, you may define your own class. The requirement is that the data class must implement Serializable interface.

Let's define our data as following:

- Training Data (TD): List of user ID, item ID and ratings. Create a file named TrainingData.java and copy the following code:

```java
// code
```

- Input Query (Q): user ID and item ID.

```java
// code
```

- Prediction output (P): predicted preference value. Primitive class Float can be used.

```java
  Float
```


## Step 2. Implement DataSource

DataSource is responsible for reading data from the source (Eg. Database or text file, etc) and prepare the Training Data for the Engine. This class needs to extend  io.prediction.controller.java.LJavaDataSource.

Each controller component (Eg. DataSource, Algorithm, etc) is restricted to having empty constructor or constructor which takes exactly one argument which is subclass of io.prediction.controller.Params as.

Define parameter class for the DataSource. Note that it needs to extend io.prediction.controller.Params. In this example, the DataSource parameter class only has one field which is the ratings file path.

Create a DataSourceParams.java file and copy the following code:

```java
// code
```

Create a DataSource.java and copy the following code. The DataSource extends io.prediction.controller.java.LJavaDataSource and must override the read() function which is repsonsible for reading the data from the source file and generating Training Data.

You can ignore the EmptyParams and Object for now. They are used for evaluation which will be explained in later tutorials.

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

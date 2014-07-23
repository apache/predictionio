# Tutorial 1 - Single Machine Java Recommendation Engine

In this tutorial, we will demonstrate how to build a single machine recommendation algorithm written in Java and integrate it with PredictionIO. Movie-lens 100k data set will be used.

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

You can ignore the EmptyParams and EmptyData for now. They are used for evaluation which will be explained in later tutorials.

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


## Step 2. Add Implmentation


## Step 3. Create Engine manifest and parameter json files

TBD

## Step 4. Compile and Run Evaluation

TBD

## Step 5. Deploy Engine

TBD

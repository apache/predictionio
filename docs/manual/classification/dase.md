---
layout: docs
title: DASE
---

# DASE Components Explained (Classification)

PredictionIO's DASE architecture brings the separation-of-concerns design principle to predictive engine development.
DASE stands for the following components of an engine:

* **D**ata - includes Data Source and Data Preparator
* **A**lgorithm(s)
* **S**erving
* **E**valuator

Let's look at the code and see how you can customize the Classification engine you built from the Classification Engine Template.

> Note: Evaluator will not be covered in this tutorial.

## The Engine Design

As you can see from the Quick Start, *MyEngine* takes a JSON prediction query, e.g. { "features": [4, 3, 8] }, and return
a JSON predicted result.

In MyEngine/src/main/scala/***Engine.scala***

`Query` case class defines the format of **query**, such as { "features": [4, 3, 8] }:

```scala
class Query(
  val features: Array[Double]
) extends Serializable

```

`PredictedResult` case class defines the format of **predicted result**, such as {"label":2.0}:

```scala
class PredictedResult(
  val label: Double
) extends Serializable
```

Finally, `ClassificationEngine` is the Engine Factory that defines the components this engine will use:
Data Source, Data Preparator, Algorithm(s) and Serving components.

```scala
object ClassificationEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("naive" -> classOf[NaiveBayesAlgorithm]),
      classOf[Serving])
  }
}
```

### Spark MLlib

Spark's MLlib NaiveBayes algorithm takes training data of RDD type, i.e. `RDD[LabeledPoint]` and train a model, which is a `NaiveBayesModel` object.

PredictionIO's MLlib Classification engine template, which *MyEngine* bases on, integrates this algorithm under the DASE architecture.
We will take a closer look at the DASE code below.
> [Check this out](https://spark.apache.org/docs/latest/mllib-naive-bayes.html) to learn more about MLlib's NaiveBayes algorithm.


## Data

In the DASE architecture, data is prepared by 2 components sequentially: *Data Source* and *Data Preparator*.
*Data Source* and *Data Preparator* takes data from the data store and prepares `RDD[LabeledPoint]` for the NaiveBayes algorithm.

### Data Source

In MyEngine/src/main/scala/***DataSource.scala***

The `def readTraining` of class `DataSource` reads, and selects, data from a data store and it returns `TrainingData`.

```scala
case class DataSourceParams(val filepath: String) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[DataSourceParams, EmptyDataParams,
  TrainingData, Query, EmptyActualResult] {

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val data = sc.textFile(dsp.filepath)
    val labeledPoints: RDD[LabeledPoint] = data.map { line =>
      val parts = line.split(',')
      LabeledPoint(parts(0).toDouble,
        Vectors.dense(parts(1).split(' ').map(_.toDouble)))
    }

    new TrainingData(labeledPoints)
  }
}
```

As seen, it reads from a text file with `sc.textFile` by default.
PredictionIO automatically loads the parameters of *datasource* specified in MyEngine/***engine.json***, including *filepath*, to `dsp`.

In ***engine.json***:

```
{
  ...
  "datasource": {
    "filepath": "./data/sample_naive_bayes_data.txt"
  }
  ...
}
```

In this sample text data file, columns are delimited by comma (,). The first column are labels. The second column are features. 


The class definition of `TrainingData` is:

```scala
class TrainingData(
  val labeledPoints: RDD[LabeledPoint]
) extends Serializable
```
and PredictionIO passes the returned `TrainingData` object to *Data Preparator*.


### Data Preparator

In MyEngine/src/main/scala/***Preparator.scala***

The `def prepare` of class `Preparator` takes `TrainingData`. It then conducts any necessary feature selection and data processing tasks.
At the end, it returns `PreparedData` which should contain the data *Algorithm* needs. For MLlib NaiveBayes, it is `RDD[LabeledPoint]`.

By default, `prepare` simply copies the unprocessed `TrainingData` data to `PreparedData`:

```scala
class PreparedData(
  val labeledPoints: RDD[LabeledPoint]
) extends Serializable

class Preparator
  extends PPreparator[EmptyPreparatorParams, TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    new PreparedData(trainingData.labeledPoints)
  }
}
```

PredictionIO passes the returned `PreparedData` object to Algorithm's `train` function.

## Algorithm

In MyEngine/src/main/scala/***NaiveBayesAlgorithm.scala***

The two functions of the algorithm class are `def train` and `def predict`.
`def train` is responsible for training a predictive model. PredictionIO will store this model and `def predict` is responsible for using this model to make prediction.

### def train

`def train` is called when you run **pio train**.  This is where MLlib NaiveBayes algorithm, i.e. `NaiveBayes.train`, is used to train a predictive model.

```scala
def train(data: PreparedData): NaiveBayesModel = {
    NaiveBayes.train(data.labeledPoints, ap.lambda)
}
```

In addition to `RDD[LabeledPoint]` (i.e. `data.labeledPoints`), `NaiveBayes.train` takes 1 parameter: *lambda*.

The values of this parameter is specified in *algorithms* of MyEngine/***engine.json***:

```
{
  ...
  "algorithms": [
    {
      "name": "naive",
      "params": {
        "lambda": 1.0
      }
    }
  ]
  ...
}
```
PredictionIO will automatically loads these values into the constructor `ap`, which has a corresponding case case `AlgorithmParams`:

```scala
case class AlgorithmParams(
  val lambda: Double
) extends Params
```

`NaiveBayes.train` then returns a `NaiveBayesModel` model. PredictionIO will automatically store the returned model.


### def predict

`def predict` is called when you send a JSON query to http://localhost:8000/queries.json. PredictionIO converts the query, such as  { "features": [4, 3, 8] } to the `Query` class you defined previously.  

The predictive model `NaiveBayesModel` of MLlib NaiveBayes offers a function called `predict`. `predict` takes a dense vector of features. 
It predicts the label of the item represented by this feature vector.

```scala
  def predict(model: NaiveBayesModel, query: Query): PredictedResult = {
    val label = model.predict(Vectors.dense(query.features))
    new PredictedResult(label)
  }
```

> You have defined the class `PredictedResult` earlier.

PredictionIO passes the returned `PredictedResult` object to *Serving*.

## Serving

The `def serve` of class `Serving` processes predicted result. It is also responsible for combining multiple predicted results into one if you have more than one predictive model.
*Serving* then returns the final predicted result. PredictionIO will convert it to a JSON response automatically.

In MyEngine/src/main/scala/***Serving.scala***

```scala
class Serving
  extends LServing[EmptyServingParams, Query, PredictedResult] {

  override
  def serve(query: Query,
    predictions: Seq[PredictedResult]): PredictedResult = {
    predictions.head
  }
}
```

When you send a JSON query to http://localhost:8000/queries.json, `PredictedResult` from all models
will be passed to `def serve` as a sequence, i.e. `Seq[PredictedResult]`.

> An engine can train multiple models if you specify more than one Algorithm component in `object RecommendationEngine` inside ***Engine.scala*.
>
> Since only one NaiveBayesAlgorithm is implemented by default, this Sequence contains one element.

In this case, `def serve` simply returns the predicted result of the first, and the only, algorithm, i.e. `predictions.head`.


Congratulations! You have just learned how to customize and build a production-ready engine. Have fun!
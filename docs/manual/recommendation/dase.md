---
layout: docs
title: DASE
---

# DASE Components Explained (Recommendation)

PredictionIO's DASE architecture brings the separation-of-concerns design principle to predictive engine development.
DASE stands for the following components of an engine:

* **D**ata - includes Data Source and Data Preparator
* **A**lgorithm(s)
* **S**erving
* **E**valuator

Let's look at the code and see how you can customize the Recommendation engine you built from the Recommendation Engine Template.

> Note: Evaluator will not be covered in this tutorial.

## The Engine Design

As you can see from the Quick Start, *MyEngine* takes a JSON prediction query, e.g. { "user": 1, "num": 4 }, and return
a JSON predicted result.

In MyEngine/src/main/scala/***Engine.scala***

`Query` case class defines the format of **query**, such as { "user": 1, "num": 4 }:

```scala
case class Query(
  val user: Int,
  val num: Int
) extends Serializable
```

`PredictedResult` case class defines the format of **predicted result**, such as {"productScores":[{"product":22,"score":4.07},{"product":62,"score":4.05},{"product":75,"score":4.04},{"product":68,"score":3.81}]}:

```scala
case class PredictedResult(
  val productScores: Array[ProductScore]
) extends Serializable

case class ProductScore(
  product: Int,
  score: Double
) extends Serializable
```

Finally, `RecommendationEngine` is the Engine Factory that defines the components this engine will use:
Data Source, Data Preparator, Algorithm(s) and Serving components.

```scala
object RecommendationEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("als" -> classOf[ALSAlgorithm]),
      classOf[Serving])
  }

```

### Spark MLlib

Spark's MLlib ALS algorithm takes training data of RDD type, i.e. `RDD[Rating]` and train a model, which is a `MatrixFactorizationModel` object.

PredictionIO's MLlib Collaborative Filtering engine template, which *MyEngine* bases on, integrates this algorithm under the DASE architecture.
We will take a closer look at the DASE code below.
> [Check this out](https://spark.apache.org/docs/latest/mllib-collaborative-filtering.html) to learn more about MLlib's ALS collaborative filtering algorithm.


## Data

In the DASE architecture, data is prepared by 2 components sequentially: *Data Source* and *Data Preparator*.
*Data Source* and *Data Preparator* takes data from the data store and prepares `RDD[Rating]` for the ALS algorithm.

### Data Source

In MyEngine/src/main/scala/***DataSource.scala***

The `def readTraining` of class `DataSource` reads, and selects, data from datastore of EventServer and it returns `TrainingData`.

```scala
case class DataSourceParams(val appId: Int) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[DataSourceParams, EmptyDataParams,
  TrainingData, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val eventsDb = Storage.getPEvents()
    val eventsRDD: RDD[Event] = eventsDb.find(
      appId = dsp.appId,
      entityType = Some("user"),
      eventNames = Some(List("rate", "buy")), // read "rate" and "buy" event
      // targetEntityType is optional field of an event.
      targetEntityType = Some(Some("item")))(sc)

    val ratingsRDD: RDD[Rating] = eventsRDD.map { event =>
      val rating = try {
        val ratingValue: Double = event.event match {
          case "rate" => event.properties.get[Double]("rating")
          case "buy" => 4.0 // map buy event to rating value of 4
          case _ => throw new Exception(s"Unexpected event ${event} is read.")
        }
        // assume entityId and targetEntityId is originally Int type
        Rating(event.entityId.toInt,
          event.targetEntityId.get.toInt,
          ratingValue)
      } catch {
        case e: Exception => {
          logger.error(s"Cannot convert ${event} to Rating. Exception: ${e}.")
          throw e
        }
      }
      rating
    }
    new TrainingData(ratingsRDD)
  }
}
```

`Storage.getPEvents()` gives you access to data you collected through Event Server and `eventsDb.find` specifies the events you want to read.
PredictionIO automatically loads the parameters of *datasource* specified in MyEngine/***engine.json***, including *appId*, to `dsp`.

In ***engine.json***:

```
{
  ...
  "datasource": {
    "appId": 1
  },
  ...
}
```


The class definition of `TrainingData` is:

```scala
class TrainingData(
  val ratings: RDD[Rating]
) extends Serializable
```
and PredictionIO passes the returned `TrainingData` object to *Data Preparator*.

<!-- TODO
> HOW-TO:
>
> You may modify readTraining function to read from other datastores, such as MongoDB -  [link]
-->


### Data Preparator

In MyEngine/src/main/scala/***Preparator.scala***

The `def prepare` of class `Preparator` takes `TrainingData`. It then conducts any necessary feature selection and data processing tasks.
At the end, it returns `PreparedData` which should contain the data *Algorithm* needs. For MLlib ALS, it is `RDD[Rating]`.

By default, `prepare` simply copies the unprocessed `TrainingData` data to `PreparedData`:

```scala
class Preparator
  extends PPreparator[EmptyPreparatorParams, TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    new PreparedData(ratings = trainingData.ratings)
  }
}

class PreparedData(
  val ratings: RDD[Rating]
) extends Serializable
```

PredictionIO passes the returned `PreparedData` object to Algorithm's `train` function.

<!-- TODO
> HOW-TO:
>
> MLlib ALS limitation: user id, item id must be integer - convert [link]
-->

## Algorithm

In MyEngine/src/main/scala/***ALSAlgorithm.scala***

The two functions of the algorithm class are `def train` and `def predict`.
`def train` is responsible for training a predictive model. PredictionIO will store this model and `def predict` is responsible for using this model to make prediction.

### def train

`def train` is called when you run **pio train**.  This is where MLlib ALS algorithm, i.e. `ALS.train`, is used to train a predictive model.

```scala
  def train(data: PreparedData): PersistentMatrixFactorizationModel = {
    val m = ALS.train(data.ratings, ap.rank, ap.numIterations, ap.lambda)
    new PersistentMatrixFactorizationModel(
      rank = m.rank,
      userFeatures = m.userFeatures,
      productFeatures = m.productFeatures)
  }
```

In addition to `RDD[Rating]`, `ALS.train` takes 3 parameters: *rank*, *iterations* and *lambda*.

The values of these parameters are specified in *algorithms* of MyEngine/***engine.json***:

```
{
  ...
  "algorithms": [
    {
      "name": "als",
      "params": {
        "rank": 10,
        "numIterations": 20,
        "lambda": 0.01
      }
    }
  ]
  ...
}
```
PredictionIO will automatically loads these values into the constructor `ap`, which has a corresponding case case `ALSAlgorithmParams`:

```scala
case class ALSAlgorithmParams(
  val rank: Int,
  val numIterations: Int,
  val lambda: Double) extends Params
```

`ALS.train` then returns a `MatrixFactorizationModel` model which contains RDD data.
RDD is a distributed collection of items which *does not* persist. To store the model, `PersistentMatrixFactorizationModel` extends `MatrixFactorizationModel` and makes it persistable.

> The detailed implementation can be found at MyEngine/src/main/scala/***PersistentMatrixFactorizationModel.scala***

PredictionIO will automatically store the returned model, i.e. `PersistentMatrixFactorizationModel` in this case.


### def predict

`def predict` is called when you send a JSON query to http://localhost:8000/queries.json. PredictionIO converts the query, such as  { "user": 1, "num": 4 } to the `Query` class you defined previously.  

The predictive model `MatrixFactorizationModel` of MLlib ALS, which is now extended as `PersistentMatrixFactorizationModel`,
offers a function called `recommendProducts`. `recommendProducts` takes two parameters: user id (i.e. query.user) and the number of products to be returned (i.e. query.num).
It predicts the top *num* of products a user will like.

```scala
def predict(
    model: PersistentMatrixFactorizationModel,
    query: Query): PredictedResult = {
    val productScores = model.recommendProducts(query.user, query.num)
      .map (r => ProductScore(r.product, r.rating))
    new PredictedResult(productScores)
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
> Since only one ALSAlgorithm is implemented by default, this Sequence contains one element.


Now you have a good understanding of the DASE model, we will show you an example of customizing the Data Preparator to exclude certain items from your training set.

#### [Next: Customizing Data Preparator](customize-data-prep.html)



<!-- TODO
> HOW-TO:
>
> Recommend products that the targeted user has not seen before [link]
>
> Give higher priority to newer products
>
> Combining several predictive model to improve prediction accuracy
-->

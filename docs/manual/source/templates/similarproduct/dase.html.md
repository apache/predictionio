---
title: DASE Components Explained (Similar Product)
---

PredictionIO's DASE architecture brings the separation-of-concerns design
principle to predictive engine development. DASE stands for the following
components of an engine:

* **D**ata - includes Data Source and Data Preparator
* **A**lgorithm(s)
* **S**erving
* **E**valuator

Let's look at the code and see how you can customize the engine
you built from the Similar Product Engine Template.

(Evaluator will not be covered in this tutorial.)

## The Engine Design

As you can see from the Quick Start, *MySimilarProduct* takes a JSON prediction
query, e.g. `{ "items": ["i1"], "num": 4 }`, and return a JSON predicted result.
In MySimilarProduct/src/main/scala/***Engine.scala***, the `Query` case class
defines the format of such **query**:

```scala
case class Query(
  items: List[String],
  num: Int,
  categories: Option[Set[String]],
  whiteList: Option[Set[String]],
  blackList: Option[Set[String]]
) extends Serializable
```

The `PredictedResult` case class defines the format of **predicted result**,
such as

```json
{"itemScores":[
  {"item":22,"score":4.07},
  {"item":62,"score":4.05},
  {"item":75,"score":4.04},
  {"item":68,"score":3.81}
]}
```

with:

```scala
case class PredictedResult(
  itemScores: Array[ItemScore]
) extends Serializable

case class ItemScore(
  item: String,
  score: Double
) extends Serializable
```

Finally, `SimilarProductEngine` is the *Engine Factory* that defines the
components this engine will use: Data Source, Data Preparator, Algorithm(s) and
Serving components.

```scala
object SimilarProductEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("als" -> classOf[ALSAlgorithm]),
      classOf[Serving])
  }
}
```

### Spark MLlib

The PredictionIO Similar Product Engine Template integrates Spark's MLlib ALS algorithm under the DASE
architecture. We will take a closer look at the DASE code below.

It takes training data of RDD type, i.e. `RDD[Rating]` and train a model, which is a `MatrixFactorizationModel` object.

You can visit [here](https://spark.apache.org/docs/latest/mllib-collaborative-filtering.html) to learn more about MLlib's ALS collaborative filtering algorithm.


## Data

In the DASE architecture, data is prepared by 2 components sequentially: *DataSource* and *DataPreparator*. They take data
from the data store and prepare them for Algorithm.

### Data Source

In MyRecommendation/src/main/scala/***DataSource.scala***, the `readTraining`
method of class `DataSource` reads and selects data from the *Event Store*
(data store of the *Event Server*). It returns `TrainingData`.

```scala
case class DataSourceParams(appId: Int) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
      EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val eventsDb = Storage.getPEvents()

    // create a RDD of (entityID, User)
    val usersRDD: RDD[(String, User)] = eventsDb.aggregateProperties(...) ...

    // create a RDD of (entityID, Item)
    val itemsRDD: RDD[(String, Item)] = eventsDb.aggregateProperties(...) ...

    // get all "user" "view" "item" events
    val viewEventsRDD: RDD[ViewEvent] = eventsDb.find(...) ...

    new TrainingData(
      users = usersRDD,
      items = itemsRDD,
      viewEvents = viewEventsRDD
    )
  }
}
```

PredictionIO automatically loads the parameters of *datasource* specified in MySimilarProduct/***engine.json***, including *appId*, to `dsp`.

In ***engine.json***:

```
{
  ...
  "datasource": {
    "params" : {
      "appId": 1
    }
  },
  ...
}
```

In `readTraining()`, `Storage.getPEvents()` returns a data access object which you could use to access data that is collected by PredictionIO Event Server.

This Similar Product template requires "user" and "item" entities that are set by events.

`eventsDb.aggregateProperties(...)` aggregates properties of the `user` and `item` that are set, unset, or delete by events.


<!-- // Please refer to  ADD LINK) for more details of setting properties for entities by events.  -->

The following code aggregates the properties of `user` and then map each result to a `User()` object.

```scala

  // create a RDD of (entityID, User)
  val usersRDD: RDD[(String, User)] = eventsDb.aggregateProperties(
    appId = dsp.appId,
    entityType = "user"
  )(sc).map { case (entityId, properties) =>
    val user = try {
      User()
    } catch {
      case e: Exception => {
        logger.error(s"Failed to get properties ${properties} of" +
          s" user ${entityId}. Exception: ${e}.")
        throw e
      }
    }
    (entityId, user)
  }

```
In the template, `User()` object is a simple dummy as a placeholder for you to customize and expand.


Similarly, the following code aggregates `item` properties  and then map each result to an `Item()` object. By default, this template assumes each item has an optional property `categories`, which is a list of String.

```scala
  // create a RDD of (entityID, Item)
  val itemsRDD: RDD[(String, Item)] = eventsDb.aggregateProperties(
    appId = dsp.appId,
    entityType = "item"
  )(sc).map { case (entityId, properties) =>
    val item = try {
      // Assume categories is optional property of item.
      Item(categories = properties.getOpt[List[String]]("categories"))
    } catch {
      case e: Exception => {
        logger.error(s"Failed to get properties ${properties} of" +
          s" item ${entityId}. Exception: ${e}.")
        throw e
      }
    }
    (entityId, item)
  }
```

The `Item` case class is defined as

```scala
case class Item(categories: Option[List[String]])
```

`eventsDb.find(...)` specifies the events that you want to read. In this case, "user view item" events are read and then each is mapped to a `ViewEvent()` object.

```scala

  // get all "user" "view" "item" events
  val viewEventsRDD: RDD[ViewEvent] = eventsDb.find(
    appId = dsp.appId,
    entityType = Some("user"),
    eventNames = Some(List("view")),
    // targetEntityType is optional field of an event.
    targetEntityType = Some(Some("item")))(sc)
    // eventsDb.find() returns RDD[Event]
    .map { event =>
      val viewEvent = try {
        event.event match {
          case "view" => ViewEvent(
            user = event.entityId,
            item = event.targetEntityId.get,
            t = event.eventTime.getMillis)
          case _ => throw new Exception(s"Unexpected event ${event} is read.")
        }
      } catch {
        case e: Exception => {
          logger.error(s"Cannot convert ${event} to ViewEvent." +
            s" Exception: ${e}.")
          throw e
        }
      }
      viewEvent
    }

```

`ViewEvent` case class is defined as:

```scala
case class ViewEvent(user: String, item: String, t: Long)
```

INFO: For flexibility, this template is designed to support user ID and item ID in String.

`TrainingData` contains an RDD of `User`, `Item` and `ViewEvent` objects. The class definition of `TrainingData` is:

```scala
class TrainingData(
  val users: RDD[(String, User)],
  val items: RDD[(String, Item)],
  val viewEvents: RDD[ViewEvent]
) extends Serializable { ... }
```

PredictionIO then passes the returned `TrainingData` object to *Data Preparator*.

You could modify the DataSource to [read other event types](/similarproduct/multi-events-multi-algos/) other than the default **view**.

### Data Preparator

In MySimilarProduct/src/main/scala/***Preparator.scala***, the `prepare` method
of class `Preparator` takes `TrainingData` as its input and performs any
necessary feature selection and data processing tasks. At the end, it returns
`PreparedData` which should contain the data *Algorithm* needs.

By default, `prepare` simply copies the unprocessed `TrainingData` data to `PreparedData`:

```scala
class Preparator
  extends PPreparator[TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    new PreparedData(
      users = trainingData.users,
      items = trainingData.items,
      viewEvents = trainingData.viewEvents)
  }
}

class PreparedData(
  val users: RDD[(String, User)],
  val items: RDD[(String, Item)],
  val viewEvents: RDD[ViewEvent]
) extends Serializable
```

PredictionIO passes the returned `PreparedData` object to Algorithm's `train` function.

## Algorithm

In MySimilarProduct/src/main/scala/***ALSAlgorithm.scala***, the two methods of
the algorithm class are `train` and `predict`. `train` is responsible for
training the predictive model;`predict` is
responsible for using this model to make prediction.

### train(...)

`train` is called when you run **pio train**. This is where MLlib ALS algorithm,
i.e. `ALS.trainImplicit()`, is used to train a predictive model.


```scala
  def train(data: PreparedData): ALSModel = {

    ...

    // create User and item's String ID to integer index BiMap
    val userStringIntMap = BiMap.stringInt(data.users.keys)
    val itemStringIntMap = BiMap.stringInt(data.items.keys)

    // collect Item as Map and convert ID to Int index
    val items: Map[Int, Item] = data.items.map { case (id, item) =>
      (itemStringIntMap(id), item)
    }.collectAsMap.toMap

    val mllibRatings = data.viewEvents
      .map { r =>
        // Convert user and item String IDs to Int index for MLlib
        val uindex = userStringIntMap.getOrElse(r.user, -1)
        val iindex = itemStringIntMap.getOrElse(r.item, -1)

        if (uindex == -1)
          logger.info(s"Couldn't convert nonexistent user ID ${r.user}"
            + " to Int index.")

        if (iindex == -1)
          logger.info(s"Couldn't convert nonexistent item ID ${r.item}"
            + " to Int index.")

        ((uindex, iindex), 1)
      }.filter { case ((u, i), v) =>
        // keep events with valid user and item index
        (u != -1) && (i != -1)
      }.reduceByKey(_ + _) // aggregate all view events of same user-item pair
      .map { case ((u, i), v) =>
        // MLlibRating requires integer index for user and item
        MLlibRating(u, i, v)
      }

    // MLLib ALS cannot handle empty training data.
    require(!mllibRatings.take(1).isEmpty,
      s"mllibRatings cannot be empty." +
      " Please check if your events contain valid user and item ID.")

    // seed for MLlib ALS
    val seed = ap.seed.getOrElse(System.nanoTime)

    val m = ALS.trainImplicit(
      ratings = mllibRatings,
      rank = ap.rank,
      iterations = ap.numIterations,
      lambda = ap.lambda,
      blocks = -1,
      alpha = 1.0,
      seed = seed)

    new ALSModel(
      productFeatures = m.productFeatures,
      itemStringIntMap = itemStringIntMap,
      items = items
    )
  }
```

#### Working with Spark MLlib's ALS.trainImplicit(....)

MLlib ALS does not support `String` user ID and item ID. `ALS.trainImplicit` thus also assumes int-only `Rating` object. First, you can rename MLlib's Integer-only `Rating` to `MLlibRating` for clarity:

```
import org.apache.spark.mllib.recommendation.{Rating => MLlibRating}
```

In order to use MLlib's ALS algorithm, we need to convert the `viewEvents` into `MLlibRating`. There are two things we need to handle:

1. Map user and item String ID of the ViewEvent into Integer ID, as required by `MLlibRating`.
2. `ViewEvent` object is an implicit event that does not have an explicit rating value. `ALS.trainImplicit()` supports implicit preference. If the `MLlibRating` has higher rating value, it means higher confidence that the user prefers the item. Hence we can aggregate how many times the user has viewed the item to indicate the confidence level that the user may prefer the item.

You create a bi-directional map with `BiMap.stringInt` which maps each String record to an Integer index.

```scala
val userStringIntMap = BiMap.stringInt(data.users.keys)
val itemStringIntMap = BiMap.stringInt(data.items.keys)
```

Then convert the user and item String ID in each ViewEvent to Int with these BiMaps. We use default -1 if the user or item String ID couldn't be found in the BiMap and filter out these events with invalid user and item ID later. After filtering, we use `reduceByKey()` to add up all values for the same key (uindex, iindex) and then finally map to `MLlibRating` object.

```scala

val mllibRatings = data.viewEvents
  .map { r =>
    // Convert user and item String IDs to Int index for MLlib
    val uindex = userStringIntMap.getOrElse(r.user, -1)
    val iindex = itemStringIntMap.getOrElse(r.item, -1)

    if (uindex == -1)
      logger.info(s"Couldn't convert nonexistent user ID ${r.user}"
        + " to Int index.")

    if (iindex == -1)
      logger.info(s"Couldn't convert nonexistent item ID ${r.item}"
        + " to Int index.")

    ((uindex, iindex), 1)
  }.filter { case ((u, i), v) =>
    // keep events with valid user and item index
    (u != -1) && (i != -1)
  }.reduceByKey(_ + _) // aggregate all view events of same user-item pair
  .map { case ((u, i), v) =>
    // MLlibRating requires integer index for user and item
    MLlibRating(u, i, v)
  }

```

In addition to `RDD[MLlibRating]`, `ALS.trainImplicit` takes the following parameters: *rank*, *iterations*, *lambda" and "seed".

The values of these parameters are specified in *algorithms* of
MyRecommendation/***engine.json***:

```
{
  ...
  "algorithms": [
    {
      "name": "als",
      "params": {
        "rank": 10,
        "numIterations": 20,
        "lambda": 0.01,
        "seed": 3
      }
    }
  ]
  ...
}
```

PredictionIO will automatically loads these values into the constructor `ap`,
which has a corresponding case case `ALSAlgorithmParams`:

```scala
case class ALSAlgorithmParams(
  rank: Int,
  numIterations: Int,
  lambda: Double,
  seed: Option[Long]) extends Params
```

`ALS.trainImplicit()` then returns a `MatrixFactorizationModel` model which contains two RDDs: userFeatures and productFeatures. They correspond to the user X latent features matrix and item X latent features matrix, respectively. In this case, we will make use of the productFeatures matrix to find simliar products by comparing the similarity of the latent features. Hence, we store this productFeatures as defined in `ALSModel` class:

```scala
class ALSModel(
  val productFeatures: RDD[(Int, Array[Double])],
  val itemStringIntMap: BiMap[String, Int],
  val items: Map[Int, Item]
) extends IPersistentModel[ALSAlgorithmParams] with Serializable { ... }
```

PredictionIO will automatically store the returned model, i.e. `ALSModel` in this example.

### predict(...)

`predict` is called when you send a JSON query to
http://localhost:8000/queries.json. PredictionIO converts the query, such as `{ "items": ["i1"], "num": 4 }` to the `Query` class you defined previously.

We can use the productFeatures stored in ALSModel to calculate the similarity between the items in query and other items. Cosine Similarity is used in this case.

This template also supports additional business logic features, such as filtering items by categories, recommending items in the white list or excluding items in the black list.

The `predict()` function first calculate the similarities scores of the queries items in query versus all other items and then filtering items satisfying the `isCandidate()` condition. Then we take the top N items.

INFO: You can easily modify `isCandidate()` checking or `whiteList` generation if you have different requirements or condition to determine if an item is a candidate item to be recommended.

```scala
def predict(model: ALSModel, query: Query): PredictedResult = {

  // convert items to Int index
  val queryList: Set[Int] = query.items.map(model.itemStringIntMap.get(_))
    .flatten.toSet

  val queryFeatures: Vector[Array[Double]] = queryList.toVector.par
    .map { item =>
      // productFeatures may not contain the requested item
      val qf: Option[Array[Double]] = model.productFeatures
        .lookup(item).headOption
      qf
    }.seq.flatten

  val whiteList: Option[Set[Int]] = query.whiteList.map( set =>
    set.map(model.itemStringIntMap.get(_)).flatten
  )
  val blackList: Option[Set[Int]] = query.blackList.map ( set =>
    set.map(model.itemStringIntMap.get(_)).flatten
  )

  val ord = Ordering.by[(Int, Double), Double](_._2).reverse

  val indexScores: Array[(Int, Double)] = if (queryFeatures.isEmpty) {
    logger.info(s"No productFeatures vector for query items ${query.items}.")
    Array[(Int, Double)]()
  } else {
    model.productFeatures
      .mapValues { f =>
        queryFeatures.map{ qf =>
          cosine(qf, f)
        }.reduce(_ + _)
      }
      .filter(_._2 > 0) // keep items with score > 0
      .collect()
  }

  val filteredScore = indexScores.view.filter { case (i, v) =>
    isCandidateItem(
      i = i,
      items = model.items,
      categories = query.categories,
      queryList = queryList,
      whiteList = whiteList,
      blackList = blackList
    )
  }

  val topScores = getTopN(filteredScore, query.num)(ord).toArray

  val itemScores = topScores.map { case (i, s) =>
    new ItemScore(
      item = model.itemIntStringMap(i),
      score = s
    )
  }

  new PredictedResult(itemScores)
}
```

Note that the item IDs in top N results are the `Int` indices. You map them back to `String` with `itemIntStringMap` before they are returned:

```scala
  val itemScores = topScores.map { case (i, s) =>
    new ItemScore(
      item = model.itemIntStringMap(i),
      score = s
    )
  }

  new PredictedResult(itemScores)
```

> You have defined the class `PredictedResult` earlier.

PredictionIO passes the returned `PredictedResult` object to *Serving*.

## Serving

The `serve` method of class `Serving` processes predicted result. It is also
responsible for combining multiple predicted results into one if you have more
than one predictive model. *Serving* then returns the final predicted result.
PredictionIO will convert it to a JSON response automatically.

In MyRecommendation/src/main/scala/***Serving.scala***,

```scala
class Serving
  extends LServing[Query, PredictedResult] {

  override
  def serve(query: Query,
    predictedResults: Seq[PredictedResult]): PredictedResult = {
    predictedResults.head
  }
}
```

When you send a JSON query to http://localhost:8000/queries.json,
`PredictedResult` from all models will be passed to `serve` as a sequence, i.e.
`Seq[PredictedResult]`.

> An engine can train multiple models if you specify more than one Algorithm
component in `object RecommendationEngine` inside ***Engine.scala***. Since only
one `ALSAlgorithm` is implemented by default, this `Seq` contains one element.


#### [Next: Multiple Events and Multiple Algorithms](multi-events-multi-algos.html)

---
title: Training with Implicit Preference (Recommendation)
---

There are two types of user preferences:

- explicit preference (also referred as "explicit feedback"), such as "rating" given to item by users.
- implicit preference (also referred as "implicit feedback"), such as "view" and "buy" history.

MLlib ALS provides two functions, `ALS.train()` and `ALS.trainImplicit()` to handle these two cases, respectively.

By default, the recommendation template uses `ALS.train()` which expects explicit rating values.

To handle implicit preference, `ALS.trainImplicit()` can be used. In this case, the "rating" is used to calculate the confidence level that the user likes the item. Higher "rating" means a stronger indication that the user likes the item.

The following provides an example of using implicit preference.

### Training with view events

For example, if the more number of times the user has viewed the item, the higher confidence that the user likes the item. We can aggregate the number of views and use this as the "rating" value.

First, we can modify `DataSource.scala` to aggregate the number of views of the user on the same item:

```scala

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
      EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val eventsDb = Storage.getPEvents()
    val eventsRDD: RDD[Event] = eventsDb.find(
      appId = dsp.appId,
      entityType = Some("user"),
      eventNames = Some(List("view")), // MODIFIED
      // targetEntityType is optional field of an event.
      targetEntityType = Some(Some("item")))(sc)

    val ratingsRDD: RDD[Rating] = eventsRDD.map { event =>
      try {
        val ratingValue: Double = event.event match {
          case "view" => 1.0 // MODIFIED
          case _ => throw new Exception(s"Unexpected event ${event} is read.")
        }
        // MODIFIED
        // key is (user id, item id)
        // value is the rating value, which is 1.
        ((event.entityId, event.targetEntityId.get), ratingValue)
      } catch {
        case e: Exception => {
          logger.error(s"Cannot convert ${event} to Rating. Exception: ${e}.")
          throw e
        }
      }
    }
    // MODIFIED
    // sum all values for the same user id and item id key
    .reduceByKey { case (a, b) => a + b }
    .map { case ((uid, iid), r) =>
      Rating(uid, iid, r)
    }

    new TrainingData(ratingsRDD)
  }
}

```

NOTE: You may put the view count aggregation logic in `ALSAlgorithm`'s `train()` instead, depending on your needs.


Then, we can modify ALSAlgorithm.scala to call `ALS.trainImplicit()` instead of `ALS.train()`:

```scala

class ALSAlgorithm(val ap: ALSAlgorithmParams)
  extends PAlgorithm[PreparedData, ALSModel, Query, PredictedResult] {

  ...

  def train(data: PreparedData): ALSModel = {

    ...

    // MODIFIED
    val m = ALS.trainImplicit(
      ratings = mllibRatings,
      rank = ap.rank,
      iterations = ap.numIterations,
      lambda = ap.lambda,
      blocks = -1,
      alpha = 1.0,
      seed = seed)

    new ALSModel(
      rank = m.rank,
      userFeatures = m.userFeatures,
      productFeatures = m.productFeatures,
      userStringIntMap = userStringIntMap,
      itemStringIntMap = itemStringIntMap)
  }

  ...

}

```

Now the recommendation engine can train a model with implicit preference events.

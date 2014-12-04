---
title: Reading Custom Events (Recommendation)
---

You can modify the DataSource to read your custom events other than the default **rate** and **buy**, or events which involve different entity types other than the default **user** and **item**.

Please refer to [DataSource](dase.html#data) section for detailed explanation of the original DataSource implementation.

To read other events, modify the function call `eventsDb.find()` in MyRecommendation/src/main/scala/***DataSource.scala***:

- Specify the names of events in `eventNames` parameters
- Specify the entity types involved in the events in the `entityType` and `targetEntityType` parameters accordingly

For example, to read **like** events which involve entityType **customer** and targetEntityType **product**:

```scala
val eventsRDD: RDD[Event] = eventsDb.find(
      appId = dsp.appId,
      entityType = Some("customer"), // MODIFIED
      eventNames = Some(List("like")), // MODIFIED
      // targetEntityType is optional field of an event.
      targetEntityType = Some(Some("product")))(sc) // MODIFIED
```

Since the MLlib ALS algorithm uses `Rating` object as input, you also need to specify how to map your custom events into MLlib's Rating object. Modify the following section of code in MyRecommendation/src/main/scala/***DataSource.scala*** accordingly.

For example, to map a **like** event to a Rating object with value of 4:

```scala
val ratingsRDD: RDD[Rating] = eventsRDD.map { event =>
      val rating = try {
        val ratingValue: Double = event.event match {
          // MODIFIED
          case "like" => 4.0 // map a like event to a rating of 4.0
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
```

#### [Next: Customizing Data Preparator](customize-data-prep.html)

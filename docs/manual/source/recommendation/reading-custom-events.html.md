---
title: Reading Custom Events (Recommendation)
---

You can modify the [default DataSource](dase.html#data) to read
 
- Custom events other than the default **rate** and **buy** events. 
- Events which involve different entity types other than the default **user** and **item**.


## Add the Custom Event
To read custom events, modify the function call `eventsDb.find()` in MyRecommendation/src/main/scala/***DataSource.scala***:

- Specify the names of events in `eventNames` parameters
- Specify the entity types involved in the events in the `entityType` and `targetEntityType` parameters accordingly

In this example below, we modify DataSource to read a custom **like** event where a customer likes a product. The event has new entityType **customer** and targetEntityType **product**:

```scala
val eventsRDD: RDD[Event] = eventsDb.find(
      appId = dsp.appId,
      entityType = Some("customer"), // MODIFIED
      eventNames = Some(List("like")), // MODIFIED
      // targetEntityType is optional field of an event.
      targetEntityType = Some(Some("product")))(sc) // MODIFIED
```

## Map the Custom Event

 MLlib ALS algorithm uses `Rating` object as input, so it is neccesary to specify the mapping of your custom event to the MLlib's Rating object. You can do so in MyRecommendation/src/main/scala/***DataSource.scala***.

To map a **like** event to a Rating object with value of 4:

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

That's it! Your engine can read a custom like event. 


#### [Next: Customizing Data Preparator](customize-data-prep.html)

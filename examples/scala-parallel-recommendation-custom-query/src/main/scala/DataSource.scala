package org.template.recommendation

import io.prediction.controller.PDataSource
import io.prediction.controller.EmptyEvaluationInfo
import io.prediction.controller.EmptyActualResult
import io.prediction.controller.Params
import io.prediction.data.storage.Event
import io.prediction.data.storage.Storage

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import grizzled.slf4j.Logger

case class DataSourceParams(appId: Int) extends Params

case class User()

case class Item(creationYear: Option[Int])

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
      EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  private val creationDate = "creationDate"

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val eventsDb = Storage.getPEvents()

    // create a RDD of (entityID, Item)
    val itemsRDD = eventsDb.aggregateProperties(
      appId = dsp.appId,
      entityType = "item"
    )(sc).map { case (entityId, properties) =>
      entityId -> Item(properties.getOpt[Int](creationDate))
    }

    // get all user rate events
    val rateEventsRDD: RDD[Event] = eventsDb.find(
      appId = dsp.appId,
      entityType = Some("user"),
      eventNames = Some(List("rate")), // read "rate"
      // targetEntityType is optional field of an event.
      targetEntityType = Some(Some("item")))(sc)

    // collect ratings
    val ratingsRDD = rateEventsRDD.flatMap { event =>
      try {
        (event.event match {
          case "rate" => event.properties.getOpt[Double]("rating")
          case _ => None
        }).map(Rating(event.entityId, event.targetEntityId.get, _))
      } catch { case e: Exception =>
        logger.error(s"Cannot convert ${event} to Rating. Exception: ${e}.")
        throw e
      }
    }.cache()

    new TrainingData(ratingsRDD, itemsRDD)
  }
}

case class Rating(user: String, item: String, rating: Double)

class TrainingData(val ratings: RDD[Rating], val items: RDD[(String, Item)])
  extends Serializable {

  override def toString =
    s"ratings: [${ratings.count()}] (${ratings.take(2).toList}...)" +
    s"items: [${items.count()} (${items.take(2).toList}...)]"
}

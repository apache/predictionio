package org.template.similar

import io.prediction.controller.PDataSource
import io.prediction.controller.EmptyEvaluationInfo
import io.prediction.controller.EmptyActualResult
import io.prediction.controller.Params
import io.prediction.data.storage.Event
import io.prediction.data.storage.Storage
import io.prediction.data.storage.EntityMap

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import grizzled.slf4j.Logger

case class DataSourceParams(val appId: Int) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
      EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val eventsDb = Storage.getPEvents()

    val users: EntityMap[User] = eventsDb.extractEntityMap[User](
      appId = dsp.appId,
      entityType = "user"
    )(sc) { dm =>
      User()
    }

    val items: EntityMap[Item] = eventsDb.extractEntityMap[Item](
      appId = dsp.appId,
      entityType = "item"
    )(sc) { dm =>
      Item(
        categories = dm.getOpt[List[String]]("categories").getOrElse(List.empty)
      )
    }

    // get all "user" "view" "item" events
    val eventsRDD: RDD[Event] = eventsDb.find(
      appId = dsp.appId,
      entityType = Some("user"),
      eventNames = Some(List("view")),
      // targetEntityType is optional field of an event.
      targetEntityType = Some(Some("item")))(sc)

    // TODO: handle view same item multiple times
    val ratingsRDD: RDD[Rating] = eventsRDD.map { event =>
      val rating = try {
        val ratingValue: Double = event.event match {
          //case "rate" => event.properties.get[Double]("rating")
          case "view" => 1.0
          case _ => throw new Exception(s"Unexpected event ${event} is read.")
        }
        // entityId and targetEntityId is String
        Rating(event.entityId,
          event.targetEntityId.get,
          ratingValue)
      } catch {
        case e: Exception => {
          logger.error(s"Cannot convert ${event} to Rating. Exception: ${e}.")
          throw e
        }
      }
      rating
    }
    new TrainingData(
      users = users,
      items = items,
      ratings = ratingsRDD
    )
  }
}

case class User()

case class Item(val categories: List[String])

case class Rating(
  val user: String,
  val item: String,
  val rating: Double
)

class TrainingData(
  val users: EntityMap[User],
  val items: EntityMap[Item],
  val ratings: RDD[Rating]
) extends Serializable {
  override def toString = {
    s"users: [${users.size} (${users.take(2).toString}...)]" +
    s"items: [${items.size} (${items.take(2).toString}...)]" +
    s"ratings: [${ratings.count()}] (${ratings.take(2).toList}...)"
  }
}

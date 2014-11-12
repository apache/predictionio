package org.template.recommendation

import io.prediction.controller._
import io.prediction.data.storage.Event
import io.prediction.data.storage.Storage

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

import grizzled.slf4j.Logger

case class DataSourceParams(val appId: Int) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[DataSourceParams, EmptyDataParams,
  TrainingData, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val eventsDb = Storage.getEventDataPEvents()
    val eventsRDD: RDD[Event] = eventsDb.getByAppIdAndTimeAndEntity(
      appId = dsp.appId,
      startTime = None,
      untilTime = None,
      entityType = None,
      entityId = None)(sc)

    val ratings: RDD[Rating] = eventsRDD.map { event =>
      val r = try {
        // assume entityId and targetEntityId is originally Int type
        Rating(event.entityId.toInt,
          event.targetEntityId.get.toInt,
          event.properties.get[Double]("rating"))
      } catch {
        case e: Exception =>
          logger.error(s"Cannot convert ${e} to Rating. Exception: ${e}.")
        throw e
      }
      r
    }
    new TrainingData(ratings)
  }
}

class TrainingData(
  val ratings: RDD[Rating]
) extends Serializable {
  override def toString = {
    s"ratings: [${ratings.count()}] (${ratings.take(2).toList}...)"
  }
}

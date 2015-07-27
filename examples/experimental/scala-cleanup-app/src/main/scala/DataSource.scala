package io.prediction.examples.experimental.cleanupapp

import io.prediction.controller.PDataSource
import io.prediction.controller.EmptyEvaluationInfo
import io.prediction.controller.EmptyActualResult
import io.prediction.controller.Params
import io.prediction.data.storage.Event
import io.prediction.data.storage.Storage
import io.prediction.workflow.StopAfterReadInterruption

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import com.github.nscala_time.time.Imports._

import grizzled.slf4j.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

case class DataSourceParams(
  appId: Int,
  cutoffTime: DateTime
) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
      EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val eventsDb = Storage.getPEvents()
    val lEventsDb = Storage.getLEvents()
    logger.info(s"CleanupApp: $dsp")

    val countBefore = eventsDb.find(
      appId = dsp.appId
    )(sc).count
    logger.info(s"Event count before cleanup: $countBefore")

    val countRemove = eventsDb.find(
      appId = dsp.appId,
      untilTime = Some(dsp.cutoffTime)
    )(sc).count
    logger.info(s"Number of events to remove: $countRemove")

    logger.info(s"Remove events from appId ${dsp.appId}")
    val eventsToRemove: Array[String] = eventsDb.find(
      appId = dsp.appId,
      untilTime = Some(dsp.cutoffTime)
    )(sc).map { case e =>
      e.eventId.getOrElse("")
    }.collect

    var lastFuture: Future[Boolean] = Future[Boolean] {true}
    eventsToRemove.foreach { case eventId =>
      if (eventId != "") {
        lastFuture = lEventsDb.futureDelete(eventId, dsp.appId)
      }
    }
    // No, it's not correct to just wait for the last result.
    // This program only demonstrates how to remove old events.
    Await.result(lastFuture, scala.concurrent.duration.Duration(5, "minutes"))
    logger.info(s"Finish cleaning up events to appId ${dsp.appId}")

    val countAfter = eventsDb.find(
      appId = dsp.appId
    )(sc).count
    logger.info(s"Event count after cleanup: $countAfter")

    throw new StopAfterReadInterruption()
  }
}

class TrainingData(
) extends Serializable {
  override def toString = ""
}

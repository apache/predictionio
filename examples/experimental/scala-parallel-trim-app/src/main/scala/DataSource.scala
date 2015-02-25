package io.prediction.examples.experimental.trimapp

import io.prediction.controller.PDataSource
import io.prediction.controller.EmptyEvaluationInfo
import io.prediction.controller.EmptyActualResult
import io.prediction.controller.Params
import io.prediction.data.storage.Event
import io.prediction.data.storage.Storage

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import com.github.nscala_time.time.Imports._

import grizzled.slf4j.Logger

case class DataSourceParams(
  srcAppId: Int,
  dstAppId: Int,
  startTime: Option[DateTime],
  untilTime: Option[DateTime]
) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
      EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val eventsDb = Storage.getPEvents()
    logger.info(s"TrimApp: $dsp")


    logger.info(s"Read events from appId ${dsp.srcAppId}")
    val srcEvents: RDD[Event] = eventsDb.find(
      appId = dsp.srcAppId,
      startTime = dsp.startTime,
      untilTime = dsp.untilTime
    )(sc)

    val dstEvents: Array[Event] = eventsDb.find(appId = dsp.dstAppId)(sc).take(1)

    if (dstEvents.size > 0) {
      throw new Exception(s"DstApp ${dsp.dstAppId} is not empty. Quitting.")
    }

    logger.info(s"Write events to appId ${dsp.dstAppId}")
    eventsDb.write(srcEvents, dsp.dstAppId)(sc)
    
    logger.info(s"Finish writing events to appId ${dsp.dstAppId}")

    new TrainingData()
  }
}

class TrainingData(
) extends Serializable {
  override def toString = ""
}

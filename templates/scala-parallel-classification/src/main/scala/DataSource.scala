package org.template.classification

import io.prediction.controller._
import io.prediction.data.storage.Event
import io.prediction.data.storage.Storage

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors

case class DataSourceParams(val appId: Int) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[DataSourceParams, EmptyDataParams,
  TrainingData, Query, EmptyActualResult] {

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val eventsDb = Storage.getEventDataPEvents()
    val eventsRDD: RDD[Event] = eventsDb.find(
      appId = dsp.appId,
      startTime = None,
      untilTime = None,
      entityType = None,
      entityId = None,
      eventNames = Some(List("$set")))(sc) // read "$set" event

    val labeledPoints: RDD[LabeledPoint] = eventsRDD.map { event =>
      LabeledPoint(event.properties.get[Double]("plan"),
        Vectors.dense(Array(
          event.properties.get[Double]("attr0"),
          event.properties.get[Double]("attr1"),
          event.properties.get[Double]("attr2")
        ))
      )
    }

    new TrainingData(labeledPoints)
  }
}

class TrainingData(
  val labeledPoints: RDD[LabeledPoint]
) extends Serializable

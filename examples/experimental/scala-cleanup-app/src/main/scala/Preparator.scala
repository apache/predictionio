package io.prediction.examples.experimental.cleanupapp

import io.prediction.controller.PPreparator
import io.prediction.data.storage.Event

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

/*
class Preparator
  extends PPreparator[TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    new PreparedData(events = trainingData.events)
  }
}

class PreparedData(
  val events: RDD[Event]
) extends Serializable
*/

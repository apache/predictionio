package org.template.ecommercerecommendation

import io.prediction.controller.PPreparator

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

class Preparator
  extends PPreparator[TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    new PreparedData(
      users = trainingData.users,
      items = trainingData.items,
      rateEvents = trainingData.rateEvents) // MODIFIED
  }
}

case class PreparedData(
  users: RDD[(String, User)],
  items: RDD[(String, Item)],
  rateEvents: RDD[RateEvent] // MODIFIED
)

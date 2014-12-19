package io.prediction.examples.pfriendrecommendation

import io.prediction.controller.EmptyParams
import io.prediction.controller.PPreparator
import io.prediction.controller.EmptyPreparatorParams

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.io.Source // ADDED
import io.prediction.controller.Params // ADDED

 // ADDED CustomPreparatorParams case class
case class CustomPreparatorParams(
  val filepath: String
) extends Params

class IdentityPreparator(pp: EmptyPreparatorParams)
  extends PPreparator[TrainingData, TrainingData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): TrainingData = {
    trainingData
  }
}

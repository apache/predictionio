package org.examples.recommendation

import io.prediction.controller.PPreparator
import io.prediction.controller.Params

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

import scala.io.Source

case class CustomPreparatorParams(
  val filepath: String
) extends Params

class CustomPreparator(pp: CustomPreparatorParams)
  extends PPreparator[CustomPreparatorParams, TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    val noTrainItems = Source.fromFile(pp.filepath).getLines.map(_.toInt).toSet
    // exclude noTrainItems from original trainingData
    val ratings = trainingData.ratings.filter( r =>
      !noTrainItems.contains(r.product)
    )
    new PreparedData(ratings)
  }
}

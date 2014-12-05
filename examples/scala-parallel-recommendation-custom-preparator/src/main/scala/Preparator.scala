package org.template.recommendation

import io.prediction.controller.PPreparator
import io.prediction.controller.EmptyPreparatorParams

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

import scala.io.Source // ADDED
import io.prediction.controller.Params // ADDED

 // ADDED CustomPreparatorParams case class
case class CustomPreparatorParams(
  val filepath: String
) extends Params

class Preparator(pp: CustomPreparatorParams) // ADDED CustomPreparatorParams
  extends PPreparator[TrainingData, PreparedData] { // ADDED CustomPreparatorParams

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    val noTrainItems = Source.fromFile(pp.filepath).getLines.map(_.toInt).toSet //CHANGED
    val ratings = trainingData.ratings.filter( r =>
      !noTrainItems.contains(r.product)
    )
    new PreparedData(ratings)
  }
}

class PreparedData(
  val ratings: RDD[Rating]
) extends Serializable

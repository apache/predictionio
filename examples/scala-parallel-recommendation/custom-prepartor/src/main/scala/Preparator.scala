package org.template.recommendation

import io.prediction.controller.PPreparator

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.io.Source // ADDED
import io.prediction.controller.Params // ADDED

// ADDED CustomPreparatorParams case class
case class CustomPreparatorParams(
  filepath: String
) extends Params

class Preparator(pp: CustomPreparatorParams) // ADDED CustomPreparatorParams
  extends PPreparator[TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    val noTrainItems = Source.fromFile(pp.filepath).getLines.toSet //CHANGED
    val ratings = trainingData.ratings.filter( r =>
      !noTrainItems.contains(r.item)
    )
    new PreparedData(ratings)
  }
}

class PreparedData(
  val ratings: RDD[Rating]
) extends Serializable

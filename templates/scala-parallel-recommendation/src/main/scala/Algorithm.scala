package org.template.recommendation

import io.prediction.controller._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.mllib.recommendation.PMatrixFactorizationModel

case class AlgorithmParams(
  val rank: Int,
  val numIterations: Int,
  val lambda: Double,
  val persistModel: Boolean) extends Params

class Query(
  val user: Int,
  val product: Int
) extends Serializable

class Prediction(
  val rating: Double
) extends Serializable


class ALSAlgorithm(val ap: AlgorithmParams)
  extends PAlgorithm[AlgorithmParams, PreparedData,
      PMatrixFactorizationModel, Query, Prediction] {

  def train(data: PreparedData): PMatrixFactorizationModel = {
    val m = ALS.train(data.ratings, ap.rank, ap.numIterations, ap.lambda)
    new PMatrixFactorizationModel(
      rank = m.rank,
      userFeatures = m.userFeatures,
      productFeatures = m.productFeatures)
  }

  def predict(
    model: PMatrixFactorizationModel, query: Query): Prediction = {
    val rating = model.predict(query.user, query.product)
    new Prediction(rating)
  }

}

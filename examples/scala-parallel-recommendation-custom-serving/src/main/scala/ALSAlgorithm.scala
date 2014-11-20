package org.template.recommendation

import io.prediction.controller.PAlgorithm
import io.prediction.controller.Params

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.mllib.recommendation.PersistentMatrixFactorizationModel

case class ALSAlgorithmParams(
  val rank: Int,
  val numIterations: Int,
  val lambda: Double) extends Params

class ALSAlgorithm(val ap: ALSAlgorithmParams)
  extends PAlgorithm[PreparedData,
      PersistentMatrixFactorizationModel, Query, PredictedResult] {

  def train(data: PreparedData): PersistentMatrixFactorizationModel = {
    val m = ALS.train(data.ratings, ap.rank, ap.numIterations, ap.lambda)
    new PersistentMatrixFactorizationModel(
      rank = m.rank,
      userFeatures = m.userFeatures,
      productFeatures = m.productFeatures)
  }

  def predict(
    model: PersistentMatrixFactorizationModel,
    query: Query): PredictedResult = {
    val productScores = model.recommendProducts(query.user, query.num)
      .map (r => ProductScore(r.product, r.rating))
    new PredictedResult(productScores)
  }

}

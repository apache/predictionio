package org.template.recommendation

import io.prediction.controller.PAlgorithm
import io.prediction.controller.Params

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.{Rating => MLlibRating}
import org.apache.spark.mllib.recommendation.ALSModel

case class ALSAlgorithmParams(
  val rank: Int,
  val numIterations: Int,
  val lambda: Double) extends Params

class ALSAlgorithm(val ap: ALSAlgorithmParams)
  extends PAlgorithm[PreparedData,
      ALSModel, Query, PredictedResult] {

  def train(data: PreparedData): ALSModel = {
    // Convert user and product String IDs to Int index for MLlib
    val mllibRatings = data.ratings.map( r =>
      // MLlibRating requires integer index for user and product
      MLlibRating(data.users(r.user).toInt,
        data.items(r.product).toInt, r.rating)
    )
    val m = ALS.train(mllibRatings, ap.rank, ap.numIterations, ap.lambda)
    new ALSModel(
      rank = m.rank,
      userFeatures = m.userFeatures,
      productFeatures = m.productFeatures,
      users = data.users,
      items = data.items)
  }

  def predict(model: ALSModel, query: Query): PredictedResult = {
    // Convert String ID to Int index for Mllib
    val userIx = model.users(query.user).toInt
    // ALSModel returns product Int index. Convert it to String ID for
    // returning PredictedResult
    val productScores = model.recommendProducts(userIx, query.num)
      .map (r => ProductScore(model.items(r.product.toLong), r.rating))
    new PredictedResult(productScores)
  }

}

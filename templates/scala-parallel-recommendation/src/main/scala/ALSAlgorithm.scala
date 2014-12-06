package org.template.recommendation

import io.prediction.controller.PAlgorithm
import io.prediction.controller.Params
import io.prediction.data.storage.BiMap

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.{Rating => MLlibRating}
import org.apache.spark.mllib.recommendation.ALSModel

import grizzled.slf4j.Logger

case class ALSAlgorithmParams(
  val rank: Int,
  val numIterations: Int,
  val lambda: Double) extends Params

class ALSAlgorithm(val ap: ALSAlgorithmParams)
  extends PAlgorithm[PreparedData, ALSModel, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(data: PreparedData): ALSModel = {
    // Convert user and product String IDs to Int index for MLlib
    val userIdToIxMap = BiMap.stringInt(data.ratings.map(_.user))
    val productIdToIxMap = BiMap.stringInt(data.ratings.map(_.product))
    val mllibRatings = data.ratings.map( r =>
      // MLlibRating requires integer index for user and product
      MLlibRating(userIdToIxMap(r.user), productIdToIxMap(r.product), r.rating)
    )
    val m = ALS.train(mllibRatings, ap.rank, ap.numIterations, ap.lambda)
    new ALSModel(
      rank = m.rank,
      userFeatures = m.userFeatures,
      productFeatures = m.productFeatures,
      userIdToIxMap = userIdToIxMap,
      productIdToIxMap = productIdToIxMap)
  }

  def predict(model: ALSModel, query: Query): PredictedResult = {
    // Convert String ID to Int index for Mllib
    model.userIdToIxMap.get(query.user).map { userIx =>
      val productIxToIdMap = model.productIdToIxMap.inverse
      // ALSModel returns product Int index. Convert it to String ID for
      // returning PredictedResult
      val productScores = model.recommendProducts(userIx, query.num)
        .map (r => ProductScore(productIxToIdMap(r.product), r.rating))
      new PredictedResult(productScores)
    }.getOrElse{
      logger.info(s"No prediction for unknown user ${query.user}.")
      new PredictedResult(Array.empty)
    }
  }

}

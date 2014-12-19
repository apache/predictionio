package io.prediction.examples.pfriendrecommendation

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine

case class Query(
  val item1: Long,
  val item2: Long
) extends Serializable

case class PredictedResult(
  val productScores: Array[ProductScore]
) extends Serializable

case class ProductScore(
  product: Int,
  score: Double
) extends Serializable

object PSimRankEngineFactory extends IEngineFactory {
  def apply() = {
    new Engine(
      /* No Sampling
      classOf[DataSource],
      */
      /* Node Sampling
      classOf[NodeSamplingDataSource],
      */
      classOf[ForestFireSamplingDataSource],
      classOf[IdentityPreparator],
      Map("simrank" -> classOf[SimRankAlgorithm]),
      classOf[Serving])
  }
}

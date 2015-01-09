package org.template.similar

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine

case class Query(
  val items: List[String],
  val num: Int,
  val categories: Option[Set[String]],
  val whiteList: Option[Set[String]],
  val blackList: Option[Set[String]]
) extends Serializable

case class PredictedResult(
  val itemScores: Array[ItemScore]
) extends Serializable

case class ItemScore(
  item: String,
  score: Double
) extends Serializable

object SimilarityEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("dimsum" -> classOf[DIMSUMAlgorithm]),
      classOf[Serving])
  }
}

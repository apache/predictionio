package com.test1

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine

case class Query(
 gender: String,
 age: Int,
 education: String
)

case class PredictedResult(
  label: Double
)

object ClassificationEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("randomforest" -> classOf[RandomForestAlgorithm]),
      classOf[Serving])
  }
}

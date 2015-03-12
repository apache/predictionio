package com.test1

import io.prediction.controller.IEngineFactory
import io.prediction.controller.Engine

class Query(
 val  gender: String,
 val  age: Int,
 val  education: String
) extends Serializable

class PredictedResult(
  val label: Double
) extends Serializable

object ClassificationEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("randomforest" -> classOf[RandomForestAlgorithm]),
      classOf[Serving])
  }
}

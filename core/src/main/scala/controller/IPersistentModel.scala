package io.prediction.controller

import org.apache.spark.SparkContext

trait IPersistentModel[AP <: Params, M] {
  def apply(id: String, params: AP, sc: SparkContext): M
}

package io.prediction.controller

import org.apache.spark.SparkContext

trait IPersistentModelLoader[AP <: Params, M] {
  def apply(id: String, params: AP, sc: SparkContext): M
}

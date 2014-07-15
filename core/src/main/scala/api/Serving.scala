package io.prediction.api

import io.prediction.core.BaseServing
import io.prediction.EmptyParams
import io.prediction.BaseParams
import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

// For depolyment, there should only be L serving class.
abstract class LServing[AP <: BaseParams : ClassTag, Q, P]
  extends BaseServing[AP, Q, P] {
  def serveBase(q: Q, ps: Seq[P]): P = {
    serve(q, ps)
  }

  def serve(query: Q, predictions: Seq[P]): P
}

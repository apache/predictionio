package io.prediction.api

import io.prediction.core.BaseServing
import io.prediction.api.EmptyParams
import io.prediction.api.Params
import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

// For depolyment, there should only be L serving class.
abstract class LServing[AP <: Params : ClassTag, Q, P]
  extends BaseServing[AP, Q, P] {
  def serveBase(q: Q, ps: Seq[P]): P = {
    serve(q, ps)
  }

  def serve(query: Q, predictions: Seq[P]): P
}

/****** Helpers ******/
// Return the first prediction.
class FirstServing[Q, P] extends LServing[EmptyParams, Q, P] {
  def serve(query: Q, predictions: Seq[P]): P = predictions.head
}

// Return the first prediction.
class AverageServing[Q] extends LServing[EmptyParams, Q, Double] {
  def serve(query: Q, predictions: Seq[Double]): Double = {
    predictions.sum / predictions.length
  }
}


package io.prediction.core

import io.prediction.BaseParams
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

abstract class BaseServing[SP <: BaseParams : ClassTag, Q, P]
  extends AbstractDoer[SP] {
  def serveBase(q: Q, ps: Seq[P]): P
}

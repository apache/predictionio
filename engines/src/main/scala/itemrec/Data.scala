package io.prediction.engines.itemrec

import io.prediction.engines.util

import com.github.nscala_time.time.Imports._

case class Query(
    val uid: String,
    val n: Int // number of items
    ) extends Serializable {
  override def toString = s"[${uid}, ${n}]"
}

// prediction output
case class Prediction(
  // the ranked iid with score
    val items: Seq[(String, Double)]
  ) extends Serializable {
  override def toString = s"${items}"
}

case class Actual(
    // actual items the user has performed actions on
    val iids: Seq[String]
  ) extends Serializable {
  override def toString = s"${iids}"
}

class MetricUnit(
  val score: Double
) extends Serializable

class MetricResult(
  val score: Double
) extends Serializable

class MultipleMetricResult(
  val score: Double
) extends Serializable

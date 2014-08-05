package io.prediction.examples.itemrank

import com.github.nscala_time.time.Imports._

class ItemTD(
  val iid: String,
  val itypes: Seq[String],
  val starttime: Option[Long],
  val endtime: Option[Long],
  val inactive: Boolean) extends Serializable {
    override def toString = s"${iid}"
  }

class UserTD(
  val uid: String
) extends Serializable {
  override def toString = s"${uid}"
}

class U2IActionTD(
  val uindex: Int,
  val iindex: Int,
  val action: String, // action name
  val v: Option[Int],
  val t: Long // action time
) extends Serializable {
  override def toString = s"${uindex} ${iindex} ${action}"
}

class TrainingData(
    val users: Map[Int, UserTD], // uindex->uid
    val items: Map[Int, ItemTD], // iindex->itemTD
    val u2iActions: Seq[U2IActionTD]
  ) extends Serializable {
    override def toString = s"TrainingData: ${u2iActions} ${users}"
  }

class RatingTD(
  val uindex: Int,
  val iindex: Int,
  val rating: Int,
  val t: Long) extends Serializable {
    override def toString = s"RatingTD: ${uindex} ${iindex} ${rating}"
  }

class PreparedData(
  val users: Map[Int, UserTD],
  val items: Map[Int, ItemTD],
  val rating: Seq[RatingTD]
) extends Serializable {
  override def toString = s"U: ${users} I: ${items} R: ${rating}"
}

class Query(
    val uid: String,
    val items: Seq[String] // items to be ranked
    ) extends Serializable {
  override def toString = s"[${uid}, ${items}]"
}

// prediction output
class Prediction(
  // the ranked items and score
    val items: Seq[(String, Double)]) extends Serializable {
  override def toString = s"${items}"
}

class Actual(
    // actual items the user has performed actions on
    val items: Seq[String]) extends Serializable {
  override def toString = s"${items}"
}

class MetricUnit(
  val q: Query,
  val p: Prediction,
  val a: Actual,
  val score: Double,
  val baseline: Double
) extends Serializable

class MetricResult(
  val testStartUntil: Tuple2[DateTime, DateTime],
  val baselineMean: Double,
  val baselineStdev: Double,
  val algoMean: Double,
  val algoStdev: Double
) extends Serializable

class MultipleMetricResult(
  val baselineMean: Double,
  val baselineStdev: Double,
  val algoMean: Double,
  val algoStdev: Double
) extends Serializable

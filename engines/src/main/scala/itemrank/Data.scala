package io.prediction.engines.itemrank

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
  val rating: Seq[RatingTD],
  val ratingOriginal: Seq[RatingTD]  // Non-deduped ratings
) extends Serializable {
  override def toString = s"U: ${users} I: ${items} R: ${rating}"
}

case class Query(
    val uid: String,
    val items: Seq[String] // items to be ranked
    ) extends Serializable {
  override def toString = s"[${uid}, ${items}]"
}

// prediction output
case class Prediction(
  // the ranked items and score
    val items: Seq[(String, Double)],
    val isOriginal: Boolean = false
  ) extends Serializable {
  override def toString = s"${items}"
}

case class Actual(
    // actual items the user has performed actions on
    val items: Seq[String],
    // other data that maybe used by metrics.
    val previousActionCount: Int = -1,
    val localDate: LocalDate = new LocalDate(0),
    val localDateTime: LocalDateTime = new LocalDateTime(0),
    val averageOrderSize: Double = -1,
    val previousOrders: Int = -1,
    val variety: Int = -1
  ) extends Serializable {
  override def toString = s"${items}"
}

class MetricUnit(
  val q: Query,
  val p: Prediction,
  val a: Actual,
  val score: Double,
  val baseline: Double,
  // a hashing function to bucketing uids
  val uidHash: Int = 0
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
) extends Serializable {
  override def toString = s"baselineMean=${baselineMean} " +
    s"baselineStdDev=${baselineStdev} " +
    s"algoMean=${algoMean} " +
    s"algoStdev=${algoStdev}"
}

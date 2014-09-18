package io.prediction.engines.base

/* this engine require following attributes */
case class AttributeNames(
  // entity types
  val user: String,
  val item: String,
  // event name of the u2i actions
  val u2iActions: Set[String],
  // properties
  val itypes: String,
  val starttime: String,
  val endtime: String,
  val inactive: String,
  val rating: String
)

/* Training Data */
class ItemTD(
  val iid: String,
  val itypes: Seq[String],
  val starttime: Option[Long] = None,
  val endtime: Option[Long] = None,
  val inactive: Boolean = false) extends Serializable {
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
  val v: Option[Int] = None,
  val t: Long // action time
) extends Serializable {
  override def toString = s"${uindex} ${iindex} ${action}"
}

class TrainingData(
    val users: Map[Int, UserTD], // uindex->uid
    val items: Map[Int, ItemTD], // iindex->itemTD
    val u2iActions: Seq[U2IActionTD]
  ) extends Serializable {
    override def toString = s"TrainingData:" +
      s"${users.take(2)}... ${items.take(2)}... ${u2iActions.take(2)}..."
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
  val ratingOriginal: Seq[RatingTD], // Non-deduped ratings
  val seenU2IActions: Option[Seq[U2IActionTD]] // actions for unseen filtering
) extends Serializable {
  override def toString = s"U: ${users.take(2)}..." +
   s" I: ${items.take(2)}... R: ${rating.take(2)}..."
}

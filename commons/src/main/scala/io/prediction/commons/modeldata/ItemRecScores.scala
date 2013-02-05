package io.prediction.commons.modeldata

import io.prediction.commons.settings.{Algo, App}

/** ItemRecScore object.
  * This object represents an item to be recommended to a user.
  *
  * @param uid User ID.
  * @param iid Item ID.
  * @param score Recommendation score.
  * @param itypes Item types of the item recommended. Copied from the item when a batch mode algorithm is run.
  * @param algoid Algo ID of this record.
  * @param modelset Model data set.
  */
case class ItemRecScore(
  uid: String,
  iid: String,
  score: Double,
  itypes: List[String],
  appid: Int,
  algoid: Int,
  modelset: Boolean
)

/** Base trait for implementations that interact with itemrec scores in the backend data store. */
trait ItemRecScores {
  /** Insert an ItemSimScore. */
  def insert(itemRecScore: ItemRecScore): Unit

  /** Get the top N ItemSimScore ranked by score in descending order. */
  def getTopN(uid: String, n: Int, itypes: Option[List[String]])(implicit app: App, algo: Algo): Iterator[ItemRecScore]
}

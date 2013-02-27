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
  * @param id ItemRecScore ID (optional field used internally for sorting)
  */
case class ItemRecScore(
  uid: String,
  iid: String,
  score: Double,
  itypes: List[String],
  appid: Int,
  algoid: Int,
  modelset: Boolean,
  id: Option[Any] = None
)

/** Base trait for implementations that interact with itemrec scores in the backend data store. */
trait ItemRecScores {
  /** Insert an ItemSimScore and return it with a real ID, if any (database vendor dependent). */
  def insert(itemRecScore: ItemRecScore): ItemRecScore

  /** Get the top N ItemSimScore ranked by score in descending order.
    *
    * @param after Returns the next top N results after the provided ItemSimScore, if provided.
    */
  def getTopN(uid: String, n: Int, itypes: Option[List[String]], after: Option[ItemRecScore])(implicit app: App, algo: Algo): Iterator[ItemRecScore]

  /** Delete by Algo ID. */
  def deleteByAlgoid(algoid: Int)

  /** Delete by Algo ID and model set. */
  def deleteByAlgoidAndModelset(algoid: Int, modelset: Boolean)
}

package io.prediction.commons.modeldata

import io.prediction.commons.settings.{ Algo, App, OfflineEval }

/**
 * ItemRecScore object.
 * This object represents an item to be recommended to a user.
 *
 * @param uid User ID.
 * @param iid Item ID.
 * @param score Recommendation score.
 * @param itypes Item types of the item recommended. Copied from the item when a batch mode algorithm is run.
 * @param appid App ID of this record.
 * @param algoid Algo ID of this record.
 * @param modelset Model data set.
 * @param id ItemRecScore ID (optional field used internally for sorting)
 */
case class ItemRecScore(
  uid: String,
  iid: String,
  score: Double,
  itypes: Seq[String],
  appid: Int,
  algoid: Int,
  modelset: Boolean,
  id: Option[Any] = None)

/** Base trait for implementations that interact with itemrec scores in the backend data store. */
trait ItemRecScores {
  /** Insert an ItemRecScore and return it with a real ID, if any (database vendor dependent). */
  def insert(itemRecScore: ItemRecScore): ItemRecScore

  /**
   * Get the top N ItemRecScore ranked by score in descending order.
   *
   * @param after Returns the next top N results after the provided ItemRecScore, if provided.
   */
  def getTopN(uid: String, n: Int, itypes: Option[Seq[String]], after: Option[ItemRecScore])(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval] = None): Iterator[ItemRecScore]

  /** Delete by Algo ID. */
  def deleteByAlgoid(algoid: Int)

  /** Delete by Algo ID and model set. */
  def deleteByAlgoidAndModelset(algoid: Int, modelset: Boolean)

  /** Check whether data exist for a given Algo. */
  def existByAlgo(algo: Algo): Boolean
}

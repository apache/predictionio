package io.prediction.commons.modeldata

import io.prediction.commons.settings.{ Algo, App, OfflineEval }

/**
 * ItemRecScore object.
 * This object represents an item to be recommended to a user.
 *
 * @param uid User ID.
 * @param iids Seq of item IDs.
 * @param scores Seq of recommendation score.
 * @param itypes Seq of item types of the item recommended. Copied from the item when a batch mode algorithm is run.
 * @param appid App ID of this record.
 * @param algoid Algo ID of this record.
 * @param modelset Model data set.
 * @param id ItemRecScore ID (optional field used internally for sorting)
 */
case class ItemRecScore(
  uid: String,
  iids: Seq[String],
  scores: Seq[Double],
  itypes: Seq[Seq[String]],
  appid: Int,
  algoid: Int,
  modelset: Boolean,
  id: Option[Any] = None)

/** Base trait for implementations that interact with itemrec scores in the backend data store. */
trait ItemRecScores extends ModelData {
  /** Insert an ItemRecScore and return it with a real ID, if any (database vendor dependent). */
  def insert(itemRecScore: ItemRecScore): ItemRecScore

  /** get an ItemRecScore by uid */
  def getByUid(uid: String)(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval] = None): Option[ItemRecScore]

  /**
   * Get the top N ranked iids.
   * @param n If n == 0, return as many iids as available
   */
  def getTopNIids(uid: String, n: Int, itypes: Option[Seq[String]])(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval] = None): Iterator[String]

  /** Delete by Algo ID. */
  def deleteByAlgoid(algoid: Int)

  /** Delete by Algo ID and model set. */
  def deleteByAlgoidAndModelset(algoid: Int, modelset: Boolean)

  /** Check whether data exist for a given Algo. */
  def existByAlgo(algo: Algo): Boolean
}

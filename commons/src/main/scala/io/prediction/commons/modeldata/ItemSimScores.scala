package io.prediction.commons.modeldata

import io.prediction.commons.settings.{ Algo, App, OfflineEval }

/**
 * ItemSimScore object.
 * This object represents an item that is similar to an item.
 *
 * @param iid Item ID.
 * @param simiids Seq of similar item ID.
 * @param scores Seq of similarity score.
 * @param itypes Seq of item types of the similar item. Copied from the item when a batch mode algorithm is run.
 * @param appid App ID of this record.
 * @param algoid Algo ID of this record.
 * @param modelset Model data set.
 * @param id ItemSimScore ID (optional field used internally for sorting)
 */
case class ItemSimScore(
  iid: String,
  simiids: Seq[String],
  scores: Seq[Double],
  itypes: Seq[Seq[String]],
  appid: Int,
  algoid: Int,
  modelset: Boolean,
  id: Option[Any] = None)

/** Base trait for implementations that interact with itemsim scores in the backend data store. */
trait ItemSimScores extends ModelData {
  /** Insert an ItemSimScore and return it with a real ID, if any (database vendor dependent). */
  def insert(itemSimScore: ItemSimScore): ItemSimScore

  /** get an ItemSimScore by iid */
  def getByIid(iid: String)(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval] = None): Option[ItemSimScore]

  /**
   * Get the top N ranked iids
   * @param n if n == 0, return iids as many as avaiable
   */
  def getTopNIids(iid: String, n: Int, itypes: Option[Seq[String]])(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval] = None): Iterator[String]

  /** Delete by Algo ID. */
  def deleteByAlgoid(algoid: Int)

  /** Delete by Algo ID and model set. */
  def deleteByAlgoidAndModelset(algoid: Int, modelset: Boolean)

  /** Check whether data exist for a given Algo. */
  def existByAlgo(algo: Algo): Boolean
}

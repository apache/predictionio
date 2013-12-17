package io.prediction.commons.modeldata

import io.prediction.commons.settings.{ Algo, App, OfflineEval }

/**
 * ItemSimScore object.
 * This object represents an item that is similar to an item.
 *
 * @param iid Item ID.
 * @param simiid Similar item ID.
 * @param score Similarity score.
 * @param itypes Item types of the similar item. Copied from the item when a batch mode algorithm is run.
 * @param appid App ID of this record.
 * @param algoid Algo ID of this record.
 * @param modelset Model data set.
 * @param id ItemSimScore ID (optional field used internally for sorting)
 */
case class ItemSimScore(
  iid: String,
  simiid: String,
  score: Double,
  itypes: Seq[String],
  appid: Int,
  algoid: Int,
  modelset: Boolean,
  id: Option[Any] = None)

/** Base trait for implementations that interact with itemsim scores in the backend data store. */
trait ItemSimScores {
  /** Insert an ItemSimScore and return it with a real ID, if any (database vendor dependent). */
  def insert(itemSimScore: ItemSimScore): ItemSimScore

  /**
   * Get the top N ItemSimScore ranked by score in descending order.
   *
   * @param after Returns the next top N results after the provided ItemSimScore, if provided.
   */
  def getTopN(iid: String, n: Int, itypes: Option[Seq[String]], after: Option[ItemSimScore])(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval] = None): Iterator[ItemSimScore]

  /** Delete by Algo ID. */
  def deleteByAlgoid(algoid: Int)

  /** Delete by Algo ID and model set. */
  def deleteByAlgoidAndModelset(algoid: Int, modelset: Boolean)

  /** Check whether data exist for a given Algo. */
  def existByAlgo(algo: Algo): Boolean
}

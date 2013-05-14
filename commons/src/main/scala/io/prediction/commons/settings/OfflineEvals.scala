package io.prediction.commons.settings

import com.github.nscala_time.time.Imports._

/** OfflineEval object
 *
 * @param id ID.
 * @param engineid The id of Engine object which owns this OfflineEval.
 * @param name The name of the this Offline Evaluation.
 * @param tuneid The OfflineTune ID
 * @param createtime The Creation time of the evaluation
 * @param starttime The Starting time of the evaluation
 * @param endtime The End time of the the evaluation. It's still running if it's None.
 * @param iterations Number of iterations. Default to 1.
 */
case class OfflineEval(
  id: Int,
  engineid: Int,
  name: String,
  iterations: Int = 1,
  tuneid: Option[Int],
  createtime: Option[DateTime],
  starttime: Option[DateTime],
  endtime: Option[DateTime]
)

trait OfflineEvals {

  /** Insert an OfflineEval and return id
   *
   * NOTE: can't use id of the offlineEval parameter
   */
  def insert(offlineEval: OfflineEval): Int

  /** Get OfflineEval by its id */
  def get(id: Int): Option[OfflineEval]

  /** Get OfflineEval by engine id */
  def getByEngineid(engineid: Int): Iterator[OfflineEval]

  /** Get OfflineEval by offline tune id */
  def getByTuneid(tuneid: Int): Iterator[OfflineEval]

  /** Update OfflineEval (create new one if the it doesn't exist) */
  def update(offlineEval: OfflineEval)

  /** delete OfflineEval by it's id) */
  def delete(id: Int)

}

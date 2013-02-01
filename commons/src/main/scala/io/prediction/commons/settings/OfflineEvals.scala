package io.prediction.commons.settings

import org.scala_tools.time.Imports._

/** OfflineEval object
 *
 * @param id ID.
 * @param engineid The id of Engine object which owns this OfflineEval.
 * @param name The name of the this Offline Evaluation.
 * @param trainingsize The percentage of training set (1 - 10)
 * @param testsize The percentage of test set (1 - 10). NOTE: trainingsize + testsize must be <= 10
 * @param timeorder Enable flag of random in time order. Allowed only if trainingsize + testsize < 10
 * @param starttime The Starting time of the evaluation
 * @param endtime The End time of the the evaluation. It's still running if it's None.
 */
case class OfflineEval(
  id: Int,
  engineid: Int,
  name: String,
  trainingsize: Int,
  testsize: Int,
  timeorder: Boolean,
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

  /** Update OfflineEval (create new one if the it doesn't exist) */
  def update(offlineEval: OfflineEval)

  /** delete OfflineEval by it's id) */
  def delete(id: Int)

}

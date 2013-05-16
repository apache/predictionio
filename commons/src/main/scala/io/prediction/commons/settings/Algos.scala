package io.prediction.commons.settings

import com.github.nscala_time.time.Imports._

/** Algo object.
  *
  * @param id ID.
  * @param engineid App ID that owns this engine.
  * @param name Algo name.
  * @param infoid AlgoInfo ID
  * @param command Command template for running the algo.
  * @param params Algo parameters as key-value pairs.
  * @param settings Algo settings as key-value pairs.
  * @param modelset Indicates which model output set to be used by the API.
  * @param status The status of the algo. eg "ready", "tuning".
  * @param offlineevalid The id of OfflineEval which uses this algo for offline evaluation
  * @param offlinetuneid The id of OfflineTune
  * @param loop The iteration number used by auto tune. (NOTE: loop=0 reserved for baseline algo)
  * @param paramset The param generation set number
  */
case class Algo(
  id: Int,
  engineid: Int,
  name: String,
  infoid: String,
  command: String,
  params: Map[String, Any],
  settings: Map[String, Any],
  modelset: Boolean,
  createtime: DateTime,
  updatetime: DateTime,
  status: String = "",
  offlineevalid: Option[Int],
  offlinetuneid: Option[Int] = None,
  loop: Option[Int] = None,
  paramset: Option[Int] = None
)

/** Base trait for implementations that interact with algos in the backend data store. */
trait Algos {
  /** Inserts an algo. */
  def insert(algo: Algo): Int

  /** Get an algo by its ID. */
  def get(id: Int): Option[Algo]

  /** Get algos by engine ID. */
  def getByEngineid(engineid: Int): Iterator[Algo]

  /** Get deployed algos by engine ID. */
  def getDeployedByEngineid(engineid: Int): Iterator[Algo]

  /** Get by OfflineEval ID. */
  def getByOfflineEvalid(evalid: Int, loop: Option[Int] = None, paramset: Option[Int] = None): Iterator[Algo]

  /** Get the auto tune subject by OfflineTune ID. */
  def getTuneSubjectByOfflineTuneid(tuneid: Int): Option[Algo]

  /** Update an algo. */
  def update(algo: Algo)

  /** Delete an algo by its ID. */
  def delete(id: Int)

  /** Check existence of an algo by its engine ID and name.
    * Algos that are part of an offline evaluation or tuning are not counted.
    */
  def existsByEngineidAndName(engineid: Int, name: String): Boolean
}

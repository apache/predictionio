package io.prediction.commons.settings

import com.github.nscala_time.time.Imports._

/** Algo object.
  *
  * @param id ID.
  * @param engineid App ID that owns this engine.
  * @param name Algo name.
  * @param infoid AlgoInfo ID
  * @param deployed Indicates whether the algo is deployed and alive.
  * @param command Command template for running the algo.
  * @param params Algo parameters as key-value pairs.
  * @param settings Algo settings as key-value pairs.
  * @param modelset Indicates which model output set to be used by the API.
  * @param evalid The id of OfflineEval which uses this algo for offline evaluation
  */
case class Algo(
  id: Int,
  engineid: Int,
  name: String,
  infoid: String,
  deployed: Boolean,
  command: String,
  params: Map[String, Any],
  settings: Map[String, Any],
  modelset: Boolean,
  createtime: DateTime,
  updatetime: DateTime,
  offlineevalid: Option[Int]
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

  /** Get by OfflineEval ID */
  def getByOfflineEvalid(evalid: Int): Iterator[Algo]

  /** Update an algo. */
  def update(algo: Algo)

  /** Delete an algo by its ID. */
  def delete(id: Int)

  /** Check existence of an algo by its engine ID and name. */
  def existsByEngineidAndName(engineid: Int, name: String): Boolean
}

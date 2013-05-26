package io.prediction.commons.settings

/** AlgoInfo object.
  *
  * @param id Unique identifier. Usually identical to the algorithm's namespace.
  * @param name Algorithm name.
  * @param description A long description of the algorithm.
  * @param batchcommands Command templates for running the algorithm in batch mode.
  * @param offlineevalcommands Command templates for running the algorithm in offline evaluation mode.
  * @param params Map of Param objects, with keys equal to IDs of Param objects it contains.
  * @param paramorder The display order of parameters.
  * @param engineinfoid The EngineInfo ID of the engine that can run this algorithm.
  * @param techreq Technology requirement for this algorithm to run.
  * @param datareq Data requirement for this algorithm to run.
  */
case class AlgoInfo(
  id: String,
  name: String,
  description: Option[String],
  batchcommands: Option[Seq[String]],
  offlineevalcommands: Option[Seq[String]],
  params: Map[String, Param],
  paramorder: Seq[String],
  engineinfoid: String,
  techreq: Seq[String],
  datareq: Seq[String]
)

/** Base trait for implementations that interact with algo info in the backend data store. */
trait AlgoInfos {
  /** Insert an algo info. */
  def insert(algoInfo: AlgoInfo): Unit

  /** Get algo info by its ID. */
  def get(id: String): Option[AlgoInfo]

  /** Get algo info by their engine type. */
  def getByEngineInfoId(engineinfoid: String): Seq[AlgoInfo]

  /** Update an algo info. */
  def update(algoInfo: AlgoInfo): Unit

  /** Delete an algo info by its ID. */
  def delete(id: String): Unit
}

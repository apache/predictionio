package io.prediction.commons.settings

/** AlgoInfo object.
  *
  * @param id Unique identifier. Usually identical to the algorithm's namespace.
  * @param name Algorithm name.
  * @param description A long description of the algorithm.
  * @param batchcommands Command templates for running the algorithm in batch mode.
  * @param offlineevalcommands Command templates for running the algorithm in offline evaluation mode.
  * @param defaultparams Default parameters as key-value pairs. Usually used by substituting template variables in command templates.
  * @param enginetype The engine type associated to this algorithm.
  * @param datareq Data requirement for this algorithm to run.
  */
case class AlgoInfo(
  id: String,
  name: String,
  pkgname: String,
  description: Option[String],
  batchcommands: Option[Seq[String]],
  offlineevalcommands: Option[Seq[String]],
  defaultparams: Seq[((String, String, Boolean), Any)], // ((display name, param name. visible), default value)
  enginetype: String,
  techreq: Seq[String],
  datareq: Seq[String]
)

/** Base trait for implementations that interact with algo info in the backend data store. */
trait AlgoInfos {
  /** Get algo info by its ID. */
  def get(id: String): Option[AlgoInfo]

  /** Get algo info by their engine type. */
  def getByEngineType(enginetype: String): Seq[AlgoInfo]
}

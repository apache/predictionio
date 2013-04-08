package io.prediction.commons.settings

/** EngineInfo object.
  *
  * @param id Unique identifier of an engine type.
  * @param name Engine name.
  * @param description A long description of the engine.
  * @param defaultsettings Default engine settings.
  * @param defaultalgoinfoid Default AlgoInfo ID for this engine.
  */
case class EngineInfo(
  id: String,
  name: String,
  description: Option[String],
  defaultsettings: Map[String, Any],
  defaultalgoinfoid: String
)

/** Base trait for implementations that interact with engine info in the backend data store. */
trait EngineInfos {
  /** Inserts an engine info. */
  def insert(engineInfo: EngineInfo): Unit

  /** Get an engine info by its ID. */
  def get(id: String): Option[EngineInfo]

  /** Updates an engine info. */
  def update(engineInfo: EngineInfo): Unit

  /** Delete an engine info by its ID. */
  def delete(id: String): Unit
}

package io.prediction.commons.settings

import io.prediction.commons.Common

import com.twitter.chill.KryoInjection

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
trait EngineInfos extends Common {
  /** Inserts an engine info. */
  def insert(engineInfo: EngineInfo): Unit

  /** Get an engine info by its ID. */
  def get(id: String): Option[EngineInfo]

  /** Get all engine info. */
  def getAll(): Seq[EngineInfo]

  /** Updates an engine info. */
  def update(engineInfo: EngineInfo, upsert: Boolean = false): Unit

  /** Delete an engine info by its ID. */
  def delete(id: String): Unit

  /** Backup all EngineInfos as a byte array. */
  def backup(): Array[Byte] = {
    val engineinfos = getAll().map { engineinfo =>
      Map(
        "id" -> engineinfo.id,
        "name" -> engineinfo.name,
        "description" -> engineinfo.description,
        "defaultsettings" -> engineinfo.defaultsettings,
        "defaultalgoinfoid" -> engineinfo.defaultalgoinfoid)
    }
    KryoInjection(engineinfos)
  }

  /** Restore EngineInfos from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[EngineInfo]] = {
    KryoInjection.invert(bytes) map { r =>
      val rdata = r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        EngineInfo(
          id = data("id").asInstanceOf[String],
          name = data("name").asInstanceOf[String],
          description = data("description").asInstanceOf[Option[String]],
          defaultsettings = data("defaultsettings").asInstanceOf[Map[String, Any]],
          defaultalgoinfoid = data("defaultalgoinfoid").asInstanceOf[String])
      }

      if (inplace) rdata foreach { update(_, true) }

      rdata
    }
  }
}

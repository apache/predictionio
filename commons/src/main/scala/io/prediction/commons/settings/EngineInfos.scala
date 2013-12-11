package io.prediction.commons.settings

import io.prediction.commons.Common

import org.json4s._
import org.json4s.native.Serialization

/**
 * EngineInfo object.
 *
 * @param id Unique identifier of an engine type.
 * @param name Engine name.
 * @param description A long description of the engine.
 * @param defaultsettings Default engine settings.
 * @param defaultalgoinfoid Default AlgoInfo ID for this engine.
 * @param defaultofflineevalmetricinfoid Default OfflineEvalMetricInfo ID for this engine.
 * @param defaultofflineevalsplitterinfoid Default OfflineEvalSplitter ID for this engine.
 */
case class EngineInfo(
  id: String,
  name: String,
  description: Option[String],
  params: Map[String, Param],
  paramsections: Seq[ParamSection],
  defaultalgoinfoid: String,
  defaultofflineevalmetricinfoid: String,
  defaultofflineevalsplitterinfoid: String) extends Info

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

  implicit val formats = Serialization.formats(NoTypeHints) + new ParamSerializer

  /** Backup all EngineInfos as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll()).getBytes("UTF-8")

  /** Restore EngineInfos from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[EngineInfo]] = {
    try {
      val rdata = Serialization.read[Seq[EngineInfo]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { update(_, true) }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}

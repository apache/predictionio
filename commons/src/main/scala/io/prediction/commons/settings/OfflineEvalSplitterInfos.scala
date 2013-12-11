package io.prediction.commons.settings

import io.prediction.commons.Common

import org.json4s._
import org.json4s.native.Serialization

/**
 * OfflineEvalSplitterInfo object.
 *
 * @param id Unique identifier of a splitter.
 * @param name Splitter name.
 * @param description A long description of the splitter.
 * @param engineinfoids A list of EngineInfo IDs that this splitter can apply to.
 * @param commands A sequence of commands to run this metric.
 * @param params Map of Param objects, with keys equal to IDs of Param objects it contains.
 * @param paramsections Seq of ParamSection objects
 * @param paramorder The display order of parameters.
 */
case class OfflineEvalSplitterInfo(
  id: String,
  name: String,
  description: Option[String],
  engineinfoids: Seq[String],
  commands: Option[Seq[String]],
  params: Map[String, Param],
  paramsections: Seq[ParamSection],
  paramorder: Seq[String]) extends Info

/** Base trait for implementations that interact with metric info in the backend data store. */
trait OfflineEvalSplitterInfos extends Common {
  /** Inserts a splitter info. */
  def insert(offlineEvalSplitterInfo: OfflineEvalSplitterInfo): Unit

  /** Get a splitter info by its ID. */
  def get(id: String): Option[OfflineEvalSplitterInfo]

  /** Get all splitter info. */
  def getAll(): Seq[OfflineEvalSplitterInfo]

  /** Get all splitter info by engineinfo ID */
  def getByEngineinfoid(engineinfoid: String): Seq[OfflineEvalSplitterInfo]

  /** Updates a splitter info. */
  def update(offlineEvalSplitterInfo: OfflineEvalSplitterInfo, upsert: Boolean = false): Unit

  /** Delete a splitter info by its ID. */
  def delete(id: String): Unit

  implicit val formats = Serialization.formats(NoTypeHints) + new ParamSerializer

  /** Backup all data as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll()).getBytes("UTF-8")

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[OfflineEvalSplitterInfo]] = {
    try {
      val rdata = Serialization.read[Seq[OfflineEvalSplitterInfo]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { update(_, true) }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}

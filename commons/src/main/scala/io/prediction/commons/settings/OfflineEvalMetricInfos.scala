package io.prediction.commons.settings

import io.prediction.commons.Common

import org.json4s._
import org.json4s.native.Serialization

/**
 * OfflineEvalMetricInfo object.
 *
 * @param id Unique identifier of a metric.
 * @param name Metric name.
 * @param description A long description of the metric.
 * @param engineinfoids A list of EngineInfo IDs that this metric can apply to.
 * @param commands A sequence of commands to run this metric.
 * @param paramdefaults Default parameters as key-value pairs. Usually used by substituting template variables in command templates.
 * @param paramnames Key value pairs of (parameter -> display name).
 * @param paramdescription Key value pairs of (parameter -> description).
 * @param paramorder The display order of parameters.
 */
case class OfflineEvalMetricInfo(
  id: String,
  name: String,
  description: Option[String],
  engineinfoids: Seq[String],
  commands: Option[Seq[String]],
  params: Map[String, Param],
  paramsections: Seq[ParamSection],
  paramorder: Seq[String]) extends Info

/** Base trait for implementations that interact with metric info in the backend data store. */
trait OfflineEvalMetricInfos extends Common {
  /** Inserts a metric info. */
  def insert(metricInfo: OfflineEvalMetricInfo): Unit

  /** Get a metric info by its ID. */
  def get(id: String): Option[OfflineEvalMetricInfo]

  /** Get all metric info. */
  def getAll(): Seq[OfflineEvalMetricInfo]

  /** Get all metric info by engineinfo ID */
  def getByEngineinfoid(engineinfoid: String): Seq[OfflineEvalMetricInfo]

  /** Updates a metric info. */
  def update(metricInfo: OfflineEvalMetricInfo, upsert: Boolean = false): Unit

  /** Delete a metric info by its ID. */
  def delete(id: String): Unit

  implicit val formats = Serialization.formats(NoTypeHints) + new ParamSerializer

  /** Backup all OfflineEvalMetricInfos as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll()).getBytes("UTF-8")

  /** Restore OfflineEvalMetricInfos from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[OfflineEvalMetricInfo]] = {
    try {
      val rdata = Serialization.read[Seq[OfflineEvalMetricInfo]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { update(_, true) }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}

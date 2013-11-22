package io.prediction.commons.settings

import io.prediction.commons.Common

import com.twitter.chill.KryoInjection

/** OfflineEvalMetricInfo object.
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
  paramdefaults: Map[String, Any],
  paramnames: Map[String, String],
  paramdescription: Map[String, String],
  paramorder: Seq[String]
)

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

  /** Backup all OfflineEvalMetricInfos as a byte array. */
  def backup(): Array[Byte] = {
    val metricinfos = getAll().map { metricinfo =>
      Map(
        "id" -> metricinfo.id,
        "name" -> metricinfo.name,
        "description" -> metricinfo.description,
        "engineinfoids" -> metricinfo.engineinfoids,
        "commands" -> metricinfo.commands,
        "paramdefaults" -> metricinfo.paramdefaults,
        "paramnames" -> metricinfo.paramnames,
        "paramdescription" -> metricinfo.paramdescription,
        "paramorder" -> metricinfo.paramorder)
    }
    KryoInjection(metricinfos)
  }

  /** Restore OfflineEvalMetricInfos from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[OfflineEvalMetricInfo]] = {
    KryoInjection.invert(bytes) map { r =>
      val rdata = r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        OfflineEvalMetricInfo(
          id = data("id").asInstanceOf[String],
          name = data("name").asInstanceOf[String],
          description = data("description").asInstanceOf[Option[String]],
          engineinfoids = data("engineinfoids").asInstanceOf[Seq[String]],
          commands = data("commands").asInstanceOf[Option[Seq[String]]],
          paramdefaults = data("paramdefaults").asInstanceOf[Map[String, Any]],
          paramnames = data("paramnames").asInstanceOf[Map[String, String]],
          paramdescription = data("paramdescription").asInstanceOf[Map[String, String]],
          paramorder = data("paramorder").asInstanceOf[Seq[String]])
      }

      if (inplace) rdata foreach { update(_, true) }

      rdata
    }
  }
}

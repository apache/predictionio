package io.prediction.commons.settings

/** OfflineEvalMetricInfo object.
  *
  * @param id Unique identifier of a metric.
  * @param name Metric name.
  * @param description A long description of the metric.
  * @param engineinfoids A list of EngineInfo IDs that this metric can apply to.
  * @param commands A sequence of commands to run this metric.
  * @param paramdefaults Default parameters as key-value pairs. Usually used by substituting template variables in command templates.
  * @param paramnames Key value paris of (parameter -> display name).
  * @param paramdescription Key value paris of (parameter -> description).
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
trait OfflineEvalMetricInfos {
  /** Inserts an metric info. */
  def insert(metricInfo: OfflineEvalMetricInfo): Unit

  /** Get an metric info by its ID. */
  def get(id: String): Option[OfflineEvalMetricInfo]

  /** Updates an metric info. */
  def update(metricInfo: OfflineEvalMetricInfo): Unit

  /** Delete an metric info by its ID. */
  def delete(id: String): Unit
}

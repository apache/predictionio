package io.prediction.commons.settings

/** MetricInfo object.
  *
  * @param id Unique identifier of a metric.
  * @param name Metric name.
  * @param description A long description of the metric.
  * @param metricinfoids A list of MetricInfo IDs that this metric can apply to.
  * @param commands A sequence of commands to run this metric.
  * @param paramdefaults Default parameters as key-value pairs. Usually used by substituting template variables in command templates.
  * @param paramdescription Key value paris of (parameter -> display name).
  * @param paramdescription Key value paris of (parameter -> description).
  * @param paramorder The display order of parameters.
  */
case class MetricInfo(
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
trait MetricInfos {
  /** Inserts an metric info. */
  def insert(metricInfo: MetricInfo): Unit

  /** Get an metric info by its ID. */
  def get(id: String): Option[MetricInfo]

  /** Updates an metric info. */
  def update(metricInfo: MetricInfo): Unit

  /** Delete an metric info by its ID. */
  def delete(id: String): Unit
}

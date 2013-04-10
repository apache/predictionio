package io.prediction.commons.settings

/** OfflineEvalMetric Object
 *
 * @param id ID
 * @param infoid MetricInfo ID
 * @param evalid ID of the OfflineEval which uses this metric
 * @param params Metric parameters as key-value pairs
 */
case class OfflineEvalMetric(
  id: Int,
  infoid: String,
  evalid: Int,
  params: Map[String, Any]
)

trait OfflineEvalMetrics {

  /** Insert a metric and return ID */
  def insert(metric: OfflineEvalMetric): Int

  /** Get a metric by its ID */
  def get(id: Int): Option[OfflineEvalMetric]

  /** Get metrics by OfflineEval ID */
  def getByEvalid(evalid: Int): Iterator[OfflineEvalMetric]

  /** Update metric */
  def update(metric: OfflineEvalMetric)

  /** Delete metric by its ID */
  def delete(id: Int)

}

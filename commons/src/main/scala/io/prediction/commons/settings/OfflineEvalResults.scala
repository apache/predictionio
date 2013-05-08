package io.prediction.commons.settings

/** OfflineEvalResult Object
 *
 * @param evalid ID of the OfflineEval
 * @param metricid ID of the metric
 * @param algoid ID of the algo
 * @param score The offline evaluation score
 * @param iteration The iteration number
 * @param splitset The name of the set used as test data (eg. "test", "validation")
 */
case class OfflineEvalResult(
  evalid: Int,
  metricid: Int,
  algoid: Int,
  score: Double,
  iteration: Int,
  splitset: String = ""
)

trait OfflineEvalResults {

  /** save(update existing or create a new one) a OfflineEvalResult and return id */
  def save(result: OfflineEvalResult): String

  /** Get a result by its OfflineEval ID, OfflineEvalMetric ID, and Algo ID. */
  def getByEvalidAndMetricidAndAlgoid(evalid: Int, metricid: Int, algoid: Int): Iterator[OfflineEvalResult]

  /** get results by OfflineEval ID */
  def getByEvalid(evalid: Int): Iterator[OfflineEvalResult]

  /** delete all results with this OfflineEval ID */
  def deleteByEvalid(evalid: Int)

}
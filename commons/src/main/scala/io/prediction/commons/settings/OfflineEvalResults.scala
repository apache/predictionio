package io.prediction.commons.settings

/** OfflineEvalResult Object
 * 
 * @param id ID. it's a String concatenated by evalid_metricid_algoid
 * @param evalid ID of the OfflineEval
 * @param metricid ID of the metric
 * @param algoid ID of the algo
 * @param score The offline evaluation score
 */
case class OfflineEvalResult(
  id: String,
  evalid: Int,
  metricid: Int,
  algoid: Int,
  score: Double
)

trait OfflineEvalResults {
  
  /** save(update existing or create a new one) a OfflineEvalResult and return id */
  def save(result: OfflineEvalResult): String
  
  /** get results by OfflineEval ID */
  def getByEvalid(evalid: Int): Iterator[OfflineEvalResult]
  
  /** delete all results with this OfflineEval ID */
  def deleteByEvalid(evalid: Int)
  
}
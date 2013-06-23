package io.prediction.commons.settings

import com.twitter.chill.KryoInjection

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

  /** Get all results. */
  def getAll(): Iterator[OfflineEvalResult]

  /** Get a result by its OfflineEval ID, OfflineEvalMetric ID, and Algo ID. */
  def getByEvalidAndMetricidAndAlgoid(evalid: Int, metricid: Int, algoid: Int): Iterator[OfflineEvalResult]

  /** get results by OfflineEval ID */
  def getByEvalid(evalid: Int): Iterator[OfflineEvalResult]

  /** delete all results with this OfflineEval ID */
  def deleteByEvalid(evalid: Int)

  /** Backup all OfflineEvalResults as a byte array. */
  def backup(): Array[Byte] = {
    val results = getAll().toSeq.map { result =>
      Map(
        "evalid" -> result.evalid,
        "metricid" -> result.metricid,
        "algoid" -> result.algoid,
        "score" -> result.score,
        "iteration" -> result.iteration,
        "splitset" -> result.splitset)
    }
    KryoInjection(results)
  }

  /** Restore OfflineEvalResults from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], upgrade: Boolean = false): Option[Seq[OfflineEvalResult]] = {
    KryoInjection.invert(bytes) map { r =>
      r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        OfflineEvalResult(
          evalid = data("evalid").asInstanceOf[Int],
          metricid = data("metricid").asInstanceOf[Int],
          algoid = data("algoid").asInstanceOf[Int],
          score = data("score").asInstanceOf[Double],
          iteration = data("iteration").asInstanceOf[Int],
          splitset = data("splitset").asInstanceOf[String])
      }
    }
  }
}

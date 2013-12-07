package io.prediction.commons.settings

import io.prediction.commons.Common

import org.json4s._
import org.json4s.native.Serialization

/**
 * OfflineEvalResult Object
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
  splitset: String = "")

trait OfflineEvalResults extends Common {

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

  implicit val formats = Serialization.formats(NoTypeHints)

  /** Backup all OfflineEvalResults as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll().toSeq).getBytes("UTF-8")

  /** Restore OfflineEvalResults from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[OfflineEvalResult]] = {
    try {
      val rdata = Serialization.read[Seq[OfflineEvalResult]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { save(_) }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}

package io.prediction.commons.settings

import io.prediction.commons.Common

import com.twitter.chill.KryoInjection

/**
 * OfflineEvalMetric Object
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
  params: Map[String, Any])

trait OfflineEvalMetrics extends Common {

  /** Insert a metric and return ID. */
  def insert(metric: OfflineEvalMetric): Int

  /** Get a metric by its ID. */
  def get(id: Int): Option[OfflineEvalMetric]

  /** Get all metrics. */
  def getAll(): Iterator[OfflineEvalMetric]

  /** Get metrics by OfflineEval ID. */
  def getByEvalid(evalid: Int): Iterator[OfflineEvalMetric]

  /** Update metric. */
  def update(metric: OfflineEvalMetric, upsert: Boolean = false)

  /** Delete metric by its ID. */
  def delete(id: Int)

  /** Backup all OfflineEvalMetrics as a byte array. */
  def backup(): Array[Byte] = {
    val metrics = getAll().toSeq.map { metric =>
      Map(
        "id" -> metric.id,
        "infoid" -> metric.infoid,
        "evalid" -> metric.evalid,
        "params" -> metric.params)
    }
    KryoInjection(metrics)
  }

  /** Restore OfflineEvalMetrics from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[OfflineEvalMetric]] = {
    KryoInjection.invert(bytes) map { r =>
      val rdata = r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        OfflineEvalMetric(
          id = data("id").asInstanceOf[Int],
          infoid = data("infoid").asInstanceOf[String],
          evalid = data("evalid").asInstanceOf[Int],
          params = data("params").asInstanceOf[Map[String, Any]])
      }

      if (inplace) rdata foreach { update(_, true) }

      rdata
    }
  }
}

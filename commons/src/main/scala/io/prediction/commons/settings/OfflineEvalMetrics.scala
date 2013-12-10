package io.prediction.commons.settings

import io.prediction.commons.Common

import org.json4s._
import org.json4s.native.Serialization

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

  implicit val formats = Serialization.formats(NoTypeHints) + new OfflineEvalMetricSerializer

  /** Backup all OfflineEvalMetrics as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll().toSeq).getBytes("UTF-8")

  /** Restore OfflineEvalMetrics from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[OfflineEvalMetric]] = {
    try {
      val rdata = Serialization.read[Seq[OfflineEvalMetric]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { update(_, true) }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}

/** json4s serializer for the OfflineEvalMetric class. */
class OfflineEvalMetricSerializer extends CustomSerializer[OfflineEvalMetric](format => (
  {
    case x: JObject =>
      implicit val formats = Serialization.formats(NoTypeHints)
      OfflineEvalMetric(
        id = (x \ "id").extract[Int].toInt,
        infoid = (x \ "infoid").extract[String],
        evalid = (x \ "evalid").extract[Int].toInt,
        params = (x \ "params").asInstanceOf[JObject].values)
  },
  {
    case x: OfflineEvalMetric =>
      implicit val formats = Serialization.formats(NoTypeHints)
      JObject(
        JField("id", Extraction.decompose(x.id)) ::
          JField("infoid", Extraction.decompose(x.infoid)) ::
          JField("evalid", Extraction.decompose(x.evalid)) ::
          JField("params", Extraction.decompose(x.params)) :: Nil)
  })
)

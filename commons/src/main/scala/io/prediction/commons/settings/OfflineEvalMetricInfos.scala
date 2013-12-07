package io.prediction.commons.settings

import io.prediction.commons.Common

import org.json4s._
import org.json4s.native.Serialization

/**
 * OfflineEvalMetricInfo object.
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
  paramorder: Seq[String])

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

  implicit val formats = Serialization.formats(NoTypeHints) + new OfflineEvalMetricInfoSerializer

  /** Backup all OfflineEvalMetricInfos as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll()).getBytes("UTF-8")

  /** Restore OfflineEvalMetricInfos from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[OfflineEvalMetricInfo]] = {
    try {
      val rdata = Serialization.read[Seq[OfflineEvalMetricInfo]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { update(_, true) }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}

/** json4s serializer for the OfflineEvalMetricInfo class. */
class OfflineEvalMetricInfoSerializer extends CustomSerializer[OfflineEvalMetricInfo](format => (
  {
    case x: JObject =>
      implicit val formats = Serialization.formats(NoTypeHints)
      OfflineEvalMetricInfo(
        id = (x \ "id").extract[String],
        name = (x \ "name").extract[String],
        description = (x \ "description").extract[Option[String]],
        engineinfoids = (x \ "engineinfoids").extract[Seq[String]],
        commands = (x \ "commands").extract[Option[Seq[String]]],
        paramdefaults = (x \ "paramdefaults").asInstanceOf[JObject].values,
        paramnames = (x \ "paramnames").extract[Map[String, String]],
        paramdescription = (x \ "paramdescription").extract[Map[String, String]],
        paramorder = (x \ "paramorder").extract[Seq[String]])
  },
  {
    case x: OfflineEvalMetricInfo =>
      implicit val formats = Serialization.formats(NoTypeHints)
      JObject(
        JField("id", Extraction.decompose(x.id)) ::
          JField("name", Extraction.decompose(x.name)) ::
          JField("description", Extraction.decompose(x.description)) ::
          JField("engineinfoids", Extraction.decompose(x.engineinfoids)) ::
          JField("commands", Extraction.decompose(x.commands)) ::
          JField("paramdefaults", Extraction.decompose(x.paramdefaults)) ::
          JField("paramnames", Extraction.decompose(x.paramnames)) ::
          JField("paramdescription", Extraction.decompose(x.paramdescription)) ::
          JField("paramorder", Extraction.decompose(x.paramorder)) :: Nil)
  })
)

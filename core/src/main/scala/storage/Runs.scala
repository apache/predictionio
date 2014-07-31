package io.prediction.storage

import com.github.nscala_time.time.Imports._
import com.google.common.io.BaseEncoding
import org.json4s._
import org.json4s.native.Serialization

/**
 * Run object.
 *
 * Stores parameters, model, and evaluation results for each run.
 *
 * @param id Run ID.
 * @param starttime Start time of the run.
 * @param endtime End time of the run.
 * @param crossvalidationresults Cross validation results.
 */
case class Run(
  id: String,
  status: String,
  startTime: DateTime,
  endTime: DateTime,
  engineId: String,
  engineVersion: String,
  engineFactory: String,
  metricsClass: String,
  batch: String,
  env: Map[String, String],
  dataSourceParams: String,
  preparatorParams: String,
  algorithmsParams: String,
  servingParams: String,
  metricsParams: String,
  models: Array[Byte],
  multipleMetricsResults: String)

/**
 * Base trait for implementations that interact with Runs in the
 * backend app data store.
 */
trait Runs {
  /** Insert a new Run. */
  def insert(run: Run): String

  /** Get a Run by ID. */
  def get(id: String): Option[Run]

  /** Get a run that is started the latest and has run to completion. */
  def getLatestCompleted(engineId: String, engineVersion: String): Option[Run]

  /** Update a Run. */
  def update(run: Run): Unit

  /** Delete a Run. */
  def delete(id: String): Unit
}

class RunSerializer extends CustomSerializer[Run](format => (
  {
    case JObject(fields) =>
      implicit val formats = DefaultFormats
      val seed = Run(
          id = "",
          status = "",
          startTime = DateTime.now,
          endTime = DateTime.now,
          engineId = "",
          engineVersion = "",
          engineFactory = "",
          metricsClass = "",
          batch = "",
          env = Map(),
          dataSourceParams = "",
          preparatorParams = "",
          algorithmsParams = "",
          servingParams = "",
          metricsParams = "",
          models = Array[Byte](),
          multipleMetricsResults = "")
      fields.foldLeft(seed) { case (run, field) =>
        field match {
          case JField("id", JString(id)) => run.copy(id = id)
          case JField("status", JString(status)) => run.copy(status = status)
          case JField("startTime", JString(startTime)) =>
            run.copy(startTime = Utils.stringToDateTime(startTime))
          case JField("endTime", JString(endTime)) =>
            run.copy(endTime = Utils.stringToDateTime(endTime))
          case JField("engineId", JString(engineId)) =>
            run.copy(engineId = engineId)
          case JField("engineVersion", JString(engineVersion)) =>
            run.copy(engineVersion = engineVersion)
          case JField("engineFactory", JString(engineFactory)) =>
            run.copy(engineFactory = engineFactory)
          case JField("metricsClass", JString(metricsClass)) =>
            run.copy(metricsClass = metricsClass)
          case JField("batch", JString(batch)) => run.copy(batch = batch)
          case JField("env", env) =>
            run.copy(env = Extraction.extract[Map[String, String]](env))
          case JField("dataSourceParams", JString(dataSourceParams)) =>
            run.copy(dataSourceParams = dataSourceParams)
          case JField("preparatorParams", JString(preparatorParams)) =>
            run.copy(preparatorParams = preparatorParams)
          case JField("algorithmsParams", JString(algorithmsParams)) =>
            run.copy(algorithmsParams = algorithmsParams)
          case JField("servingParams", JString(servingParams)) =>
            run.copy(servingParams = servingParams)
          case JField("metricsParams", JString(metricsParams)) =>
            run.copy(metricsParams = metricsParams)
          case JField("models", JString(models)) =>
            run.copy(models = BaseEncoding.base64.decode(models))
          case JField("multipleMetricsResults", JString(multipleMetricsResults)) =>
            run.copy(multipleMetricsResults = multipleMetricsResults)
          case _ => run
        }
      }
  },
  {
    case run: Run =>
      JObject(
        JField("id", JString(run.id)) ::
        JField("status", JString(run.status)) ::
        JField("startTime", JString(run.startTime.toString)) ::
        JField("endTime", JString(run.endTime.toString)) ::
        JField("engineId", JString(run.engineId)) ::
        JField("engineVersion", JString(run.engineVersion)) ::
        JField("engineFactory", JString(run.engineFactory)) ::
        JField("metricsClass", JString(run.metricsClass)) ::
        JField("batch", JString(run.batch)) ::
        JField("env", Extraction.decompose(run.env)(DefaultFormats)) ::
        JField("dataSourceParams", JString(run.dataSourceParams)) ::
        JField("preparatorParams", JString(run.preparatorParams)) ::
        JField("algorithmsParams", JString(run.algorithmsParams)) ::
        JField("servingParams", JString(run.servingParams)) ::
        JField("metricsParams", JString(run.metricsParams)) ::
        JField("models", JString(BaseEncoding.base64.encode(run.models))) ::
        JField("multipleMetricsResults", JString(run.multipleMetricsResults)) ::
        Nil)
  }
))

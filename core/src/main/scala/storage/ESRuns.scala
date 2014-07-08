package io.prediction.storage

import com.github.nscala_time.time.Imports._
import com.google.common.io.BaseEncoding
import grizzled.slf4j.Logging
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.client.Client
import org.elasticsearch.common.xcontent.XContentFactory._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.ext.JodaTimeSerializers
import org.json4s.native.JsonMethods._

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._

class ESRuns(client: Client, index: String) extends Runs with Logging {
  implicit val formats = DefaultFormats ++ JodaTimeSerializers.all
  private val estype = "runs"

  val indices = client.admin.indices
  val typeExistResponse = indices.prepareTypesExists(index).setTypes(estype).get
  if (!typeExistResponse.isExists) {
    val json = (estype -> ("properties" -> ("models" -> ("type" -> "binary"))))
    info(s"mapping: ${compact(render(json))}")
    indices.preparePutMapping(index).setType(estype).
      setSource(compact(render(json))).get
  }

  def insert(run: Run): String = {
    try {
      val builder = jsonBuilder().startObject().
        field("startTime",              run.startTime.toDate).
        field("endTime",                run.endTime.toDate).
        field("engineManifestId",       run.engineManifestId).
        field("engineManifestVersion",  run.engineManifestVersion).
        field("batch",                  run.batch).
        field("evaluationDataParams",   run.evaluationDataParams).
        field("validationParams",       run.validationParams).
        field("cleanserParams",         run.cleanserParams).
        field("algoParamsList",         run.algoParamsList).
        field("serverParams",           run.serverParams).
        field("models",                 run.models).
        field("crossValidationResults", run.crossValidationResults).
        endObject()
      val response = client.prepareIndex(index, estype).
        setSource(builder.string).get
      response.getId
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        ""
    }
  }

  def get(id: String) = {
    try {
      val response = client.prepareGet(index, estype, id).get
      val json = parse(response.getSourceAsString)
      Some(Run(
        id = response.getId,
        startTime = new DateTime((json \ "startTime").extract[String]),
        endTime = new DateTime((json \ "endTime").extract[String]),
        engineManifestId = (json \ "engineManifestId").extract[String],
        engineManifestVersion =
          (json \ "engineManifestVersion").extract[String],
        batch = (json \ "batch").extract[String],
        evaluationDataParams = (json \ "evaluationDataParams").extract[String],
        validationParams = (json \ "validationParams").extract[String],
        cleanserParams = (json \ "cleanserParams").extract[String],
        algoParamsList = (json \ "algoParamsList").extract[String],
        serverParams = (json \ "serverParams").extract[String],
        models = BaseEncoding.base64.decode((json \ "models").extract[String]),
        crossValidationResults =
          (json \ "crossValidationResults").extract[String]))
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        None
      case e: NullPointerException =>
        error(s"Run ID ${id} not found")
        None
    }
  }

  def delete(id: String) = {
    try {
      val response = client.prepareDelete(index, estype, id).get
    } catch {
      case e: ElasticsearchException => error(e.getMessage)
    }
  }
}

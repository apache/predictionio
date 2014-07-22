package io.prediction.storage.elasticsearch

import com.github.nscala_time.time.Imports._
import com.google.common.io.BaseEncoding
import grizzled.slf4j.Logging
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.client.Client
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.{ read, write }

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._

import io.prediction.storage.{ Run, Runs, RunSerializer }

class ESRuns(client: Client, index: String) extends Runs with Logging {
  implicit val formats = DefaultFormats + new RunSerializer
  private val estype = "runs"

  val indices = client.admin.indices
  val typeExistResponse = indices.prepareTypesExists(index).setTypes(estype).get
  if (!typeExistResponse.isExists) {
    val json = (estype -> ("properties" -> ("models" -> ("type" -> "binary"))))
    indices.preparePutMapping(index).setType(estype).
      setSource(compact(render(json))).get
  }

  def insert(run: Run): String = {
    try {
      val response = client.prepareIndex(index, estype).
        setSource(write(run)).get
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
      if (response.isExists)
        Some(read[Run](response.getSourceAsString))
      else
        None
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        None
    }
  }

  def update(run: Run): Unit = {
    try {
      client.prepareUpdate(index, estype, run.id).setDoc(write(run)).get
    } catch {
      case e: ElasticsearchException => error(e.getMessage)
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

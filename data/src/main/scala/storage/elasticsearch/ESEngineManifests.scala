package io.prediction.data.storage.elasticsearch

import grizzled.slf4j.Logging
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.client.Client
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{ read, write }

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._

import io.prediction.data.storage.{ EngineManifest, EngineManifests }
import io.prediction.data.storage.EngineManifestSerializer

class ESEngineManifests(client: Client, index: String) extends EngineManifests
    with Logging {
  implicit val formats = DefaultFormats + new EngineManifestSerializer
  private val estype = "engine_manifests"
  private def esid(id: String, version: String) = s"${id} ${version}"

  def insert(engineManifest: EngineManifest) = {
    val json = write(engineManifest)
    val response = client.prepareIndex(
      index,
      estype,
      esid(engineManifest.id, engineManifest.version)).
      setSource(json).execute().actionGet()
  }

  def get(id: String, version: String) = {
    try {
      val response = client.prepareGet(index, estype, esid(id, version)).
        execute().actionGet()
      if (response.isExists)
        Some(read[EngineManifest](response.getSourceAsString))
      else
        None
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        None
    }
  }

  def getAll() = {
    try {
      var builder = client.prepareSearch()
      ESUtils.getAll[EngineManifest](client, builder)
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        Seq()
    }
  }

  def update(engineManifest: EngineManifest, upsert: Boolean = false) =
    insert(engineManifest)

  def delete(id: String, version: String) = {
    try {
      val response = client.prepareDelete(index, estype, esid(id, version)).
        execute().actionGet()
    } catch {
      case e: ElasticsearchException => error(e.getMessage)
    }
  }
}

package io.prediction.storage.elasticsearch

import grizzled.slf4j.Logging
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.client.Client
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{ read, write }

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._

import io.prediction.storage.{ EngineManifest, EngineManifests }

class ESEngineManifests(client: Client, index: String) extends EngineManifests
    with Logging {
  implicit val formats = Serialization.formats(NoTypeHints)
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
      Some(read[EngineManifest](response.getSourceAsString))
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        None
    }
  }

  def getAll() = {
    val response = client.prepareSearch().execute().actionGet()
    val hits = response.getHits().hits()
    hits.map(h => read[EngineManifest](h.getSourceAsString))
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

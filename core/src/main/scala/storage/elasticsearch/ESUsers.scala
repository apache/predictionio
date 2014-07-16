package io.prediction.storage.elasticsearch

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logging
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.DistanceUnit
import org.elasticsearch.index.query.FilterBuilders._
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortBuilders._
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.format.ISODateTimeFormat
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{ read, write }

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._

import io.prediction.storage.{ User, Users, UserSerializer, Utils }

/** Elasticsearch implementation of Users. */
class ESUsers(client: Client, index: String) extends Users with Logging {
  implicit val formats = DefaultFormats.lossless + new UserSerializer
  private val estype = "users"
  private val indices = client.admin.indices

  val indexExistResponse = indices.prepareExists(index).get
  if (!indexExistResponse.isExists) {
    indices.prepareCreate(index).get
  }

  val typeExistResponse = indices.prepareTypesExists(index).setTypes(estype).get
  if (!typeExistResponse.isExists) {
    val json = (estype ->
      ("properties" -> ("latlng" -> ("type" -> "geo_point"))))
    indices.preparePutMapping(index).setType(estype).
      setSource(compact(render(json))).get
  }

  def insert(user: User) = {
    val response = client.prepareIndex(
      index,
      estype,
      Utils.idWithAppid(user.appid, user.id)).setSource(write(user)).get()
  }

  def get(appid: Int, id: String) = {
    try {
      val response = client.prepareGet(
        index,
        estype,
        Utils.idWithAppid(appid, id)).get()
      Some(read[User](response.getSourceAsString))
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        None
      case e: NullPointerException => None
    }
  }

  def getByAppid(appid: Int) = {
    try {
      val response = client.prepareSearch(index).setTypes(estype).
        setPostFilter(termFilter("appid", appid)).get()
      val hits = response.getHits().hits()
      hits.map(h => read[User](h.getSourceAsString)).toIterator
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        Seq[User]().toIterator
    }
  }

  def update(user: User) = insert(user)

  def delete(appid: Int, id: String) = {
    try {
      client.prepareDelete(index, estype, Utils.idWithAppid(appid, id)).get
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
    }
  }

  def delete(user: User) = delete(user.appid, user.id)

  def deleteByAppid(appid: Int) = {
    try {
      client.prepareDeleteByQuery(index).setTypes(estype).setQuery(
        QueryBuilders.termQuery("appid", appid)).get
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
    }
  }

  def countByAppid(appid: Int): Long = {
    try {
      val response = client.prepareCount(index).setTypes(estype).setQuery(
        QueryBuilders.termQuery("appid", appid)).get
      response.getCount
    } catch {
        case e: ElasticsearchException =>
          error(e.getMessage)
          0
    }
  }
}

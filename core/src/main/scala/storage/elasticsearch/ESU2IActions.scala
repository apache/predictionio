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

import io.prediction.storage.{ U2IAction, U2IActions, U2IActionSerializer, Utils }

/** Elasticsearch implementation of U2IActions. */
class ESU2IActions(client: Client, index: String) extends U2IActions with Logging {
  implicit val formats = DefaultFormats.lossless + new U2IActionSerializer
  private val estype = "u2iactions"
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

  def insert(u2iaction: U2IAction) = {
    val response = client.prepareIndex(index, estype).
      setSource(write(u2iaction)).get
  }

  def getAllByAppid(appid: Int) = {
    try {
      val response = client.prepareSearch(index).setTypes(estype).
        setPostFilter(termFilter("appid", appid)).get
      val hits = response.getHits().hits()
      hits.map(h => read[U2IAction](h.getSourceAsString)).toIterator
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        Seq[U2IAction]().toIterator
    }
  }

  def getByAppidAndTime(appid: Int, startTime: DateTime, untilTime: DateTime) = {
    try {
      val response = client.prepareSearch(index).setTypes(estype).
        setPostFilter(andFilter(
          termFilter("appid", appid),
          rangeFilter("t").gte(startTime).lt(untilTime))).get
      val hits = response.getHits().hits()
      hits.map(h => read[U2IAction](h.getSourceAsString)).toIterator
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        Seq[U2IAction]().toIterator
    }
  }

  def getAllByAppidAndUidAndIids(appid: Int, uid: String, iids: Seq[String]) = {
    try {
      val response = client.prepareSearch(index).setTypes(estype).
        setPostFilter(andFilter(
          termFilter("appid", appid),
          termFilter("uid", uid),
          inFilter("iid", iids: _*))).get
      val hits = response.getHits().hits()
      hits.map(h => read[U2IAction](h.getSourceAsString)).toIterator
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        Seq[U2IAction]().toIterator
    }
  }

  def getAllByAppidAndIid(appid: Int, iid: String, sortedByUid: Boolean = true) = {
    try {
      val request = client.prepareSearch(index).setTypes(estype).
        setPostFilter(andFilter(
          termFilter("appid", appid),
          termFilter("iid", iid)))
      if (sortedByUid) request.addSort("uid", SortOrder.ASC)
      val response = request.get
      val hits = response.getHits().hits()
      hits.map(h => read[U2IAction](h.getSourceAsString)).toIterator
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        Seq[U2IAction]().toIterator
    }
  }

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

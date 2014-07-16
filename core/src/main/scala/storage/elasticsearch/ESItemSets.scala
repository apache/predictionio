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

import io.prediction.storage.{ ItemSet, ItemSets, ItemSetSerializer, Utils }

/** Elasticsearch implementation of ItemSets. */
class ESItemSets(client: Client, index: String) extends ItemSets with Logging {
  implicit val formats = DefaultFormats.lossless + new ItemSetSerializer
  private val estype = "itemsets"

  def insert(itemset: ItemSet) = {
    val response = client.prepareIndex(
      index,
      estype,
      Utils.idWithAppid(itemset.appid, itemset.id)).
      setSource(write(itemset)).get
  }

  def get(appid: Int, id: String) = {
    try {
      val response = client.prepareGet(
        index,
        estype,
        Utils.idWithAppid(appid, id)).get
      Some(read[ItemSet](response.getSourceAsString))
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
        setPostFilter(termFilter("appid", appid)).get
      val hits = response.getHits().hits()
      hits.map(h => read[ItemSet](h.getSourceAsString)).toIterator
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        Seq[ItemSet]().toIterator
    }
  }

  def getByAppidAndTime(
      appid: Int,
      startTime: DateTime,
      untilTime: DateTime) = {
    try {
      val response = client.prepareSearch(index).setTypes(estype).setPostFilter(
        andFilter(
          termFilter("appid", appid),
          rangeFilter("t").gte(startTime).lt(untilTime))).get
      val hits = response.getHits().hits()
      hits.map(h => read[ItemSet](h.getSourceAsString)).toIterator
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        Seq[ItemSet]().toIterator
    }
  }

  def delete(itemset: ItemSet) = {
    try {
      client.prepareDelete(index, estype, Utils.idWithAppid(
        itemset.appid, itemset.id)).get
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
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
}

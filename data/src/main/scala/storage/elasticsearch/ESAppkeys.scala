/** Copyright 2014 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.data.storage.elasticsearch

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
import scala.util.Random

import io.prediction.data.storage.{ Appkey, Appkeys, Utils }

/** Elasticsearch implementation of Items. */
class ESAppkeys(client: Client, index: String) extends Appkeys with Logging {
  implicit val formats = DefaultFormats.lossless
  private val estype = "appkeys"

  def insert(appkey: Appkey) = {
    val generatedkey = Random.alphanumeric.take(64).mkString
    val realappkey = appkey.copy(appkey = generatedkey)
    if (update(realappkey)) Some(generatedkey) else None
  }

  def get(appkey: String) = {
    try {
      val response = client.prepareGet(
        index,
        estype,
        appkey).get()
      Some(read[Appkey](response.getSourceAsString))
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        None
      case e: NullPointerException => None
    }
  }

  def getAll() = {
    try {
      val builder = client.prepareSearch(index).setTypes(estype)
      ESUtils.getAll[Appkey](client, builder)
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        Seq[Appkey]()
    }
  }

  def getByAppid(appid: Int) = {
    try {
      val builder = client.prepareSearch(index).setTypes(estype).
        setPostFilter(termFilter("appid", appid))
      ESUtils.getAll[Appkey](client, builder)
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        Seq[Appkey]()
    }
  }

  def update(appkey: Appkey) = {
    try {
      val response = client.prepareIndex(index, estype, appkey.appkey).
        setSource(write(appkey)).get()
      true
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        false
    }
  }

  def delete(appkey: String) = {
    try {
      client.prepareDelete(index, estype, appkey).get
      true
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        false
    }
  }
}

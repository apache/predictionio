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

import io.prediction.data.storage.{ AccessKey, AccessKeys, Utils }

/** Elasticsearch implementation of AccessKeys. */
class ESAccessKeys(client: Client, index: String)
    extends AccessKeys with Logging {
  implicit val formats = DefaultFormats.lossless
  private val estype = "accesskeys"

  val indices = client.admin.indices
  val indexExistResponse = indices.prepareExists(index).get
  if (!indexExistResponse.isExists) {
    indices.prepareCreate(index).get
  }
  val typeExistResponse = indices.prepareTypesExists(index).setTypes(estype).get
  if (!typeExistResponse.isExists) {
    val json =
      (estype ->
        ("properties" ->
          ("key" -> ("type" -> "string") ~ ("index" -> "not_analyzed")) ~
          ("events" -> ("type" -> "string") ~ ("index" -> "not_analyzed"))))
    indices.preparePutMapping(index).setType(estype).
      setSource(compact(render(json))).get
  }

  def insert(accessKey: AccessKey) = {
    val generatedkey = Random.alphanumeric.take(64).mkString
    val realaccesskey = accessKey.copy(key = generatedkey)
    if (update(realaccesskey)) Some(generatedkey) else None
  }

  def get(key: String) = {
    try {
      val response = client.prepareGet(
        index,
        estype,
        key).get()
      Some(read[AccessKey](response.getSourceAsString))
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
      ESUtils.getAll[AccessKey](client, builder)
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        Seq[AccessKey]()
    }
  }

  def getByAppid(appid: Int) = {
    try {
      val builder = client.prepareSearch(index).setTypes(estype).
        setPostFilter(termFilter("appid", appid))
      ESUtils.getAll[AccessKey](client, builder)
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        Seq[AccessKey]()
    }
  }

  def update(accessKey: AccessKey) = {
    try {
      val response = client.prepareIndex(index, estype, accessKey.key).
        setSource(write(accessKey)).get()
      true
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        false
    }
  }

  def delete(key: String) = {
    try {
      client.prepareDelete(index, estype, key).get
      true
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        false
    }
  }
}

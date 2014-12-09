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

import io.prediction.data.storage.{ App, Apps, Utils }

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

/** Elasticsearch implementation of Items. */
class ESApps(client: Client, index: String) extends Apps with Logging {
  implicit val formats = DefaultFormats.lossless
  private val estype = "apps"
  private val seq = new ESSequences(client, index)

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
          ("name" -> ("type" -> "string") ~ ("index" -> "not_analyzed"))))
    indices.preparePutMapping(index).setType(estype).
      setSource(compact(render(json))).get
  }

  def insert(app: App) = {
    val id =
      if (app.id == 0) {
        var roll = seq.genNext("apps")
        while (!get(roll).isEmpty) roll = seq.genNext("apps")
        roll
      }
      else app.id
    val realapp = app.copy(id = id)
    if (update(realapp)) Some(id) else None
  }

  def get(id: Int) = {
    try {
      val response = client.prepareGet(
        index,
        estype,
        id.toString).get()
      Some(read[App](response.getSourceAsString))
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        None
      case e: NullPointerException => None
    }
  }

  def getByName(name: String) = {
    try {
      val response = client.prepareSearch(index).setTypes(estype).
        setPostFilter(termFilter("name", name)).get
      val hits = response.getHits().hits()
      if (hits.size > 0)
        Some(read[App](hits.head.getSourceAsString))
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
      val builder = client.prepareSearch(index).setTypes(estype)
      ESUtils.getAll[App](client, builder)
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        Seq[App]()
    }
  }

  def update(app: App) = {
    try {
      val response = client.prepareIndex(index, estype, app.id.toString).
        setSource(write(app)).get()
      true
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        false
    }
  }

  def delete(id: Int) = {
    try {
      client.prepareDelete(index, estype, id.toString).get
      true
    } catch {
      case e: ElasticsearchException =>
        error(e.getMessage)
        false
    }
  }
}

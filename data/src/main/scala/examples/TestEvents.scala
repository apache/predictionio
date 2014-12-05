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

package io.prediction.data.examples

import  io.prediction.data.storage.LEvents
import  io.prediction.data.storage.Event
import  io.prediction.data.storage.DataMap
import  io.prediction.data.storage.hbase
import  io.prediction.data.storage.StorageClientConfig
import  io.prediction.data.storage.Storage

//import io.prediction.data.storage.elasticsearch.ESStorageClient
//import io.prediction.data.storage.hbase.HBStorageClient

import io.prediction.data.storage.elasticsearch.ESLEvents
import io.prediction.data.storage.hbase.HBLEvents

import org.json4s.JObject
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.{ read, write }

import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.DateTime

object TestLEvents {

  def main(args: Array[String]) {
    args(0) match {
      case "HB" => testHBLEvents()
      case "ES" => testESLEvents()
    }
  }

  val e = Event(
    event = "$set",
    entityType = "axx",
    entityId = "abc",
    targetEntityType = None,
    targetEntityId = None,
    properties = DataMap(parse("""
      { "numbers" : [1, 2, 3, 4],
        "abc" : "some_string",
        "def" : 4, "k" : false
      } """).asInstanceOf[JObject]),
    eventTime = DateTime.now,
    tags = List("tag1", "tag2"),
    prId = None
  )

  val e2 = e.copy(properties=DataMap())

  def testESLEvents() {

    /*val config = StorageClientConfig(Seq("localhost"), Seq(9300))
    val storageClient = new ESStorageClient(config)
    val client = storageClient.client
    val eventConnector = storageClient.eventClient*/
    val eventConnector = Storage.getDataObject[LEvents]("predictionio_events_es").asInstanceOf[ESLEvents]
    implicit val formats = eventConnector.formats

    //client.prepareGet("testindex", "events", "Abcdef").get()

    val x = write(e)
    println(x)
    println(x.getClass)

    val de = eventConnector.insert(e, 4)
    println(de)
    de match {
      case Right(d) => {
        val e2 = eventConnector.get(d, 4)
        println(e2)
        val k = eventConnector.delete(d, 4)
        println(k)
        val k2 = eventConnector.delete(d, 4)
        println(k2)
      }
      case _ => {println("match error")}
    }

    val i1 = eventConnector.insert(e, 4)
    println(i1)
    val i2 = eventConnector.insert(e, 4)
    println(i2)
    val i3 = eventConnector.insert(e, 4)
    println(i3)

    // force refresh index for testing, else get may not have result
    //client.admin().indices().prepareRefresh("testindex").get()

    val all = eventConnector.getByAppId(4)
    println(all.right.map{ x =>
      val l = x.toList
      s"size ${l.size}, ${l}"
    })

    val delAll = eventConnector.deleteByAppId(4)
    println(delAll)
    val all2 = eventConnector.getByAppId(4)
    println(all2)
    //client.close()
  }

  def testHBLEvents() = {

    println("testHBLEvents")

    /*val config = StorageClientConfig(Seq("localhost"), Seq(9300))
    val storageClient = new HBStorageClient(config)
    val client = storageClient.client
    val eventConnector = storageClient.eventClient*/
    val storageClient = new hbase.StorageClient(StorageClientConfig(
      hosts = Seq("localhost"),
      ports = Seq(1234)))

    val eventConnector = new hbase.HBLEvents(storageClient.client,
      "predictionio_events_hb")

    val appId = 4

    eventConnector.init(appId)
    val de = eventConnector.insert(e, appId)
    println(de)
    de match {
      case Right(d) => {
        val e2 = eventConnector.get(d, appId)
        println(e2)
        val k = eventConnector.delete(d, appId)
        println(k)
        val k2 = eventConnector.delete(d, appId)
        println(k2)
      }
      case _ => {println("match error")}
    }

    val i1 = eventConnector.insert(e, appId)
    println(i1)
    val i2 = eventConnector.insert(e, appId)
    println(i2)
    val i3 = eventConnector.insert(e, appId)
    println(i3)

    val all = eventConnector.getByAppId(appId)
    println(all.right.map{ x =>
      val l = x.toList
      s"size ${l.size}, ${l}"
    })

    val delAll = eventConnector.deleteByAppId(appId)
    println(delAll)
    val all2 = eventConnector.getByAppId(appId)
    println(all2)

    (eventConnector.insert(e2, appId) match {
      case Right(id) => eventConnector.get(id, appId)
      case Left(x) => Left(x)
    }) match {
      case Right(e) => println(e)
      case _ => println("error")
    }

  }
}

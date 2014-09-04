package io.prediction.data.storage

import io.prediction.data.Utils
import io.prediction.data.storage.elasticsearch.ESStorageClient
import io.prediction.data.storage.hbase.HBStorageClient

import org.json4s.JObject
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.{ read, write }

import org.joda.time.DateTime

object TestEvents {

  def main(args: Array[String]) {
    testESEvent()
  }

  val e = Event(
    entityId = "abc",
    targetEntityId = None,
    event = "$set",
    properties = DataMap(parse("""
      { "numbers" : [1, 2, 3, 4],
        "abc" : "some_string",
        "def" : 4, "k" : false
      } """).asInstanceOf[JObject]),
    eventTime = DateTime.now,
    tags = List("tag1", "tag2"),
    appId = 4,
    predictionKey = None
  )

  def testESEvent() {

    val config = StorageClientConfig(Seq("localhost"), Seq(9300))
    val storageClient = new ESStorageClient(config)
    val client = storageClient.client
    val eventConnector = storageClient.eventClient
    // new ESEvents(client, "testindex")
    implicit val formats = eventConnector.formats

    client.prepareGet("testindex", "events", "Abcdef").get()

    val x = write(e)
    println(x)
    println(x.getClass)

    val de = eventConnector.insert(e)
    println(de)
    de match {
      case Right(d) => {
        val e2 = eventConnector.get(d)
        println(e2)
        val k = eventConnector.delete(d)
        println(k)
        val k2 = eventConnector.delete(d)
        println(k2)
      }
      case _ => {println("match error")}
    }

    val i1 = eventConnector.insert(e)
    println(i1)
    val i2 = eventConnector.insert(e)
    println(i2)
    val i3 = eventConnector.insert(e)
    println(i3)

    // force refresh index for testing, else get may not have result
    client.admin().indices().prepareRefresh("testindex").get()

    val all = eventConnector.getByAppId(4)
    println(all.right.map{ x =>
      val l = x.toList
      s"size ${l.size}, ${l}"
    })

    val delAll = eventConnector.deleteByAppId(4)
    println(delAll)
    val all2 = eventConnector.getByAppId(4)
    println(all2)
    client.close()
  }

  def testHBEvent() = {

    println("testHBEvent")

    val config = StorageClientConfig(Seq("localhost"), Seq(9300))
    val storageClient = new HBStorageClient(config)
    val client = storageClient.client
    val eventConnector = storageClient.eventClient

    val de = eventConnector.insert(e)
    println(de)
    de match {
      case Right(d) => {
        val e2 = eventConnector.get(d)
        println(e2)
        val k = eventConnector.delete(d)
        println(k)
        val k2 = eventConnector.delete(d)
        println(k2)
      }
      case _ => {println("match error")}
    }

    val i1 = eventConnector.insert(e)
    println(i1)
    val i2 = eventConnector.insert(e)
    println(i2)
    val i3 = eventConnector.insert(e)
    println(i3)

    val all = eventConnector.getByAppId(4)
    println(all.right.map{ x =>
      val l = x.toList
      s"size ${l.size}, ${l}"
    })

    val delAll = eventConnector.deleteByAppId(4)
    println(delAll)
    val all2 = eventConnector.getByAppId(4)
    println(all2)

  }
}

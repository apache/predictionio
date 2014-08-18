package io.prediction.dataapi.elasticsearch

import io.prediction.dataapi.Event
//import io.prediction.dataapi.EventSeriliazer
import io.prediction.dataapi.Events
import grizzled.slf4j.Logging

import org.elasticsearch.ElasticsearchException
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.node.NodeBuilder.nodeBuilder
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.ConnectTransportException

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{ read, write }
import org.json4s.ext.JodaTimeSerializers

import com.github.nscala_time.time.Imports._

// blocking
class ESEvents(client: Client, index: String) extends Events with Logging {

  implicit val formats = DefaultFormats.lossless ++ JodaTimeSerializers.all
  // new EventSeriliazer

  val typeName = "events"

  def insert(event: Event): Option[String] = {
    try {
      val response = client.prepareIndex(index, typeName)
        .setSource(write(event)).get
      Some(response.getId())
    } catch {
      case e: ElasticsearchException => {
        error(e.getMessage)
        println(e)
        None
      }
    }
  }

  def get(eventId: String): Option[Event] = {
    try {
      val response = client.prepareGet(index, typeName, eventId).get()
      Some(read[Event](response.getSourceAsString))
    } catch {
      case e : ElasticsearchException => {
        error(e.getMessage)
        println(e.getMessage)
        None
      }
    }
  }

  def delete(eventId: String): Boolean = {
    try {
      val response = client.prepareDelete(index, typeName, eventId).get()
      response.isFound()
    } catch {
      case e: ElasticsearchException => {
        error(e.getMessage)
        println(e.getMessage)
        false
      }
    }
  }
}


object TestEvents {

  import io.prediction.dataapi.StorageClient

  def main(args: Array[String]) {
    val e = Event(
      entityId = "abc",
      targetEntityId = None,
      event = "$set",
      properties = parse("""
        { "numbers" : [1, 2, 3, 4],
          "abc" : "some_string",
          "def" : 4, "k" : false
        } """).asInstanceOf[JObject],
      eventTime = DateTime.now,
      tags = List("tag1", "tag2"),
      appId = 4,
      predictionKey = None
    )

    val client = StorageClient.client
    val eventConnector = new ESEvents(client, "testindex")
    implicit val formats = eventConnector.formats

    val x = write(e)
    println(x)
    println(x.getClass)

    val d = eventConnector.insert(e).get
    println(d)
    val e2 = eventConnector.get(d)
    println(e2.get)
    val k = eventConnector.delete(d)
    println(k)
    val k2 = eventConnector.delete(d)
    println(k2)
    client.close()
  }
}

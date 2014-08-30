package io.prediction.dataapi.storage.elasticsearch

import io.prediction.dataapi.storage.Event
import io.prediction.dataapi.storage.StorageError
import io.prediction.dataapi.storage.Events
import io.prediction.dataapi.storage.EventJson4sSupport

import grizzled.slf4j.Logging

import org.elasticsearch.ElasticsearchException
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.node.NodeBuilder.nodeBuilder
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.ConnectTransportException
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse
import org.elasticsearch.action.ActionListener
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.index.query.QueryBuilders

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{ read, write }
//import org.json4s.ext.JodaTimeSerializers

import com.github.nscala_time.time.Imports._

import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global // TODO

class ESEvents(client: Client, index: String) extends Events with Logging {

  implicit val formats = DefaultFormats + new EventJson4sSupport.DBSerializer
  //implicit val formats = DefaultFormats.lossless ++ JodaTimeSerializers.all

  val typeName = "events"

  override
  def futureInsert(event: Event): Future[Either[StorageError, String]] = {
    val response = Promise[IndexResponse]

    client.prepareIndex(index, typeName)
      .setSource(write(event))
      .execute(new ESActionListener(response))

    response.future
      .map(r => Right(r.getId()))
      .recover {
        case e: Exception => Left(StorageError(e.toString))
      }
  }

  override
  def futureGet(eventId: String):
    Future[Either[StorageError, Option[Event]]] = {

    val response = Promise[GetResponse]

    client.prepareGet(index, typeName, eventId)
      .execute(new ESActionListener(response))

    response.future
      .map { r =>
        if (r.isExists)
          Right(Some(read[Event](r.getSourceAsString)))
        else
          Right(None)
      }.recover {
        case e: Exception => Left(StorageError(e.toString))
      }
  }

  override
  def futureDelete(eventId: String): Future[Either[StorageError, Boolean]] = {
    val response = Promise[DeleteResponse]

    client.prepareDelete(index, typeName, eventId)
      .execute(new ESActionListener(response))

    response.future
      .map(r => Right(r.isFound()))
      .recover {
        case e: Exception => Left(StorageError(e.toString))
      }
  }

  override
  def futureGetByAppId(appId: Int):
    Future[Either[StorageError, Iterator[Event]]] = {
    val response = Promise[SearchResponse]

    client.prepareSearch(index).setTypes(typeName)
      .setPostFilter(FilterBuilders.termFilter("appId", appId))
      .execute(new ESActionListener(response))

    response.future
      .map{ r =>
        val dataIt = r.getHits().hits()
          .map(h => read[Event](h.getSourceAsString)).toIterator
        Right(dataIt)
      }.recover {
        case e: Exception => Left(StorageError(e.toString))
      }

  }

  override
  def futureDeleteByAppId(appId: Int):
    Future[Either[StorageError, Unit]] = {

    val response = Promise[DeleteByQueryResponse]

    client.prepareDeleteByQuery(index).setTypes(typeName)
      .setQuery(QueryBuilders.termQuery("appId", appId))
      .execute(new ESActionListener(response))

    response.future
      .map { r =>
        val indexResponse = r.getIndex(index)
        val numFailures = indexResponse.getFailedShards()
        if (numFailures != 0)
          Left(StorageError(s"Failed to delete ${numFailures} shards."))
        else
          Right(())
      }.recover {
        case e: Exception => Left(StorageError(e.toString))
      }

  }

/* old code

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

  override
  def get(eventId: String): Option[Event] = {
    try {
      val response = client.prepareGet(index, typeName, eventId).get()
      if (response.isExists)
        Some(read[Event](response.getSourceAsString))
      else
        None
    } catch {
      case e : ElasticsearchException => {
        error(e.getMessage)
        println(e.getMessage)
        None
      }
    }
  }

  override
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
*/

}


class ESActionListener[T](val p: Promise[T]) extends ActionListener[T]{
  override def onResponse(r: T) = {
    p.success(r)
  }
  override def onFailure(e: Throwable) = {
    p.failure(e)
  }
}


object TestEvents {

  import io.prediction.dataapi.storage.StorageClientConfig

  def main(args: Array[String]) {
    val e = Event(
      entityId = "abc",
      targetEntityId = None,
      event = "$set",
      properties = /*Map(
        "numbers" -> List(1, 2, 3, 4),
        "abc" -> "some_string",
        "def" -> 4,
         "k" -> false
      ),*/
      parse("""
        { "numbers" : [1, 2, 3, 4],
          "abc" : "some_string",
          "def" : 4, "k" : false
        } """).asInstanceOf[JObject].obj.toMap,
      eventTime = DateTime.now,
      tags = List("tag1", "tag2"),
      appId = 4,
      predictionKey = None
    )

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
}

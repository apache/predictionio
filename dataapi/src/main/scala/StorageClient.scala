package io.prediction.dataapi

import io.prediction.dataapi.elasticsearch.ESEvents

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.node.NodeBuilder.nodeBuilder
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.ConnectTransportException

object StorageClient {
  case class StorageClientConfig(
    hosts: Seq[String],
    ports: Seq[Int])

  val config = StorageClientConfig(Seq("localhost"), Seq(9300))
  
  val client = try {
    val transportClient = new TransportClient()
    (config.hosts zip config.ports) foreach { hp =>
      transportClient.addTransportAddress(
        new InetSocketTransportAddress(hp._1, hp._2))
    }
    transportClient
  } catch {
    case e: ConnectTransportException =>
      throw new Exception(e.getMessage)
  }

  val eventClient = new ESEvents(client, "testindex")
}

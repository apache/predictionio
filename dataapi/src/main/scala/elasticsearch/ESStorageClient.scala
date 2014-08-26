package io.prediction.dataapi.elasticsearch

import io.prediction.dataapi.BaseStorageClient
import io.prediction.dataapi.StorageClientConfig

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.node.NodeBuilder.nodeBuilder
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.ConnectTransportException

class ESStorageClient(val config: StorageClientConfig)
  extends BaseStorageClient {

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

  override
  val prefix = "ES"
}

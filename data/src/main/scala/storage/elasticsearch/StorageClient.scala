package io.prediction.data.storage.elasticsearch

import grizzled.slf4j.Logging
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.ConnectTransportException

import io.prediction.data.storage.BaseStorageClient
import io.prediction.data.storage.StorageClientConfig
import io.prediction.data.storage.StorageClientException

class StorageClient(val config: StorageClientConfig) extends BaseStorageClient
    with Logging {
  override val prefix = "ES"
  val client = try {
    val transportClient = new TransportClient()
    (config.hosts zip config.ports) foreach { hp =>
      transportClient.addTransportAddress(
        new InetSocketTransportAddress(hp._1, hp._2))
    }
    transportClient
  } catch {
    case e: ConnectTransportException =>
      throw new StorageClientException(e.getMessage)
  }
}

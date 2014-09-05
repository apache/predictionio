package io.prediction.data.storage.mongodb

import com.mongodb.casbah.Imports._
import grizzled.slf4j.Logging

import io.prediction.data.storage.BaseStorageClient
import io.prediction.data.storage.StorageClientConfig
import io.prediction.data.storage.StorageClientException

class StorageClient(val config: StorageClientConfig) extends BaseStorageClient
    with Logging {
  override val prefix = "Mongo"
  val client = try {
    val addresses = (config.hosts zip config.ports).map(hp =>
      new ServerAddress(hp._1, hp._2)
    ).toList
    MongoClient(addresses)
  } catch {
    case e: MongoException =>
      throw new StorageClientException(e.getMessage)
  }
}

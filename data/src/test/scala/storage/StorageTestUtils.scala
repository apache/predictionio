package io.prediction.data.storage

import io.prediction.data.storage.hbase.HBClient

import com.mongodb.casbah.Imports._
import org.elasticsearch.client.Client

object StorageTestUtils {
  val elasticsearchSourceName = "elasticsearch"
  val mongodbSourceName = "mongodb"
  val hbaseSourceName = "hbase"

  def dropElasticsearchIndex(indexName: String) = {
    Storage.getClient(elasticsearchSourceName).get.client.asInstanceOf[Client].
      admin.indices.prepareDelete(indexName).get
  }

  def dropMongoDatabase(dbName: String) = {
    Storage.getClient(mongodbSourceName).get.client.asInstanceOf[MongoClient].
      dropDatabase(dbName)
  }

  def dropHBaseNamespace(namespace: String) = {

    val hbClient = Storage.getClient(mongodbSourceName).get.client
      .asInstanceOf[HBClient]

    val tableNames = hbClient.admin.listTableNamesByNamespace(namespace)
    tableNames.foreach { name =>
      hbClient.admin.disableTable(name)
      hbClient.admin.deleteTable(name)
    }
    hbClient.admin.deleteNamespace(namespace)
  }

}

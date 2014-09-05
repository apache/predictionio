package io.prediction.data.storage

import com.mongodb.casbah.Imports._
import org.elasticsearch.client.Client

object StorageTestUtils {
  val elasticsearchSourceName = "elasticsearch"
  val mongodbSourceName = "mongodb"

  def dropElasticsearchIndex(indexName: String) = {
    Storage.getClient(elasticsearchSourceName).get.client.asInstanceOf[Client].
      admin.indices.prepareDelete(indexName).get
  }

  def dropMongoDatabase(dbName: String) = {
    Storage.getClient(mongodbSourceName).get.client.asInstanceOf[MongoClient].
      dropDatabase(dbName)
  }
}

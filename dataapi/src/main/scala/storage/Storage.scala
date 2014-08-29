package io.prediction.dataapi.storage

case class StorageClientConfig(
  hosts: Seq[String],
  ports: Seq[Int])

trait BaseStorageClient {
  val config: StorageClientConfig
  val client: AnyRef
  val eventClient: Events // TODO: temporarily
  val prefix: String = ""
}

object Storage {
  // TODO: read from env or configuration

  val config = StorageClientConfig(Seq("localhost"), Seq(9300))

  lazy val esStorageClient: BaseStorageClient =
    new elasticsearch.ESStorageClient(config)

  lazy val hbStorageClient: BaseStorageClient =
    new hbase.HBStorageClient(config)

  def storageClient(storageType: String): BaseStorageClient = {
    storageType match {
      case "ES" => esStorageClient
      case "HB" => hbStorageClient
      case _ => esStorageClient
    }
  }
  def client(storageType: String) = storageClient(storageType).client

  def eventClient(storageType: String) = storageClient(storageType).eventClient

}

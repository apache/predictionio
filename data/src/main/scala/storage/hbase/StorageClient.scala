package io.prediction.data.storage.hbase

import io.prediction.data.storage.BaseStorageClient
import io.prediction.data.storage.StorageClientConfig

import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.HBaseAdmin
import org.apache.hadoop.hbase.NamespaceDescriptor
import org.apache.hadoop.hbase.NamespaceExistException
import org.apache.hadoop.conf.Configuration

case class HBClient(
  val conf: Configuration,
  val admin: HBaseAdmin
)

class StorageClient(val config: StorageClientConfig)
  extends BaseStorageClient {

  val conf = HBaseConfiguration.create()

  val client = HBClient(
    conf = conf,
    admin = new HBaseAdmin(conf)
  )

  private val namespace = "predictionio_appdata"

  val nameDesc = NamespaceDescriptor.create(namespace).build()

  try {
    client.admin.createNamespace(nameDesc)
  } catch {
    case e: NamespaceExistException => Unit
    case e: Exception => throw new RuntimeException(e)
  }

  val eventClient = new HBEvents(client, namespace)

  override
  val prefix = "HB"
}

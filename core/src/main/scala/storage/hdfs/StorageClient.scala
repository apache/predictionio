package io.prediction.storage.hdfs

import io.prediction.storage.BaseStorageClient
import io.prediction.storage.StorageClientConfig
import io.prediction.storage.StorageClientException

import grizzled.slf4j.Logging
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path

class StorageClient(val config: StorageClientConfig) extends BaseStorageClient
    with Logging {
  override val prefix = "HDFS"
  val conf = new Configuration
  val fs = FileSystem.get(conf)
  fs.setWorkingDirectory(new Path(config.hosts.head))
  val client = fs
}

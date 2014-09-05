package io.prediction.data.storage.localfs

import io.prediction.data.storage.BaseStorageClient
import io.prediction.data.storage.StorageClientConfig
import io.prediction.data.storage.StorageClientException

import grizzled.slf4j.Logging

import java.io.File

class StorageClient(val config: StorageClientConfig) extends BaseStorageClient
    with Logging {
  override val prefix = "LocalFS"
  val f = new File(config.hosts.head)
  if (f.exists) {
    if (!f.isDirectory) throw new StorageClientException(
      s"${f} already exists but it is not a directory!")
    if (!f.canWrite) throw new StorageClientException(
      s"${f} already exists but it is not writable!")
  } else {
    if (!f.mkdirs) throw new StorageClientException(
      s"${f} does not exist and automatic creation failed!")
  }
  val client = f
}

package io.prediction.data.storage.hdfs

import io.prediction.data.storage.Model
import io.prediction.data.storage.Models

import com.google.common.io.ByteStreams
import grizzled.slf4j.Logging
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path

import java.io.IOException

class HDFSModels(fs: FileSystem, prefix: String)
  extends Models with Logging {

  def insert(i: Model) = {
    try {
      val fsdos = fs.create(new Path(s"${prefix}${i.id}"))
      fsdos.write(i.models)
      fsdos.close
    } catch {
      case e: IOException => error(e.getMessage)
    }
  }

  def get(id: String) = {
    try {
      val p = new Path(s"${prefix}${id}")
      Some(Model(
        id = id,
        models = ByteStreams.toByteArray(fs.open(p))))
    } catch {
      case e: Throwable =>
        error(e.getMessage)
        None
    }
  }

  def delete(id: String) = {
    val p = new Path(s"${prefix}${id}")
    if (!fs.delete(p, false))
      error(s"Unable to delete ${fs.makeQualified(p).toString}!")
  }
}

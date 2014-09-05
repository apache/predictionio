package io.prediction.data.storage.localfs

import io.prediction.data.storage.Model
import io.prediction.data.storage.Models

import grizzled.slf4j.Logging

import scala.io.Source

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class LocalFSModels(f: File, prefix: String)
  extends Models with Logging {

  def insert(i: Model) = {
    try {
      val fos = new FileOutputStream(new File(f, s"${prefix}${i.id}"))
      fos.write(i.models)
      fos.close
    } catch {
      case e: FileNotFoundException => error(e.getMessage)
    }
  }

  def get(id: String) = {
    try {
      Some(Model(
        id = id,
        models = Source.fromFile(new File(f, s"${prefix}${id}"))(
          scala.io.Codec.ISO8859).map(_.toByte).toArray))
    } catch {
      case e: Throwable =>
        error(e.getMessage)
        None
    }
  }

  def delete(id: String) = {
    val m = new File(f, s"${prefix}${id}")
    if (!m.delete) error(s"Unable to delete ${m.getCanonicalPath}!")
  }
}

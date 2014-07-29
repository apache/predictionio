package io.prediction.controller

import io.prediction.workflow.KryoInstantiator

import com.twitter.chill.KryoInjection
import org.json4s._
import org.json4s.ext.JodaTimeSerializers

import scala.io.Source

import _root_.java.io.File
import _root_.java.io.FileOutputStream

/** Controller utilities.
  *
  * @group General
  */
object Utils {
  /** Default JSON4S serializers for PredictionIO controllers. */
  val json4sDefaultFormats = DefaultFormats.lossless ++ JodaTimeSerializers.all

  /** Save a model object as a file to a temporary location on local filesystem.
    * It will first try to use the location indicated by the environmental
    * variable PIO_FS_TMPDIR, then fall back to the java.io.tmpdir property.
    *
    * @param id Used as the filename of the file.
    * @param model Model object.
    */
  def save(id: String, model: Any): Unit = {
    val tmpdir = sys.env.get("PIO_FS_TMPDIR").getOrElse(
      System.getProperty("java.io.tmpdir"))
    val modelFile = tmpdir + File.separator + id
    (new File(tmpdir)).mkdirs
    val fos = new FileOutputStream(modelFile)
    fos.write(KryoInjection(model))
    fos.close
  }

  /** Load a model object from a file in a temporary location on local
    * filesystem. It will first try to use the location indicated by the
    * environmental variable PIO_FS_TMPDIR, then fall back to the java.io.tmpdir
    * property.
    *
    * @param id Used as the filename of the file.
    */
  def load(id: String): Any = {
    val tmpdir = sys.env.get("PIO_FS_TMPDIR").getOrElse(
      System.getProperty("java.io.tmpdir"))
    val modelFile = tmpdir + File.separator + id
    val src = Source.fromFile(modelFile)(scala.io.Codec.ISO8859)
    val kryoInstantiator = new KryoInstantiator(getClass.getClassLoader)
    val kryo = KryoInjection.instance(kryoInstantiator)
    val m = kryo.invert(src.map(_.toByte).toArray).get
    src.close
    m
  }
}

package io.prediction.controller.java

import scala.collection.JavaConversions._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/** Internal Java utilities. */
object JavaUtils {
  // This "fake" tags are adopted from Spark's Java API.
  // Scala requires manifest or classtag for some data. But Prediction.IO
  // doesn't really need it as our system is oblivious to the actual data. We
  // pass a fake ClassTag / Manifest to keep the scala compiler happy.
  def fakeClassTag[T]: ClassTag[T] = {
    ClassTag.AnyRef.asInstanceOf[ClassTag[T]]
  }

  def fakeManifest[T]: Manifest[T] = {
    manifest[AnyRef].asInstanceOf[Manifest[T]]
  }
}

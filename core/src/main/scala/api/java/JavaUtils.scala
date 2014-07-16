package io.prediction.api.java

import scala.collection.JavaConversions._
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import scala.reflect.ClassTag


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

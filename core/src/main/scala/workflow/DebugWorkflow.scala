package io.prediction.workflow

import grizzled.slf4j.Logging

import scala.language.existentials


import com.github.nscala_time.time.Imports.DateTime

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.FileInputStream
import java.io.ObjectInputStream
import scala.collection.JavaConversions._
import java.lang.{ Iterable => JIterable }
import java.util.{ Map => JMap }

//import io.prediction.core._
//import io.prediction._

import org.apache.spark.rdd.RDD

import scala.reflect.Manifest

//import io.prediction.java._

// FIXME: move to better location.
object WorkflowContext extends Logging {
  def apply(batch: String = "", env: Map[String, String] = Map()): SparkContext = {
    val conf = new SparkConf().setAppName(s"PredictionIO: $batch")
    info(s"Environment received: ${env}")
    env.map(kv => conf.setExecutorEnv(kv._1, kv._2))
    info(s"SparkConf executor environment: ${conf.getExecutorEnv}")
    conf.set("spark.local.dir", "~/tmp/spark")
    conf.set("spark.executor.memory", "8g")

    val sc = new SparkContext(conf)
    return sc
  }
}

object DebugWorkflow {
  def debugString[D](data: D): String = {
    val s: String = data match {
      case rdd: RDD[_] => {
        debugString(rdd.collect)
      }
      case array: Array[_] => {
        "[" + array.map(debugString).mkString(",") + "]"
      }
      case d: AnyRef => {
        d.toString
      }
      case null => "null"
    }
    s
  }
}


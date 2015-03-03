package io.prediction.controller

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.reflect._
import scala.reflect.runtime.universe._
import grizzled.slf4j.Logger

abstract class Metric[EI, Q, P, A, R](implicit rOrder: Ordering[R]) 
extends Serializable {
  def header: String = this.getClass.getName
  def calculate(sc: SparkContext, evalDataSet: Seq[(EI, RDD[(Q, P, A)])]): R 

  def compare(r0: R, r1: R): Int = rOrder.compare(r0, r1)
}

// Returns the global average of the score returned by the calculate method.
abstract class AverageMetric[EI, Q, P, A]
extends Metric[EI, Q, P, A, Double] {
  
  def calculate(q: Q, p: P, a: A): Double
   
  def calculate(sc: SparkContext, evalDataSet: Seq[(EI, RDD[(Q, P, A)])])
  : Double = {
    // TODO(yipjustin): Parallelize
    val r: Seq[(Double, Long)] = evalDataSet
    .map { case (_, qpaRDD) => 
      val s = qpaRDD.map { case (q, p, a) => calculate(q, p, a) }.reduce(_ + _)
      val c = qpaRDD.count
      (s, c)
    }

    (r.map(_._1).sum / r.map(_._2).sum)
  }
}

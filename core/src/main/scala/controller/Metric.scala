package io.prediction.controller

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.reflect._
import scala.reflect.runtime.universe._
import grizzled.slf4j.Logger

abstract class Metric[EI, Q, P, A, R](implicit rOrder: Ordering[R]) 
extends Serializable {
  def header: String 
  def calculate(sc: SparkContext, evalDataSet: Seq[(EI, RDD[(Q, P, A)])]): R 

  def compare(r0: R, r1: R): Int = rOrder.compare(r0, r1)
}

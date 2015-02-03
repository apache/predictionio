package io.prediction.core

import io.prediction.controller.Utils

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import scala.reflect._

abstract class BaseEngine[EI, Q, P, A] extends Serializable {
  def train(sc: SparkContext): Seq[Any]
  //def eval(
}






class TestEngine extends BaseEngine[Int, Int, Int, Int] {
  def train(sc: SparkContext): Seq[Int] = Seq(1,2,3)

  def test(sc: SparkContext): RDD[Int] = sc.parallelize(Seq(1,3,5))
}


package io.prediction.engines.test

import com.github.nscala_time.time.Imports.DateTime

import io.prediction.core.BaseEngine
//import io.prediction.core.AbstractEngine
import io.prediction.DefaultServer
import io.prediction.DefaultCleanser
import io.prediction.SparkDefaultCleanser

import io.prediction.workflow.SparkWorkflow
//import io.prediction.workflow.EvaluationWorkflow

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

object Run {
  def a(sc: SparkContext): RDD[Int] = {
    sc.parallelize(Array(1,2,3,4,5))
  }
  
  def b(sc: SparkContext): RDD[(Int, Int)] = {
    sc.parallelize(Array((1,3), (2,4), (1,10), (5, 4)))
  }

  def c(sc: SparkContext): RDD[_] = {
    val x: RDD[(Int, Int)] = a(sc).map(e => (e, e+1))
    x
  }

  def d(sc: SparkContext): RDD[_] = {
    val x: RDD[Int] = a(sc).map(_ + 1)
    x
  }

  //def e(sc: SparkContext): RDD[Tuple2[_,_]] = {
  def e(sc: SparkContext): RDD[(_,_)] = {
    //c(sc)
    val x: RDD[(_, _)] = a(sc).map(e => (e, e+1))
    x
    //a(sc).map(e => (e, e+1))
  }

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName(s"PredictionIO: A")
    conf.set("spark.local.dir", "~/tmp/spark")
    conf.set("spark.executor.memory", "8g")

    val sc = new SparkContext(conf)

    val x: RDD[_] = a(sc)
    x.collect.foreach{println}

    val y: RDD[_] = b(sc)
    y.collect.foreach{println}
    
    val z: RDD[_] = c(sc)
    z.collect.foreach{println}

    val zz: RDD[_] = z.map{ e => {
      val f = e.asInstanceOf[Tuple2[_, _]]
      s"${f._1} -> ${f._2}"
    }}
    zz.collect.foreach{println}

    val zzz: RDD[Tuple2[_,_]] = e(sc)
    zzz.collect.foreach{ e => println(s"${e._1} -> ${e._2}") }
  }
}

package io.prediction.workflow

import io.prediction.core.Doer
import scala.language.existentials

import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseEngine
import io.prediction.java.JavaUtils

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

import io.prediction.core._
import io.prediction._

import org.apache.spark.rdd.RDD

import scala.reflect.Manifest

import io.prediction.java._

object APIDebugWorkflow {
  def run[
      DSP <: BaseParams,
      DUP <: BaseParams,
      PP <: BaseParams,
      TD,
      PD,
      F,
      A](
    batch: String = "",
    dataSourceClass: Class[_ <: BaseDataSource[DSP, DUP, TD, F, A]] = null,
    dataSourceParams: BaseParams = null,
    preparatorClass: Class[_ <: BasePreparator[PP, TD, PD]] = null,
    preparatorParams: BaseParams = null
  ) {
    println("APIDebugWorkflow.run")
    println("Start spark context")
    val sc = WorkflowContext()

    if (dataSourceClass == null || dataSourceParams == null) {
      println("Dataprep Class or Params is null. Stop here");
      return
    }
    
    println("Data Source")
    val dataSource = Doer(dataSourceClass, dataSourceParams)

    val evalParamsDataMap
    : Map[EI, (DUP, TD, RDD[(F, A)])] = dataSource
      .readBase(sc)
      .zipWithIndex
      .map(_.swap)
      .toMap

    val localParamsSet: Map[EI, DUP] = evalParamsDataMap.map { 
      case(ei, e) => (ei -> e._1)
    }

    val evalDataMap: Map[EI, (TD, RDD[(F, A)])] = evalParamsDataMap.map {
      case(ei, e) => (ei -> (e._2, e._3))
    }

    println(s"Number of training set: ${localParamsSet.size}")

    evalDataMap.foreach{ case (ei, data) => {
      val (trainingData, validationData) = data
      println(s"TrainingData $ei")
      println(DebugWorkflow.debugString(trainingData))
      println(s"ValidationData $ei")
      validationData.collect.map(DebugWorkflow.debugString).foreach(println)
    }}

    println("Data source complete")
    
    if (preparatorClass == null) {
      println("Preparator is null. Stop here")
      return
    }

    println("Preparator")
    val preparator = Doer(preparatorClass, preparatorParams)
    
    val evalPreparedMap: Map[EI, PD] = evalDataMap
    .map{ case (ei, data) => (ei, preparator.prepareBase(sc, data._1)) }

    evalPreparedMap.foreach{ case (ei, pd) => {
      println(s"Prepared $ei")
      println(DebugWorkflow.debugString(pd))
    }}
  
    println("Preparator complete")
  }


}


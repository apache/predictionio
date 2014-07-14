package io.prediction.workflow

import io.prediction.core.Doer
import scala.language.existentials

import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseEngine
import io.prediction.core.BaseAlgorithm2
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
      Q,
      P,
      A](
    batch: String = "",
    dataSourceClass: Class[_ <: BaseDataSource[DSP, DUP, TD, Q, A]] = null,
    dataSourceParams: BaseParams = null,
    preparatorClass: Class[_ <: BasePreparator[PP, TD, PD]] = null,
    preparatorParams: BaseParams = null,
    algorithmClassMap: 
      Map[String, Class[_ <: BaseAlgorithm2[_ <: BaseParams, PD, _, Q, P]]] = null,
    algorithmParamsList: Seq[(String, BaseParams)] = null
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
    : Map[EI, (DUP, TD, RDD[(Q, A)])] = dataSource
      .readBase(sc)
      .zipWithIndex
      .map(_.swap)
      .toMap

    val localParamsSet: Map[EI, DUP] = evalParamsDataMap.map { 
      case(ei, e) => (ei -> e._1)
    }

    val evalDataMap: Map[EI, (TD, RDD[(Q, A)])] = evalParamsDataMap.map {
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
    
    if (algorithmClassMap == null) {
      println("Algo is null. Stop here")
      return
    }

    println("Algo model construction")
    // fake algo map.
    //val algoMap = Map("" -> algo)
    //val algoParamsList = Seq(("", algoParams))

    // Instantiate algos
    val algoInstanceList: Array[BaseAlgorithm2[_, PD, _, Q, P]] =
    algorithmParamsList
      .map { 
        case (algoName, algoParams) => 
          Doer(algorithmClassMap(algoName), algoParams)
      }
      .toArray

    // Model Training
    // Since different algo can have different model data, have to use Any.
    // We need to use par here. Since this process allows algorithms to perform
    // "Actions" 
    // (see https://spark.apache.org/docs/latest/programming-guide.html#actions)
    // on RDDs. Meaning that algo may kick off a spark pipeline to for training.
    // Hence, we parallelize this process.
    val evalAlgoModelMap: Map[EI, Seq[(AI, Any)]] = evalPreparedMap
    .par
    .map { case (ei, preparedData) => {

      val algoModelSeq: Seq[(AI, Any)] = algoInstanceList
      .zipWithIndex
      .map { case (algo, index) => {
        val model: Any = algo.trainBase(sc, preparedData)
        (index, model)
      }}

      (ei, algoModelSeq)
    }}
    .seq
    .toMap

    evalAlgoModelMap.map{ case(ei, aiModelSeq) => {
      aiModelSeq.map { case(ai, model) => {
        println(s"Model ei: $ei ai: $ei")
        println(DebugWorkflow.debugString(model))
      }}
    }}
  }


}


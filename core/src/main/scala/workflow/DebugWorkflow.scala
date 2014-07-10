package io.prediction.workflow

import scala.language.existentials

import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseEngine

import com.github.nscala_time.time.Imports.DateTime

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.FileInputStream
import java.io.ObjectInputStream

import io.prediction.core._
import io.prediction._

import org.apache.spark.rdd.RDD

import scala.reflect.Manifest

object WorkflowContext {
  def apply(batch: String = ""): SparkContext = {
    val conf = new SparkConf().setAppName(s"PredictionIO: $batch")
    conf.set("spark.local.dir", "~/tmp/spark")
    conf.set("spark.executor.memory", "8g")

    val sc = new SparkContext(conf)
    return sc
  }
}

object DebugWorkflow {
  type EI = Int  // Evaluation Index
  type AI = Int  // Algorithm Index
  type FI = Long // Feature Index

  def debugString[D](data: D): String = {
    val s: String = data match {
      case rdd: RDD[_] => {
        //println("RDD[_]")
        debugString(rdd.collect)
      }
      case array: Array[_] => {
        //println("Array[_]")
        "[" + array.map(debugString).mkString(",") + "]"
      }
      case d: AnyRef => {
        //println("AnyRef")
        d.toString
      }
    }
    s
    //println(s)
  }

  def run[
      EDP <: BaseParams : Manifest,
      TDP <: BaseParams : Manifest,
      VDP <: BaseParams : Manifest,
      CP <: BaseParams: Manifest,
      AP <: BaseParams: Manifest,
      TD: Manifest, 
      F: Manifest,
      A: Manifest,
      CD: Manifest,
      M: Manifest,
      P: Manifest] (
    batch: String = "",
    dataPrep: BaseDataPreparator[EDP, TDP, VDP, TD, F, A] = null,
    cleanser: BaseCleanser[TD, CD, CP] = null,
    algo: BaseAlgorithm[CD, F, P, _, _ <: BaseParams] = null,
    evalDataParams: BaseParams = null,
    cleanserParams: BaseParams = null,
    algoParams: BaseParams = null
  ) {
    
    println("DebugWorkflow.run")
    println("Start spark context")
    val sc = WorkflowContext(batch)

    println("Data preparation")
    // Data Prep
    val evalParamsDataMap
    : Map[EI, (TDP, VDP, TD, RDD[(F, A)])] = dataPrep
      .prepareBase(sc, evalDataParams)

    val localParamsSet: Map[EI, (TDP, VDP)] = evalParamsDataMap.map { 
      case(ei, e) => (ei -> (e._1, e._2))
    }

    val evalDataMap: Map[EI, (TD, RDD[(F, A)])] = evalParamsDataMap.map {
      case(ei, e) => (ei -> (e._3, e._4))
    }

    println(s"Number of training / validation set: ${localParamsSet.size}")

    evalDataMap.foreach{ case (ei, data) => {
      val (trainingData, validationData) = data
      println(s"TrainingData $ei")
      //println(trainingData)
      println(debugString(trainingData))
      println(s"ValidationData $ei")
      validationData.collect.foreach(println)
    }}

    println("DataPreparation complete")

    if (cleanser == null) {
      println("Cleanser is null. Stop here")
      return
    }
    
    println("Cleansing")
    cleanser.initBase(cleanserParams)

    val evalCleansedMap: Map[EI, CD] = evalDataMap
    .map{ case (ei, data) => (ei, cleanser.cleanseBase(data._1)) }

    evalCleansedMap.foreach{ case (ei, cd) => {
      println(s"Cleansed $ei")
      //println(cd)
      println(debugString(cd))
    }}

    if (algo == null) {
      println("Algo is null. Stop here")
      return
    }

    println("Algo")
    // fake algo map.
    val algoMap = Map("" -> algo)
    val algoParamsList = Seq(("", algoParams))

    // Instantiate algos
    val algoInstanceList: Array[BaseAlgorithm[CD, F, P, _, _]] = 
    algoParamsList
      .map { case (algoName, algoParams) => {
        val algo = algoMap(algoName)
        algo.initBase(algoParams)
        algo
      }}
      .toArray

    // Model Training
    // Since different algo can have different model data, have to use Any.
    val evalAlgoModelMap: Map[EI, Seq[(AI, Any)]] = evalCleansedMap
    .par
    .map { case (ei, cleansedData) => {

      val algoModelSeq: Seq[(AI, Any)] = algoInstanceList
      .zipWithIndex
      .map { case (algo, index) => {
        val model: Any = algo.trainBase(sc, cleansedData)
        (index, model)
      }}

      println(s"EI: $ei")
      algoModelSeq.foreach{ e => println(s"${e._1} ${e._2}")}

      (ei, algoModelSeq)
    }}
    .seq
    .toMap

    evalAlgoModelMap.map{ case(ei, aiModelSeq) => {
      aiModelSeq.map { case(ai, model) => {
        println(s"Model ei: $ei ai: $ei")
        println(debugString(model))
      }}
    }}

    
    println("DebugWorkflow.run completed.")

  }
}








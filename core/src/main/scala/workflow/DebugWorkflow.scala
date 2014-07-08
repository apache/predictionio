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

  def dataPrep[
      EDP <: BaseParams : Manifest,
      TDP <: BaseParams : Manifest,
      VDP <: BaseParams : Manifest,
      TD: Manifest,
      F: Manifest,
      A: Manifest](
    baseDataPrep: BaseDataPreparator[EDP, TDP, VDP, TD, F, A],
    batch: String = "",
    evalDataParams: BaseParams) {

    println("DebugWorkflow.dataPrep")

    val sc = WorkflowContext(batch)
    val dataPrep = baseDataPrep

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
      println(trainingData)
      println(s"ValidationData $ei")
      validationData.collect.foreach(println)
    }}

    println("DebugWorkflow.dataPrep complete")
  }
}


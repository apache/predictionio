package io.prediction.workflow

import scala.language.existentials

import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseEngine

import io.prediction.BaseEvaluationDataParams


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

import com.twitter.chill.Externalizer

/*
object DeploymentWorkflow {
  type EI = Int  // Evaluation Index
  type AI = Int  // Algorithm Index

  type BAlgorithm = BaseAlgorithm[
    _ <: BaseCleansedData,
    _ <: BaseFeature,
    _ <: BasePrediction,
    _ <: BaseModel,
    _ <: BaseAlgoParams]
  type BServer = BaseServer[
    _ <: BaseFeature,
    _ <: BasePrediction,
    _ <: BaseServerParams]
  type BValidator = BaseValidator[
    _ <: BaseValidationParams,
    _ <: BaseTrainingDataParams,
    _ <: BaseValidationDataParams,
    _ <: BaseFeature,
    _ <: BasePrediction,
    _ <: BaseActual,
    _ <: BaseValidationUnit,
    _ <: BaseValidationResults,
    _ <: BaseCrossValidationResults]

  def run[
      EDP <: BaseEvaluationDataParams : Manifest,
      VP <: BaseValidationParams : Manifest,
      TDP <: BaseTrainingDataParams : Manifest,
      VDP <: BaseValidationDataParams : Manifest,
      TD <: BaseTrainingData : Manifest,
      TD1 <: BaseTrainingData : Manifest,
      CD <: BaseCleansedData : Manifest,
      F <: BaseFeature : Manifest,
      F1 <: BaseFeature : Manifest,
      P <: BasePrediction : Manifest,
      P1 <: BasePrediction : Manifest,
      A <: BaseActual : Manifest,
      VU <: BaseValidationUnit : Manifest,
      VR <: BaseValidationResults : Manifest,
      CVR <: BaseCrossValidationResults : Manifest](
    batch: String,
    trainingDataParams: BaseTrainingDataParams,
    cleanserParams: BaseCleanserParams,
    algoParamsList: Seq[(String, BaseAlgoParams)],
    serverParams: BaseServerParams,
    baseEngine: BaseEngine[TD1,CD,F1,P1],
    baseEvaluator
      : BaseEvaluator[EDP,VP,TDP,VDP,TD,F,P,A,VU,VR,CVR]
    ): Seq[BaseModel] = {

    val verbose = false

    val conf = new SparkConf().setAppName(s"PredictionIO: $batch")
    conf.set("spark.local.dir", "~/tmp/spark")
    conf.set("spark.executor.memory", "8g")

    val sc = new SparkContext(conf)

    val numPartitions = 8

    val dataPrep = baseEvaluator.dataPreparatorClass.newInstance

    var trainingParamsMap: RDD[(EI, BaseTrainingDataParams)] =
      sc.parallelize(Seq((0, trainingDataParams)))

    trainingParamsMap = trainingParamsMap
      .repartition(numPartitions)

    // Prepare Training Data
    val trainingDataMap: RDD[(EI, BaseTrainingData)] =
      trainingParamsMap.mapValues(dataPrep.prepareTrainingBase)

    if (verbose) {
      trainingDataMap.collect.foreach(println)
    }

    // Cleanse Data
    val cleanser = baseEngine.cleanserClass.newInstance
    cleanser.initBase(cleanserParams)

    val cleansedMap: RDD[(EI, BaseCleansedData)] =
      trainingDataMap.mapValues(cleanser.cleanseBase)

    if (verbose) {
      cleansedMap.collect.foreach(e => println("cleansed: " + e))
    }

    // Model Training, we support multiple algo, hence have to cartesianize data
    // Array[AlgoInstance]
    val algoDummy = sc.parallelize(0 until algoParamsList.length)

    val algoInstanceList: Array[BAlgorithm] = algoParamsList
      .map{ e => {
        val (algoName, algoParams) = e
        val algo = baseEngine.algorithmClassMap(algoName).newInstance
        algo.initBase(algoParams)
        algo
      }}
      .toArray

    // (Eval, (AlgoId, Cleansed))
    val cleansedMulMap: RDD[(EI, (AI, BaseCleansedData))] = cleansedMap
      .cartesian(algoDummy)   // ((Eval, CD), Algo)
      .map(e => (e._1._1, (e._2, e._1._2)))

    // Model : (Eval, (Algo, BaseModel))
    val modelMap: RDD[(EI, (AI, BaseModel))] = cleansedMulMap
      // (Eval, (Algo, M))
      .mapValues(e => (e._1, algoInstanceList(e._1).trainBase(e._2)))

    if (verbose) {
      modelMap.collect.foreach{ case(evalId, algo) => {
        println(s"eval: $evalId, algo: ${algo._1} model: ${algo._2}")
      }}
    }

    modelMap.collect.map { case (evalId, algo) => algo._2 }
  }
}
*/

package io.prediction.workflow

/*
import io.prediction.engines.stock.LocalFileStockEvaluator
import io.prediction.engines.stock.StockEvaluator
import io.prediction.engines.stock.EvaluationDataParams
import io.prediction.engines.stock.RandomAlgoParams
import io.prediction.engines.stock.StockEngine
*/

import scala.language.existentials

import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseEngine

import io.prediction.BaseEvaluationDataParams


import io.prediction.core.AbstractDataPreparator

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

//import org.saddle._
import com.twitter.chill.Externalizer






object SparkWorkflow {
  type EI = Int  // Evaluation Index
  type AI = Int  // Algorithm Index

  def onePassPredictFunction(
    algos: Array[AbstractAlgorithm],
    server: AbstractServer,
    input: (Iterable[(AI, BaseModel)], Iterable[(BaseFeature, BaseActual)]))
  : Iterable[(BaseFeature, BasePrediction, BaseActual)] = {
    val modelIter = input._1
      val featureActualIter = input._2

      val models = modelIter.toSeq.sortBy(_._1).map(_._2) 

      featureActualIter.map{ case(feature, actual) => {
        val predictions = algos.zipWithIndex.map{ 
          case (algo, i) => algo.predictBase(models(i), feature)
        }
        val prediction = server.combineBase(feature, predictions)
        (feature, prediction, actual)
      }}
  }

  def run[
      EDP <: BaseEvaluationDataParams : Manifest,
      VP <: BaseValidationParams : Manifest,
      TDP <: BaseTrainingDataParams : Manifest,
      VDP <: BaseValidationDataParams : Manifest,
      TD <: BaseTrainingData : Manifest,
      CD <: BaseCleansedData : Manifest,
      F <: BaseFeature : Manifest,
      P <: BasePrediction : Manifest,
      A <: BaseActual : Manifest,
      VU <: BaseValidationUnit : Manifest,
      VR <: BaseValidationResults : Manifest,
      CVR <: BaseCrossValidationResults : Manifest](
    batch: String,
    evalDataParams: BaseEvaluationDataParams,
    validationParams: BaseValidationParams,
    cleanserParams: BaseCleanserParams,
    algoParamsList: Seq[(String, BaseAlgoParams)],
    serverParams: BaseServerParams,
    baseEngine: BaseEngine[TD,CD,F,P],
    baseEvaluator
      : BaseEvaluator[EDP,VP,TDP,VDP,TD,F,P,A,VU,VR,CVR]
    ): Unit = {

    val verbose = false

    val conf = new SparkConf().setAppName(s"PredictionIO: $batch")
    conf.set("spark.local.dir", "/Users/yipjustin/tmp/spark")
    conf.set("spark.executor.memory", "8g")

    val sc = new SparkContext(conf)

    val numPartitions = 8
    
    val dataPrep = baseEvaluator.dataPreparatorBaseClass.newInstance

    val localParamsSet = dataPrep
      .getParamsSetBase(evalDataParams)
      .zipWithIndex
      .map(_.swap)

    val localTrainingParamsSet = localParamsSet.map(e => (e._1, e._2._1))
    val localValidationParamsSet = localParamsSet.map(e => (e._1, e._2._2))

    var trainingParamsMap: RDD[(EI, BaseTrainingDataParams)] = 
      sc.parallelize(localTrainingParamsSet)
    var validationParamsMap: RDD[(EI, BaseValidationDataParams)] = 
      sc.parallelize(localValidationParamsSet)

    trainingParamsMap = trainingParamsMap.repartition(numPartitions)
    validationParamsMap = validationParamsMap.repartition(numPartitions)



    // ParamsSet
    val paramsMap: 
      RDD[(Int, (BaseTrainingDataParams, BaseValidationDataParams))] = 
        sc.parallelize(localParamsSet)

    // Prepare Training Data
    val trainingDataMap: RDD[(EI, BaseTrainingData)] =
      trainingParamsMap.mapValues(dataPrep.prepareTrainingBase)

    if (verbose) {
      trainingDataMap.collect.foreach(println)
    }

    // Prepare Validation Data
    val validationDataMap: RDD[(EI, (BaseFeature, BaseActual))] =
      validationParamsMap.flatMapValues(dataPrep.prepareValidationSpark)

    if (verbose) { 
      validationDataMap.collect.foreach(println)
    }


    // TODO: Cleanse Data
    val cleanser = baseEngine.cleanserBaseClass.newInstance
    cleanser.initBase(cleanserParams) 

    val cleansedMap: RDD[(EI, BaseCleansedData)] = 
      trainingDataMap.mapValues(cleanser.cleanseBase)

    if (verbose) { 
      cleansedMap.collect.foreach(e => println("cleansed: " + e))
    }

    // Model Training, we support multiple algo, hence have to cartesianize data
    // Array[AlgoInstance]
    val algoDummy = sc.parallelize(0 until algoParamsList.length)

    val algoInstanceList: Array[AbstractAlgorithm] = algoParamsList
      .map{ e => {
        val (algoName, algoParams) = e
        val algo = baseEngine.algorithmBaseClassMap(algoName).newInstance
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

    // Take a more effficient (but potentially less scalabe way
    // We cogroup all model with features, hence make prediction with all algo
    // in one pass, as well as the combine logic of server.
    // Doing this way save one reduce-stage as we don't have to join results.
    val modelFeatureGroupedMap
    : RDD[(EI, 
        (Iterable[(AI, BaseModel)], Iterable[(BaseFeature, BaseActual)]))
      ] = modelMap.cogroup(validationDataMap)
 
    val server = baseEngine.serverBaseClass.newInstance
    server.initBase(serverParams)

    // Partial function for the one pass wrapper
    val onePassPrediction = onePassPredictFunction(
      algoInstanceList, server,
      _:  (Iterable[(AI, BaseModel)], Iterable[(BaseFeature, BaseActual)]))

    val predictionMap: RDD[(EI, (BaseFeature, BasePrediction, BaseActual))] =
      modelFeatureGroupedMap.flatMapValues(onePassPrediction)

    if (verbose) {
      predictionMap.collect.foreach{ case(ei, fpa) => {
        println(s"Prediction: $ei F: ${fpa._1} P: ${fpa._2} A: ${fpa._3}")
      }}
    }


    // Validation
    val validator = baseEvaluator.validatorBaseClass.newInstance
    validator.initBase(validationParams)

    val validationUnitMap: RDD[(Int, BaseValidationUnit)]
      = predictionMap.mapValues(validator.validateSpark)

    if (verbose) {
      validationUnitMap.collect.foreach{ case(i, e) => {
        println(s"ValidationUnit: i=$i e=$e")
      }}
    }

    // Validation Results
    // First join with TrainingData
    val validationParamsUnitMap = validationUnitMap
      .groupByKey
      .join(paramsMap)
      .mapValues(_.swap)

    val validationSetMap
    : RDD[(Int, 
      ((BaseTrainingDataParams, BaseValidationDataParams), 
        BaseValidationResults))]
      = validationParamsUnitMap.mapValues(validator.validateSetSpark)

    if (verbose) {
      validationSetMap.collect.foreach{ case(i, e) => {
        println(s"ValidationResult: i=$i a=${e._1} b=${e._2}")
      }}
    }

    val crossValidationResults: RDD[BaseCrossValidationResults] =
      validationSetMap
      .coalesce(numPartitions=1)
      .glom()
      .map(validator.crossValidateSpark)

    crossValidationResults.collect.foreach{ println }
  }

}


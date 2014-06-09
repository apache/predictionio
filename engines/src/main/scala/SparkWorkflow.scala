import io.prediction.engines.stock.LocalFileStockEvaluator
import io.prediction.engines.stock.StockEvaluator
import io.prediction.engines.stock.EvaluationDataParams
import io.prediction.engines.stock.RandomAlgoParams
import io.prediction.engines.stock.StockEngine
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

import org.saddle._
import com.twitter.chill.Externalizer

object SparkWorkflow {
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
    algoParams: BaseAlgoParams,
    baseEvaluator
      : BaseEvaluator[EDP,VP,TDP,VDP,TD,F,P,A,VU,VR,CVR],
    baseEngine: BaseEngine[TD,CD,F,P]): Unit = {

    val conf = new SparkConf().setAppName(s"PredictionIO: $batch")
    conf.set("spark.local.dir", "/Users/yipjustin/tmp/spark")
    conf.set("spark.executor.memory", "1g")

    val sc = new SparkContext(conf)
    
    val dataPrep = baseEvaluator.dataPreparatorBaseClass.newInstance

    val localParamsSet = dataPrep
      .getParamsSetBase(evalDataParams)
      .zipWithIndex
      .map(_.swap)

    val localTrainingParamsSet = localParamsSet.map(e => (e._1, e._2._1))
    val localValidationParamsSet = localParamsSet.map(e => (e._1, e._2._2))

    val trainingParamsMap: RDD[(Int, BaseTrainingDataParams)] = 
      sc.parallelize(localTrainingParamsSet)
    val validationParamsMap: RDD[(Int, BaseValidationDataParams)] = 
      sc.parallelize(localValidationParamsSet)


    // ParamsSet
    val paramsMap: 
      RDD[(Int, (BaseTrainingDataParams, BaseValidationDataParams))] = 
        sc.parallelize(localParamsSet)

    // Prepare Training Data
    val trainingDataMap: RDD[(Int, BaseTrainingData)] =
      trainingParamsMap.mapValues(dataPrep.prepareTrainingBase)

    trainingDataMap.collect.foreach(println)

    // Prepare Validation Data
    val validationDataMap: RDD[(Int, (BaseFeature, BaseActual))] =
      validationParamsMap.flatMapValues(dataPrep.prepareValidationSpark)

    validationDataMap.collect.foreach(println)


    // TODO: Cleanse Data
    val cleanser = baseEngine.cleanserBaseClass.newInstance
    // init.
    val cleansedMap: RDD[(Int, BaseCleansedData)] = 
      trainingDataMap.mapValues(cleanser.cleanseBase)

    cleansedMap.collect.foreach(e => println("cleansed: " + e))

    // Model Training
    val algo = baseEngine.algorithmBaseClassMap("regression").newInstance
    algo.initBase(algoParams)

    val modelMap: RDD[(Int, BaseModel)] = cleansedMap.mapValues(algo.trainBase)

    modelMap.collect.foreach(e => println("Model: " + e))

    // Prediction
    val modelValidationMap
    : RDD[(Int, (Iterable[BaseModel], Iterable[(BaseFeature, BaseActual)]))] =
      modelMap.cogroup(validationDataMap)
   
    modelValidationMap.collect.foreach{ e => {
      val (i, l) = e
      l._2.foreach { case(a,b) => {
        println(s"fdsa: i=$i  " + a.asInstanceOf[F])
      }}
      l._1.foreach { m => println("model: " + m) }
      
    }}

    val predictionMap
      : RDD[(Int, (BaseFeature, BasePrediction, BaseActual))] =
      modelValidationMap.flatMapValues(algo.predictSpark)

    val collectedPredictionMap = predictionMap.collect
    collectedPredictionMap.foreach{ e => {
      val (i, l) = e
      val (a,b,c) = l
      println(s"X: F: $a P: $b A: $c")
    }}


    // Validation
    val validator = baseEvaluator.validatorBaseClass.newInstance
    validator.initBase(validationParams)

    val validationUnitMap: RDD[(Int, BaseValidationUnit)]
      = predictionMap.mapValues(validator.validateSpark)

    validationUnitMap.collect.foreach{ case(i, e) => {
      println(s"ValidationUnit: i=$i e=$e")
    }}

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

    validationSetMap.collect.foreach{ case(i, e) => {
      println(s"ValidationResult: i=$i a=${e._1} b=${e._2}")
    }}

    val crossValidationResults: RDD[BaseCrossValidationResults] =
      validationSetMap.glom().map(validator.crossValidateSpark)

    crossValidationResults.collect.foreach{ println }
  }

}


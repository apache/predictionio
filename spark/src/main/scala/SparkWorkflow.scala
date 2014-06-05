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
    algoParams: BaseAlgoParams,
    baseEvaluator
      : BaseEvaluator[EDP,VP,TDP,VDP,TD,F,P,A,VU,VR,CVR],
    baseEngine: BaseEngine[TD,CD,F,P]): Unit = {

    val conf = new SparkConf().setAppName(s"PredictionIO: $batch")
    val sc = new SparkContext(conf)
    
    val dataPrep = baseEvaluator.dataPreparatorBaseClass.newInstance
    //val dataPrep = baseEvaluator.dataPreparatorClass.newInstance

    val localParamsSet = dataPrep
      .getParamsSetBase(evalDataParams)
      .zipWithIndex
      .map(_.swap)

    val localTrainingParamsSet = localParamsSet.map(e => (e._1, e._2._1))
    val localValidationParamsSet = localParamsSet.map(e => (e._1, e._2._2))

    val trainingParamsMap: RDD[(Int, TDP)] = 
      sc.parallelize(localTrainingParamsSet)
    val validationParamsMap: RDD[(Int, VDP)] = 
      sc.parallelize(localValidationParamsSet)

    //trainingParamsSet.foreach(println)
    //println(trainingParamsSet.first)

    // Prepare Training Data
    val trainingDataMap: RDD[(Int, TD)] =
      trainingParamsMap.mapValues(dataPrep.prepareTrainingBase)

    trainingDataMap.foreach(println)

    // Prepare Validation Data
    val validationDataMap: RDD[(Int, (F, A))] =
      validationParamsMap.flatMapValues(dataPrep.prepareValidationSpark)

    validationDataMap.foreach(println)

    // TODO: Cleanse Data

    // Model Training
    //val algo = baseEngine.algorithmBaseClassMap("regression").newInstance
    val algo = baseEngine.algorithmBaseClassMap("random").newInstance
    algo.initBase(algoParams)

    val modelMap: RDD[(Int, BaseModel)] = 
      trainingDataMap.mapValues(algo.trainBase)

    modelMap.foreach(println)

    // Prediction
    val modelValidationMap: RDD[(Int, (Iterable[BaseModel], Iterable[(F,A)]))] =
      trainingDataMap.cogroup(validationDataMap)

    val predictionMap: RDD[(Int, Iterable[(F, A, P)])] =
      modelValidationMap.mapValues(algo.predictSpark)
      
    predictionMap.foreach(println)


    //val trainingSet = trainingParamsSet.map(dataPrep.prepareTrainingBase)

    /*
    localParamsSet.map { params => {
      val (trainingParams, validationParams) = params

      val trainingParamsSet = sc.parallelize(Array(trainingParams))

      val trainingSet = trainingParamsSet.map(dataPrep.prepareTrainingBase)

    }}
    */

    /*
    val paramsSet = sc.parallelize(localParamsSet)

    paramsSet.foreach(println)
    
    val trainingParamsSet = paramsSet.map(_._1)

    val trainingSet = trainingParamsSet.map(dataPrep.prepareTrainingBase)

    println("training set")
    println(trainingSet.first)
    trainingSet.foreach(println)

    val validationParamsSet = paramsSet.map(_._2)
    val validationSet = validationParamsSet.flatMap(
      dataPrep.prepareValidationBaseSpark)

    println("validation set")
    println(validationSet.first)
    validationSet.foreach(println)
    */
  }

}


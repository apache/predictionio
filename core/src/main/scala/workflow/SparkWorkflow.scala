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

object SparkWorkflow {
  type EI = Int  // Evaluation Index
  type AI = Int  // Algorithm Index

  //type BTD = BaseTrainingData
  type BTDP = BaseTrainingDataParams
  type BVDP = BaseValidationDataParams
  type BF = BaseFeature
  type BA = BaseActual
  type BCD = BaseCleansedData


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

      //TD <: BaseTrainingData : Manifest,
  class AlgoServerWrapper(val algos: Array[BAlgorithm], val server: BServer)
  extends Serializable {
    def onePassPredict(
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
  }
  
  class ValidatorWrapper(val validator: BValidator) extends Serializable {
    def validateSet(input: ((BTDP, BVDP), Iterable[BaseValidationUnit]))
      : ((BTDP, BVDP), BaseValidationResults) = {
      val results = validator.validateSetBase(
        input._1._1, input._1._2, input._2.toSeq)
      (input._1, results)
    }

    def crossValidate(
      input: Array[
        ((BTDP, BVDP), BaseValidationResults)
      ]): BaseCrossValidationResults = {
      // maybe sort them.
      val data = input.map(e => (e._1._1, e._1._2, e._2))
      validator.crossValidateBase(data)
    }
  }

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
    evalDataParams: BaseEvaluationDataParams,
    validationParams: BaseValidationParams,
    cleanserParams: BaseCleanserParams,
    algoParamsList: Seq[(String, BaseAlgoParams)],
    serverParams: BaseServerParams,
    baseEngine: BaseEngine[TD1,CD,F1,P1],
    baseEvaluator
      : BaseEvaluator[EDP,VP,TDP,VDP,TD,F,P,A,VU,VR,CVR]
    ): (Seq[(BaseTrainingDataParams, BaseValidationDataParams, BaseValidationResults)], BaseCrossValidationResults) = {

    val verbose = true

    val conf = new SparkConf().setAppName(s"PredictionIO: $batch")
    conf.set("spark.local.dir", "~/tmp/spark")
    conf.set("spark.executor.memory", "8g")

    val sc = new SparkContext(conf)

    val numPartitions = 4

    val dataPrep = baseEvaluator.dataPreparatorClass.newInstance

    val localParamsSet
    : Map[EI, (BaseTrainingDataParams, BaseValidationDataParams)] = dataPrep
      .getParamsSetBase(evalDataParams)
      .zipWithIndex
      .map(_.swap)
      .toMap

    // Data Prep
    // Now we cast as actual class.... not sure if this is the right way...
    //val evalDataMap: Map[EI, (TD, RDD[(F, A)])] = localParamsSet
    //val evalDataMap: Map[EI, (BaseTrainingData, RDD[(F, A)])] = localParamsSet
    val evalDataMap
    : Map[EI, (BaseTrainingData, RDD[(BaseFeature, BaseActual)])] = localParamsSet
    .map{ case (ei, localParams) => {
      val (localTrainingParams, localValidationParams) = localParams

      val trainingData = dataPrep.prepareTrainingBase(sc, localTrainingParams)
      val validationData = dataPrep.prepareValidationBase(sc, localValidationParams)
      (ei, (trainingData, validationData))
    }}
    .toMap

    if (verbose) {
      evalDataMap.foreach{ case (ei, data) => {
        val (trainingData, validationData) = data
        println(s"TrainingData $ei")
        println(trainingData)
        println(s"ValidationData $ei")
        validationData.collect.foreach(println)
      }}
    }
   
    // Cleansing
    val cleanser = baseEngine.cleanserClass.newInstance
    //val cleanser = cleanserClass.newInstance
    cleanser.initBase(cleanserParams)

    val evalCleansedMap: Map[EI, BaseCleansedData] = evalDataMap
    .map{ case (ei, data) => (ei, cleanser.cleanseBase(data._1)) }

    if (verbose) {
      evalCleansedMap.foreach{ case (ei, cd) => {
        println(s"Cleansed $ei")
        println(cd)
      }}
    }

    // Model Training
    val algoInstanceList: Array[BAlgorithm] = algoParamsList
      .map { case (algoName, algoParams) => {
        val algo = baseEngine.algorithmClassMap(algoName).newInstance
        algo.initBase(algoParams)
        algo
      }}
      .toArray

    val evalAlgoModelMap: Map[EI, RDD[(AI, BaseModel)]] = evalCleansedMap
    .map { case (ei, cleansedData) => {

      val algoModelSeq: Seq[RDD[(AI, BaseModel)]] = algoInstanceList
      .zipWithIndex
      .map { case (algo, index) => {
        val model: RDD[BaseModel] = algo.trainBase(cleansedData)
        model.map(e => (index, e))
      }}
      
      (ei, sc.union(algoModelSeq) )
    }}
    .toMap

    if (verbose) {
      evalAlgoModelMap.foreach{ case (ei, algoModel) => {
        println(s"Model: $ei $algoModel")
      }}
    }
    
    val server = baseEngine.serverClass.newInstance
    //val server = serverClass.newInstance
    server.initBase(serverParams)

    // Prediction
    // Take a more effficient (but potentially less scalabe way
    // We cogroup all model with features, hence make prediction with all algo
    // in one pass, as well as the combine logic of server.
    // Doing this way save one reduce-stage as we don't have to join results.
    val evalPredictionMap
    : Map[EI, RDD[(BaseFeature, BasePrediction, BaseActual)]] = evalDataMap
    .map { case (ei, data) => {
      val validationData: RDD[(Int, (BaseFeature, BaseActual))] = data._2
        .map(e => (0, e))
      val algoModel: RDD[(Int, (AI, BaseModel))] = evalAlgoModelMap(ei)
        .map(e => (0, e))

      // group the two things together.
      val d = algoModel.cogroup(validationData).values

      val algoServerWrapper = new AlgoServerWrapper(algoInstanceList, server)
      (ei, d.flatMap(algoServerWrapper.onePassPredict))
    }}
    .toMap

    if (verbose) {
      evalPredictionMap.foreach{ case(ei, fpa) => {
        println(s"Prediction $ei $fpa")
      }}
    }

    // Validation Unit
    val validator = baseEvaluator.validatorClass.newInstance
    validator.initBase(validationParams)


    val evalValidationUnitMap: Map[Int, RDD[BaseValidationUnit]] =
      evalPredictionMap.mapValues(_.map(validator.validateBase))

    if (verbose) {
      evalValidationUnitMap.foreach{ case(i, e) => {
        println(s"ValidationUnit: i=$i e=$e")
      }}
    }


    // Validation Set
    val validatorWrapper = new ValidatorWrapper(validator)

    val evalValidationResultsMap
    : Map[EI, RDD[((BTDP, BVDP), BaseValidationResults)]] = evalValidationUnitMap
    .map{ case (ei, validationUnits) => {
      val validationResults
      : RDD[((BTDP, BVDP), BaseValidationResults)] = validationUnits
        .coalesce(numPartitions=1)
        .glom()
        .map(e => (localParamsSet(ei), e.toIterable))
        .map(validatorWrapper.validateSet)

      (ei, validationResults)
    }}

    if (verbose) {
      evalValidationResultsMap.foreach{ case(ei, e) => {
        println(s"ValidationResults $ei $e")
      }}
    }

    val cvInput = evalValidationResultsMap
      .flatMap { case (i, e) => e.collect }
      .map{ case (p, e) => (p._1, p._2, e) }
      .toSeq

    val crossValidationResults: RDD[BaseCrossValidationResults] = sc
      .union(evalValidationResultsMap.values.toSeq)
      .coalesce(numPartitions=1)
      .glom()
      .map(validatorWrapper.crossValidate)

    val cvOutput = crossValidationResults.collect

    cvOutput foreach { println }

    (cvInput, cvOutput(0))
  }
}

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
  type BTD = BaseTrainingData


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
    _,_,_,_,_,_]

  class AlgoServerWrapper[NF, NP, NA, NCD](
      //val algos: Array[BAlgorithm], 
      val algos: Array[BaseAlgorithm[NCD,NF,NP,_,_]], 
      //val server: BServer)
      val server: BaseServer[NF, NP, _])
  extends Serializable {

    def onePassPredict[F, P, A](
      //input: (Iterable[(AI, BaseModel)], Iterable[(BaseFeature, BaseActual)]))
      input: (Iterable[(AI, Any)], Iterable[(F, A)]))
    : Iterable[(F, P, A)] = {
      val modelIter = input._1
      val featureActualIter = input._2

      val models = modelIter.toSeq.sortBy(_._1).map(_._2)

      featureActualIter.map{ case(feature, actual) => {
        val nFeature = feature.asInstanceOf[NF]

        val predictions = algos.zipWithIndex.map{
          case (algo, i) => algo.predictBase(
            models(i),
            nFeature)
        }
        val prediction = server.combineBase(
          nFeature,
          predictions)

        (feature, prediction.asInstanceOf[P], actual)
      }}
    }
  }
//<<<<<<< HEAD
  
  class ValidatorWrapper[VR, CVR <: AnyRef](
    //val validator: BValidator) extends Serializable {
    val validator: BaseValidator[_,_,_,_,_,_,_,VR,CVR]) extends Serializable {
    //def validateSet(input: ((BTDP, BVDP), Iterable[BaseValidationUnit]))
    //  : ((BTDP, BVDP), BaseValidationResults) = {
    def validateSet(input: ((BTDP, BVDP), Iterable[Any]))
      : ((BTDP, BVDP), VR) = {
//=======

//  class ValidatorWrapper(val validator: BValidator) extends Serializable {
//    def validateSet(input: ((BTDP, BVDP), Iterable[BaseValidationUnit]))
//      : ((BTDP, BVDP), BaseValidationResults) = {
//>>>>>>> master
      val results = validator.validateSetBase(
        input._1._1, input._1._2, input._2.toSeq)
      (input._1, results)
    }

    def crossValidate(
      input: Array[
        //((BTDP, BVDP), BaseValidationResults)
        ((BTDP, BVDP), VR)
      ]): CVR = {
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
      TD: Manifest,
      NTD : Manifest,
      NCD : Manifest,
      F : Manifest,
      NF : Manifest,
      P : Manifest,
      NP : Manifest,
      A : Manifest,
      VU : Manifest,
      VR : Manifest,
      CVR <: AnyRef : Manifest](
    batch: String,
    evalDataParams: BaseEvaluationDataParams,
    validationParams: BaseValidationParams,
    cleanserParams: BaseCleanserParams,
    algoParamsList: Seq[(String, BaseAlgoParams)],
    serverParams: BaseServerParams,
    baseEngine: BaseEngine[NTD,NCD,NF,NP],
    baseEvaluator
      : BaseEvaluator[EDP,VP,TDP,VDP,TD,F,P,A,VU,VR,CVR]
//<<<<<<< HEAD
/*
    ): (
    Seq[(
      BaseTrainingDataParams, 
      BaseValidationDataParams, 
      VR)], 
    CVR) = {
*/
//=======
    ): (Array[Array[Any]], 
    Seq[(BaseTrainingDataParams, BaseValidationDataParams, VR)], 
    CVR) = {
    //): (Array[Array[BaseModel]], Seq[(BaseTrainingDataParams, BaseValidationDataParams, BaseValidationResults)], BaseCrossValidationResults) = {
//>>>>>>> master
    // Add a flag to disable parallelization.
    val verbose = true

    val conf = new SparkConf().setAppName(s"PredictionIO: $batch")
    conf.set("spark.local.dir", "~/tmp/spark")
    conf.set("spark.executor.memory", "8g")

    val sc = new SparkContext(conf)

    val dataPrep = baseEvaluator.dataPreparatorClass.newInstance

    // Data Prep
    val evalParamsDataMap
    : Map[EI, (BTDP, BVDP, TD, RDD[(F, A)])] = dataPrep
      .prepareBase(sc, evalDataParams)

    val localParamsSet: Map[EI, (BTDP, BVDP)] = evalParamsDataMap.map { 
      case(ei, e) => (ei -> (e._1, e._2))
    }

    val evalDataMap: Map[EI, (TD, RDD[(F, A)])] = evalParamsDataMap.map {
      case(ei, e) => (ei -> (e._3, e._4))
    }

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
    cleanser.initBase(cleanserParams)

    //val evalCleansedMap: Map[EI, BaseCleansedData] = evalDataMap
    val evalCleansedMap: Map[EI, NCD] = evalDataMap
    .map{ case (ei, data) => (ei, cleanser.cleanseBase(data._1)) }

    if (verbose) {
      evalCleansedMap.foreach{ case (ei, cd) => {
        println(s"Cleansed $ei")
        println(cd)
      }}
    }

    // Instantiate algos
    //val algoInstanceList: Array[BAlgorithm] = algoParamsList
    val algoInstanceList: Array[BaseAlgorithm[NCD, NF, NP, _, _]] = 
    algoParamsList
      .map { case (algoName, algoParams) => {
        val algo = baseEngine.algorithmClassMap(algoName).newInstance
        algo.initBase(algoParams)
        algo
      }}
      .toArray

    // Model Training
    // Since different algo can have different model data, have to use Any.
    val evalAlgoModelMap: Map[EI, RDD[(AI, Any)]] = evalCleansedMap
    .par
    .map { case (ei, cleansedData) => {

      val algoModelSeq: Seq[RDD[(AI, Any)]] = algoInstanceList
      .zipWithIndex
      .map { case (algo, index) => {
        val model: RDD[Any] = algo
          .trainBase(sc, cleansedData)
          .map(_.asInstanceOf[Any])
        model.map(e => (index, e))
      }}

      (ei, sc.union(algoModelSeq) )
    }}
    .seq
    .toMap

    if (verbose) {
      evalAlgoModelMap.foreach{ case (ei, algoModel) => {
        println(s"Model: $ei $algoModel")
      }}
    }

    val models = evalAlgoModelMap.values.toArray.map { rdd =>
      rdd.collect.map { p =>
        p._2
      }.toArray
    }

    val server = baseEngine.serverClass.newInstance
    server.initBase(serverParams)

    // Prediction
    // Take a more effficient (but potentially less scalabe way
    // We cogroup all model with features, hence make prediction with all algo
    // in one pass, as well as the combine logic of server.
    // Doing this way save one reduce-stage as we don't have to join results.
    val evalPredictionMap
    //: Map[EI, RDD[(BaseFeature, BasePrediction, BaseActual)]] = evalDataMap
    : Map[EI, RDD[(F, P, A)]] = evalDataMap
    .map { case (ei, data) => {
      val validationData: RDD[(Int, (F, A))] = data._2
        .map(e => (0, e))
      val algoModel: RDD[(Int, (AI, Any))] = evalAlgoModelMap(ei)
        .map(e => (0, e))

      // group the two things together.
      val d = algoModel.cogroup(validationData).values

      val algoServerWrapper = new AlgoServerWrapper[NF, NP, A, NCD](
        algoInstanceList, server)
      (ei, d.flatMap(algoServerWrapper.onePassPredict[F, P, A]))
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

    /*
    val evalValidationUnitMap: Map[Int, RDD[BaseValidationUnit]] =
      evalPredictionMap
        .mapValues(rdd => rdd.map(
          e => (
            e._1.asInstanceOf[BaseFeature],
            e._2.asInstanceOf[BasePrediction],
            e._3.asInstanceOf[BaseActual])))
        .mapValues(_.map(validator.validateBase))
    */

    val evalValidationUnitMap: Map[Int, RDD[VU]] =
      evalPredictionMap
        /*
        .mapValues(rdd => rdd.map(
          e => (
            e._1.asInstanceOf[BaseFeature],
            e._2.asInstanceOf[BasePrediction],
            e._3.asInstanceOf[BaseActual])))
        */
        .mapValues(_.map(validator.validateBase))

    if (verbose) {
      evalValidationUnitMap.foreach{ case(i, e) => {
        println(s"ValidationUnit: i=$i e=$e")
      }}
    }

    // Validation Set
    val validatorWrapper = new ValidatorWrapper(validator)

    val evalValidationResultsMap
    : Map[EI, RDD[((BTDP, BVDP), VR)]] = evalValidationUnitMap
    .map{ case (ei, validationUnits) => {
      val validationResults
      : RDD[((BTDP, BVDP), VR)] = validationUnits
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

    val crossValidationResults: RDD[CVR] = sc
      .union(evalValidationResultsMap.values.toSeq)
      .coalesce(numPartitions=1)
      .glom()
      .map(validatorWrapper.crossValidate)

    val cvOutput = crossValidationResults.collect

    cvOutput foreach { println }

    (models, cvInput, cvOutput(0))
  }
}

package io.prediction.workflow

import grizzled.slf4j.Logging

import scala.language.existentials

//import io.prediction.core.BaseEvaluator
//import io.prediction.core.BaseEngine
//import io.prediction.java.JavaUtils

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

//import io.prediction.core._
//import io.prediction._

import org.apache.spark.rdd.RDD

import scala.reflect.Manifest

//import io.prediction.java._

// FIXME: move to better location.
object WorkflowContext extends Logging {
  def apply(batch: String = "", env: Map[String, String] = Map()): SparkContext = {
    val conf = new SparkConf().setAppName(s"PredictionIO: $batch")
    info(s"Environment received: ${env}")
    env.map(kv => conf.setExecutorEnv(kv._1, kv._2))
    info(s"SparkConf executor environment: ${conf.getExecutorEnv}")
    conf.set("spark.local.dir", "~/tmp/spark")
    conf.set("spark.executor.memory", "8g")

    val sc = new SparkContext(conf)
    return sc
  }
}

object DebugWorkflow {
  def debugString[D](data: D): String = {
    val s: String = data match {
      case rdd: RDD[_] => {
        debugString(rdd.collect)
      }
      case array: Array[_] => {
        "[" + array.map(debugString).mkString(",") + "]"
      }
      case d: AnyRef => {
        d.toString
      }
      case null => "null"
    }
    s
  }
}

/*
object JavaDebugWorkflow {
  @deprecated("Use API Debug", "20140716")
  def run[
      EDP <: BaseParams ,
      VP <: BaseParams ,
      TDP <: BaseParams ,
      VDP <: BaseParams ,
      CP <: BaseParams,
      AP <: BaseParams,
      SP <: BaseParams,
      TD,
      F,
      A,
      CD,
      M,
      P,
      VU ,
      VR ,
      CVR <: AnyRef ](
    batch: String = "",
    dataPrepClass:
      Class[_  <: JavaLocalDataPreparator[EDP, TDP, VDP, TD, F, A]] = null,
    cleanserClass: Class[_ <: JavaLocalCleanser[TD, CD, CP]] = null,
    algoClassMap:
      JMap[String, Class[_ <: JavaLocalAlgorithm[CD, F, P, _, _ <: BaseParams]]] = null,
    serverClass: Class[_ <: JavaServer[F, P, SP]] = null,
    validatorClass:
      Class[_ <: JavaValidator[VP, TDP, VDP, F, P, A, VU, VR, CVR]] = null,
    evalDataParams: BaseParams = null,
    cleanserParams: BaseParams = null,
    algoParamsList: JIterable[(String, BaseParams)] = null,
    serverParams: BaseParams = null,
    validatorParams: BaseParams = null) {
    println("JavaDebug")

    val scalaAlgoClassMap = (if (algoClassMap == null) null
      else Map(algoClassMap.toSeq:_ *))

    val scalaAlgoParamsList = (
      if (algoParamsList == null) null
      else algoParamsList.toSeq)

    DebugWorkflow.run(
      batch,
      dataPrepClass,
      cleanserClass,
      scalaAlgoClassMap,
      serverClass,
      validatorClass,
      evalDataParams,
      cleanserParams,
      scalaAlgoParamsList,
      serverParams,
      validatorParams)(
      JavaUtils.fakeManifest[EDP],
      JavaUtils.fakeManifest[VP],
      JavaUtils.fakeManifest[TDP],
      JavaUtils.fakeManifest[VDP],
      JavaUtils.fakeManifest[CP],
      JavaUtils.fakeManifest[AP],
      JavaUtils.fakeManifest[SP],
      JavaUtils.fakeManifest[RDD[TD]],
      JavaUtils.fakeManifest[F],
      JavaUtils.fakeManifest[A],
      JavaUtils.fakeManifest[RDD[CD]],
      JavaUtils.fakeManifest[M],
      JavaUtils.fakeManifest[P],
      JavaUtils.fakeManifest[VU],
      JavaUtils.fakeManifest[VR],
      JavaUtils.fakeManifest[CVR]
    )
  }
}

object DebugWorkflow {
  def debugString[D](data: D): String = {
    val s: String = data match {
      case rdd: RDD[_] => {
        debugString(rdd.collect)
      }
      case array: Array[_] => {
        "[" + array.map(debugString).mkString(",") + "]"
      }
      case d: AnyRef => {
        d.toString
      }
      case null => "null"
    }
    s
  }

  // Probably CP, AP, SP don't require Manifest
  @deprecated("Use API Debug", "20140716")
  def run[
      EDP <: BaseParams : Manifest,
      VP <: BaseParams : Manifest,
      TDP <: BaseParams : Manifest,
      VDP <: BaseParams : Manifest,
      CP <: BaseParams: Manifest,
      AP <: BaseParams: Manifest,
      SP <: BaseParams: Manifest,
      TD: Manifest,
      F: Manifest,
      A: Manifest,
      CD: Manifest,
      M: Manifest,
      P: Manifest,
      VU : Manifest,
      VR : Manifest,
      CVR <: AnyRef : Manifest](
    batch: String = "",
    dataPrepClass:
      Class[_  <: BaseDataPreparator[EDP, TDP, VDP, TD, F, A]] = null,
    cleanserClass: Class[_ <: BaseCleanser[TD, CD, CP]] = null,
    algoClassMap:
      Map[String, Class[_ <: BaseAlgorithm[CD, F, P, _, _ <: BaseParams]]] = null,
    serverClass: Class[_ <: BaseServer[F, P, SP]] = null,
    validatorClass:
      Class[_ <: BaseValidator[VP, TDP, VDP, F, P, A, VU, VR, CVR]] = null,
    evalDataParams: BaseParams = null,
    cleanserParams: BaseParams = null,
    algoParamsList: Seq[(String, BaseParams)] = null,
    serverParams: BaseParams = null,
    validatorParams: BaseParams = null
  ) {

    println("DebugWorkflow.run")
    println("Start spark context")
    val sc = WorkflowContext(batch)

    if (dataPrepClass == null) {
      println("Dataprep is null. Stop here");
      return
    }

    val dataPrep = dataPrepClass.newInstance

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

    if (cleanserClass == null) {
      println("Cleanser is null. Stop here")
      return
    }

    val cleanser = cleanserClass.newInstance

    println("Cleansing")
    cleanser.initBase(cleanserParams)

    val evalCleansedMap: Map[EI, CD] = evalDataMap
    .map{ case (ei, data) => (ei, cleanser.cleanseBase(data._1)) }

    evalCleansedMap.foreach{ case (ei, cd) => {
      println(s"Cleansed $ei")
      //println(cd)
      println(debugString(cd))
    }}

    if (algoClassMap == null) {
      println("Algo is null. Stop here")
      return
    }

    println("Algo model construction")
    // fake algo map.
    //val algoMap = Map("" -> algo)
    //val algoParamsList = Seq(("", algoParams))

    // Instantiate algos
    val algoInstanceList: Array[BaseAlgorithm[CD, F, P, _, _]] =
    algoParamsList
      .map { case (algoName, algoParams) => {
        val algo = algoClassMap(algoName).newInstance
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

    if (serverClass == null) {
      println("Server is null. Stop here")
      return
    }
    val server = serverClass.newInstance

    println("Algo prediction")

    val evalPredictionMap
    : Map[EI, RDD[(F, P, A)]] = evalDataMap.map { case (ei, data) => {
      val validationData: RDD[(F, A)] = data._2
      val algoModel: Seq[Any] = evalAlgoModelMap(ei)
        .sortBy(_._1)
        .map(_._2)

      val algoServerWrapper = new AlgoServerWrapper[F, P, A, CD](
        algoInstanceList, server, skipOpt = true, verbose = true)
      (ei, algoServerWrapper.predict[F, P, A](algoModel, validationData))
    }}
    .toMap

    evalPredictionMap.foreach{ case(ei, fpaRdd) => {
      println(s"Prediction $ei $fpaRdd")
      fpaRdd.collect.foreach{ case(f, p, a) => {
        val fs = DebugWorkflow.debugString(f)
        val ps = DebugWorkflow.debugString(p)
        val as = DebugWorkflow.debugString(a)
        println(s"F: $fs P: $ps A: $as")
      }}

    }}

    if (validatorClass == null) {
      println("Validator is null. Stop here")
      return
    }

    val validator = validatorClass.newInstance

    // Validation Unit
    //val validator = baseEvaluator.validatorClass.newInstance
    validator.initBase(validatorParams)

    val evalValidationUnitMap: Map[Int, RDD[VU]] =
      evalPredictionMap.mapValues(_.map(validator.validateBase))

    evalValidationUnitMap.foreach{ case(i, e) => {
      println(s"ValidationUnit: i=$i e=$e")
    }}

    // Validation Set
    val validatorWrapper = new ValidatorWrapper(validator)

    val evalValidationResultsMap
    : Map[EI, RDD[((TDP, VDP), VR)]] = evalValidationUnitMap
    .map{ case (ei, validationUnits) => {
      val validationResults
      : RDD[((TDP, VDP), VR)] = validationUnits
        .coalesce(numPartitions=1)
        .glom()
        .map(e => (localParamsSet(ei), e.toIterable))
        .map(validatorWrapper.validateSet)

      (ei, validationResults)
    }}

    evalValidationResultsMap.foreach{ case(ei, e) => {
      println(s"ValidationResults $ei $e")
    }}

    val crossValidationResults: RDD[CVR] = sc
      .union(evalValidationResultsMap.values.toSeq)
      .coalesce(numPartitions=1)
      .glom()
      .map(validatorWrapper.crossValidate)

    val cvOutput: Array[CVR] = crossValidationResults.collect

    cvOutput foreach { println }

    println("DebugWorkflow.run completed.")

  }
}
*/

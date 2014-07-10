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

import com.twitter.chill.Externalizer

object EvaluationWorkflow {
  // Wrapper for single algo engine
  def run[
      EDP <: BaseParams : Manifest,
      VP <: BaseParams : Manifest,
      TDP <: BaseParams : Manifest,
      VDP <: BaseParams : Manifest,
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
    singleAlgoEngine: SingleAlgoEngine[NTD,NCD,NF,NP],
    baseEvaluator: BaseEvaluator[EDP,VP,TDP,VDP,TD,F,P,A,VU,VR,CVR],
    batch: String = "",
    evalDataParams: BaseParams = null,
    validationParams: BaseParams = null,
    cleanserParams: BaseParams = null,
    algoParams: BaseParams = null,
    serverParams: BaseParams = null
    ): (Array[Array[Any]], Seq[(TDP, VDP, VR)], CVR) = {
    val r = EvaluationWorkflowImpl.run(
      batch, evalDataParams, validationParams, 
      cleanserParams, Seq(("", algoParams)), serverParams,
      singleAlgoEngine, baseEvaluator)
    println("SingleAlgoEngine")
    r
  }

  def run[
      EDP <: BaseParams : Manifest,
      VP <: BaseParams : Manifest,
      TDP <: BaseParams : Manifest,
      VDP <: BaseParams : Manifest,
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
    evalDataParams: BaseParams,
    validationParams: BaseParams,
    cleanserParams: BaseParams,
    algoParamsList: Seq[(String, BaseParams)],
    serverParams: BaseParams,
    baseEngine: BaseEngine[NTD,NCD,NF,NP],
    baseEvaluator: BaseEvaluator[EDP,VP,TDP,VDP,TD,F,P,A,VU,VR,CVR]
    ): (Array[Array[Any]], Seq[(TDP, VDP, VR)], CVR) = {
    EvaluationWorkflowImpl.run(
      batch, evalDataParams, validationParams, cleanserParams,
      algoParamsList, serverParams, baseEngine, baseEvaluator)
  }
}



object EvaluationWorkflowImpl {
  /*
  type EI = Int  // Evaluation Index
  type AI = Int  // Algorithm Index
  type FI = Long // Feature Index

  type BP = BaseParams
  */

  


  def run[
      EDP <: BaseParams : Manifest,
      VP <: BaseParams : Manifest,
      TDP <: BaseParams : Manifest,
      VDP <: BaseParams : Manifest,
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
    evalDataParams: BaseParams,
    validationParams: BaseParams,
    cleanserParams: BaseParams,
    algoParamsList: Seq[(String, BaseParams)],
    serverParams: BaseParams,
    baseEngine: BaseEngine[NTD,NCD,NF,NP],
    baseEvaluator: BaseEvaluator[EDP,VP,TDP,VDP,TD,F,P,A,VU,VR,CVR]
    ): (Array[Array[Any]], Seq[(TDP, VDP, VR)], CVR) = {
    // Add a flag to disable parallelization.
    val verbose = false

    val sc = WorkflowContext(batch)

    val dataPrep = baseEvaluator.dataPreparatorClass.newInstance

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

    val evalCleansedMap: Map[EI, NCD] = evalDataMap
    .map{ case (ei, data) => (ei, cleanser.cleanseBase(data._1)) }

    if (verbose) {
      evalCleansedMap.foreach{ case (ei, cd) => {
        println(s"Cleansed $ei")
        println(cd)
      }}
    }

    // Instantiate algos
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

    if (verbose) {
      evalAlgoModelMap.foreach{ case (ei, algoModel) => {
        println(s"Model: $ei $algoModel")
      }}
    }

    /*
    val models = evalAlgoModelMap.values.toArray.map { rdd =>
      rdd.collect.map { p =>
        p._2
      }.toArray
    }
    */

    // FIXME(yipjustin): Deployment uses this trained model. But we have to
    // handle two cases where the model is local / RDD. Fix later.
    val models = Array(Array[Any]())

    val server = baseEngine.serverClass.newInstance
    server.initBase(serverParams)

    // Prediction
    // Take a more effficient (but potentially less scalabe way
    // We cogroup all model with features, hence make prediction with all algo
    // in one pass, as well as the combine logic of server.
    // Doing this way save one reduce-stage as we don't have to join results.
    val evalPredictionMap
    : Map[EI, RDD[(F, P, A)]] = evalDataMap.map { case (ei, data) => {
      val validationData: RDD[(F, A)] = data._2
      val algoModel: Seq[Any] = evalAlgoModelMap(ei)
        .sortBy(_._1)
        .map(_._2)

      val algoServerWrapper = new AlgoServerWrapper[NF, NP, A, NCD](
        algoInstanceList, server)
      (ei, algoServerWrapper.predict[F, P, A](algoModel, validationData))
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

    val evalValidationUnitMap: Map[Int, RDD[VU]] =
      evalPredictionMap.mapValues(_.map(validator.validateBase))

    if (verbose) {
      evalValidationUnitMap.foreach{ case(i, e) => {
        println(s"ValidationUnit: i=$i e=$e")
      }}
    }

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


// skipOpt = true: use slow parallel model for prediction, requires one extra
// join stage.
class AlgoServerWrapper[NF, NP, NA, NCD](
    val algos: Array[BaseAlgorithm[NCD,NF,NP,_,_]], 
    val server: BaseServer[NF, NP, _],
    val skipOpt: Boolean = false,
    val verbose: Boolean = false)
extends Serializable {
  
  // Use algo.predictBase
  def onePassPredict[F, P, A](
    modelIter: Iterator[(AI, Any)], 
    featureActualIter: Iterator[(F, A)])
  : Iterator[(F, P, A)] = {
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

  // Use algo.predictBase
  def predictLocalModel[F, P, A](models: Seq[RDD[Any]], input: RDD[(F, A)])
  : RDD[(F, P, A)] = {
    println("predictionLocalModel")
    val sc = models.head.context
    // must have only one partition since we need all models per feature.
    // todo: duplicate indexedModel into multiple partition.
    val reInput = input.coalesce(numPartitions = 1)

    val indexedModels: Seq[RDD[(AI, Any)]] = models.zipWithIndex
      .map { case (rdd, ai) => rdd.map(m => (ai, m)) }

    val rddModel: RDD[(AI, Any)] = sc.union(indexedModels)
      .coalesce(numPartitions = 1)

    rddModel.zipPartitions(reInput)(onePassPredict[F, P, A])
  }

  // Use algo.batchPredictBase
  def predictParallelModel[F, P, A](models: Seq[Any], input: RDD[(F, A)])
  : RDD[(F, P, A)] = {
    if (verbose) { 
      println("predictionParallelModel")
    }

    // Prefixed with "i" stands for "i"ndexed
    val iInput: RDD[(FI, (F, A))] = input.zipWithUniqueId.map(_.swap)

    val iFeature: RDD[(FI, NF)] = iInput
      .map(e => (e._1, e._2._1.asInstanceOf[NF]))
    val sc = input.context

    // Each algo/model is run independely.
    val iAlgoPredictionSeq: Seq[RDD[(FI, (AI, NP))]] = models
      .zipWithIndex
      .map { case (model, ai) => {
        algos(ai)
          .batchPredictBase(model, iFeature)
          .map{ case (fi, p) => (fi, (ai, p)) }
      }}

    val iAlgoPredictions: RDD[(FI, Seq[NP])] = sc
      .union(iAlgoPredictionSeq)
      .groupByKey
      .mapValues { _.toSeq.sortBy(_._1).map(_._2) }

    
    val joined: RDD[(FI, (Seq[NP], (F, A)))] = iAlgoPredictions.join(iInput)
    
    if (verbose) {
      println("predictionParallelModel.before combine")
      joined.collect.foreach {  case(fi, (ps, (f, a))) => {
        val pstr = DebugWorkflow.debugString(ps)
        val fstr = DebugWorkflow.debugString(f)
        val astr = DebugWorkflow.debugString(a)
        //e => println(DebugWorkflow.debugString(e))
        println(s"I: $fi F: $fstr A: $astr Ps: $pstr")
      }}
    }

    val combined: RDD[(FI, (F, P, A))] = joined
    .mapValues{ case (predictions, (feature, actual)) => {
      val prediction = server.combineBase(feature, predictions)
      (feature, prediction.asInstanceOf[P], actual)
    }}

    if (verbose) {
      println("predictionParallelModel.after combine")
      combined.collect.foreach { case(fi, (f, p, a)) => {
        val fstr = DebugWorkflow.debugString(f)
        val pstr = DebugWorkflow.debugString(p)
        val astr = DebugWorkflow.debugString(a)
        println(s"I: $fi F: $fstr A: $astr P: $pstr")
      }}
    }

    combined.values
  }

  def predict[F, P, A](models: Seq[Any], input: RDD[(F, A)])
  : RDD[(F, P, A)] = {
    // We split the prediction into multiple mode.
    // If all algo support using local model, we will run against all of them
    // in one pass.
    //val someNonLocal = algos.exists(!_.isInstanceOf[LocalModelAlgorithm])
    val someNonLocal = algos
      .exists(!_.isInstanceOf[LocalModelAlgorithm[F, P, _]])

    if (!someNonLocal && !skipOpt) {
      // When algo is local, the model is the only element in RDD[M].
      val localModelAlgo = algos
        .map(_.asInstanceOf[LocalModelAlgorithm[F, P, Any]])
      val rddModels = localModelAlgo.zip(models)
        .map{ case (algo, model) => algo.getModel(model) }
      predictLocalModel[F, P, A](rddModels, input)
    } else {
      predictParallelModel[F, P, A](models, input)
    }
  }
}
  
class ValidatorWrapper[
    TDP <: BaseParams, VDP <: BaseParams, VU, VR, CVR <: AnyRef](
  val validator: BaseValidator[_,TDP,VDP,_,_,_,VU,VR,CVR]) 
extends Serializable {
  def validateSet(input: ((TDP, VDP), Iterable[VU]))
    : ((TDP, VDP), VR) = {
    val results = validator.validateSetBase(
      input._1._1, input._1._2, input._2.toSeq)
    (input._1, results)
  }

  def crossValidate(input: Array[((TDP, VDP), VR)]): CVR = {
    // maybe sort them.
    val data = input.map(e => (e._1._1, e._1._2, e._2))
    validator.crossValidateBase(data)
  }
}



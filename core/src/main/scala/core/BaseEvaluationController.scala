package io.prediction.core

import scala.reflect.Manifest
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

// FIXME(yipjustin). I am being lazy...
import io.prediction._

//    TDP <: BaseTrainingDataParams : Manifest,
    //VDP <: BaseValidationDataParams : Manifest,
    //TD <: BaseTrainingData,

abstract class BaseDataPreparator[
    EDP <: BaseEvaluationDataParams : Manifest,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    BTD, 
    F <: BaseFeature,
    A <: BaseActual]
  extends AbstractParameterizedDoer[EDP] {

  type BTDP = BaseTrainingDataParams
  type BVDP = BaseValidationDataParams
  type BF = BaseFeature
  type BA = BaseActual

  def init(params: EDP): Unit = {}

  def prepareBase(sc: SparkContext, params: BaseEvaluationDataParams)
  : Map[Int, (BTDP, BVDP, BTD, RDD[(BF, BA)])] = {
    val localParamsSet
    : Map[Int, (BaseTrainingDataParams, BaseValidationDataParams)] =
      getParamsSetBase(params)
      .zipWithIndex
      .map(_.swap)
      .toMap

    val evalDataMap
    : Map[Int, (BTD, RDD[(BF, BA)])] = localParamsSet
    .par
    .map{ case (ei, localParams) => {
      val (localTrainingParams, localValidationParams) = localParams

      val trainingData = prepareTrainingBase(sc, localTrainingParams)
      val validationData = prepareValidationBase(sc, localValidationParams)
      (ei, (trainingData, validationData))
    }}
    .seq
    .toMap

    evalDataMap.map { case(ei, e) => {
      val params = localParamsSet(ei)
      (ei, (params._1, params._2, e._1, e._2))
    }}
    .toMap
  }

  def getParamsSetBase(params: BaseEvaluationDataParams)
  : Seq[(TDP, VDP)] = getParamsSet(params.asInstanceOf[EDP])

  def getParamsSet(params: EDP): Seq[(TDP, VDP)]
 
  def prepareTrainingBase(
    sc: SparkContext,
    params: BaseTrainingDataParams): BTD

  def prepareValidationBase(
    sc: SparkContext,
    params: BaseValidationDataParams): RDD[(BaseFeature, BaseActual)] 
}


abstract class LocalDataPreparator[
    EDP <: BaseEvaluationDataParams : Manifest,
    TDP <: BaseTrainingDataParams : Manifest,
    VDP <: BaseValidationDataParams,
    TD <: BaseTrainingData : Manifest,
    F <: BaseFeature,
    A <: BaseActual]
    extends BaseDataPreparator[EDP, TDP, VDP, RDDTD[TD], F, A] {
  def prepareTrainingBase(
    sc: SparkContext,
    params: BaseTrainingDataParams): RDDTD[TD] = {
    println("LocalDataPreparator.prepareTrainingBase")
    val tdp = params.asInstanceOf[TDP]
    val sParams = sc.parallelize(Array(tdp))
    val v = sParams.map(prepareTraining)
    new RDDTD(v = v)
  }

  def prepareTraining(params: TDP): TD

  def prepareValidationBase(
    sc: SparkContext,
    params: BaseValidationDataParams): RDD[(BaseFeature, BaseActual)] = {
    val vdp = params.asInstanceOf[VDP]
    sc.parallelize(prepareValidation(vdp))
      .map(e => 
        (e._1.asInstanceOf[BaseFeature], e._2.asInstanceOf[BaseActual]))
  }
  
  def prepareValidation(params: VDP): Seq[(F, A)]
}

// In this case, TD may contain multiple RDDs
// But still, F and A cannot contain RDD
abstract class SparkDataPreparator[
    EDP <: BaseEvaluationDataParams : Manifest,
    TDP <: BaseTrainingDataParams : Manifest,
    VDP <: BaseValidationDataParams,
    TD : Manifest,
    F <: BaseFeature,
    A <: BaseActual]
    extends BaseDataPreparator[EDP, TDP, VDP, TD, F, A] {
  def prepareTrainingBase(
    sc: SparkContext,
    params: BaseTrainingDataParams): TD = {
    println("SparkDataPreparator.prepareTrainingBase")
    val tdp = params.asInstanceOf[TDP]
    prepareTraining(sc, tdp)
  }

  def prepareTraining(sc: SparkContext, params: TDP): TD

  def prepareValidationBase(
    sc: SparkContext,
    //params: BaseValidationDataParams): RDD[(F, A)] = {
    params: BaseValidationDataParams): RDD[(BaseFeature, BaseActual)] = {
    val vdp = params.asInstanceOf[VDP]
    prepareValidation(sc, vdp)
      .map(e => 
        (e._1.asInstanceOf[BaseFeature], e._2.asInstanceOf[BaseActual]))
  }
  
  def prepareValidation(sc: SparkContext, params: VDP): RDD[(F, A)]
}




  

abstract class BaseValidator[
    VP <: BaseValidationParams : Manifest,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    F <: BaseFeature,
    P <: BasePrediction,
    A <: BaseActual,
    VU <: BaseValidationUnit,
    VR <: BaseValidationResults,
    CVR <: BaseCrossValidationResults]
  extends AbstractParameterizedDoer[VP] {

  def validateSeq(predictionSeq: BasePredictionSeq)
    : BaseValidationUnitSeq = {
    val input: Seq[(F, P, A)] = predictionSeq
      .asInstanceOf[PredictionSeq[F, P, A]].data
    val output = input.map(e => validate(e._1, e._2, e._3))
    return new ValidationUnitSeq(data = output)
  }

  def validateBase(input: (BaseFeature, BasePrediction, BaseActual))
    : BaseValidationUnit = {
    validateBase(input._1, input._2, input._3)
  }
  
  def validateBase(
      feature: BaseFeature, 
      prediction: BasePrediction, 
      actual: BaseActual)
    : BaseValidationUnit = {
    validate(
      feature.asInstanceOf[F],
      prediction.asInstanceOf[P],
      actual.asInstanceOf[A])
  }

  def validate(feature: F, predicted: P, actual: A): VU

  /*
  // obsolete code
  def validateSet(
    trainingDataParams: BaseTrainingDataParams,
    validationDataParams: BaseValidationDataParams,
    validationUnitSeq: BaseValidationUnitSeq): BaseValidationParamsResults = {
    val tdp = trainingDataParams.asInstanceOf[TDP]
    val vdp = validationDataParams.asInstanceOf[VDP]

    val results = validateSet(tdp, vdp,
      validationUnitSeq.asInstanceOf[ValidationUnitSeq[VU]].data)

    return new ValidationParamsResults(tdp, vdp, results)
  }
  */

  def validateSetBase(
    trainingDataParams: BaseTrainingDataParams,
    validationDataParams: BaseValidationDataParams,
    validationUnits: Seq[BaseValidationUnit]): BaseValidationResults = {
    validateSet(
      trainingDataParams.asInstanceOf[TDP],
      validationDataParams.asInstanceOf[VDP],
      validationUnits.map(_.asInstanceOf[VU]))
  }

  def validateSet(
    trainingDataParams: TDP,
    validationDataParams: VDP,
    validationUnits: Seq[VU]): VR

  /*
  // Obsolete
  def crossValidateBase(validationParamsResultsSeq: Seq[BaseValidationParamsResults])
  : BaseCrossValidationResults = {
    val input = validationParamsResultsSeq
      .map(_.asInstanceOf[ValidationParamsResults[TDP, VDP, VR]])
      .map(e => (e.trainingDataParams, e.validationDataParams, e.data))

    crossValidate(input)
  }
  */

  def crossValidateBase(
    input: Seq[(BaseTrainingDataParams, BaseValidationDataParams,
      BaseValidationResults)]): BaseCrossValidationResults = {
    crossValidate(input.map(e => (
      e._1.asInstanceOf[TDP],
      e._2.asInstanceOf[VDP],
      e._3.asInstanceOf[VR])))
  }

  def crossValidate(validateResultsSeq: Seq[(TDP, VDP, VR)]): CVR
}

/* Evaluator */
class BaseEvaluator[
    EDP <: BaseEvaluationDataParams,
    VP <: BaseValidationParams,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    TD <: BaseTrainingData,
    F <: BaseFeature,
    P <: BasePrediction,
    A <: BaseActual,
    VU <: BaseValidationUnit,
    VR <: BaseValidationResults,
    CVR <: BaseCrossValidationResults](
  val dataPreparatorClass
    : Class[_ <: BaseDataPreparator[EDP, TDP, VDP, TD, F, A]],
  val validatorClass
    : Class[_ <: BaseValidator[VP, TDP, VDP, F, P, A, VU, VR, CVR]]) {}


class LocalEvaluator[
    EDP <: BaseEvaluationDataParams,
    VP <: BaseValidationParams,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    TD <: BaseTrainingData,
    F <: BaseFeature,
    P <: BasePrediction,
    A <: BaseActual,
    VU <: BaseValidationUnit,
    VR <: BaseValidationResults,
    CVR <: BaseCrossValidationResults](
  dataPreparatorClass
    : Class[_ <: LocalDataPreparator[EDP, TDP, VDP, TD, F, A]],
  validatorClass
    : Class[_ <: BaseValidator[VP, TDP, VDP, F, P, A, VU, VR, CVR]])
  extends BaseEvaluator[EDP, VP, TDP, VDP, RDDTD[TD], F, P, A, VU, VR, CVR](
    dataPreparatorClass, validatorClass) {}

    //TD <: BaseTrainingData,
  
class SparkEvaluator[
    EDP <: BaseEvaluationDataParams,
    VP <: BaseValidationParams,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    TD <: BaseTrainingData,
    F <: BaseFeature,
    P <: BasePrediction,
    A <: BaseActual,
    VU <: BaseValidationUnit,
    VR <: BaseValidationResults,
    CVR <: BaseCrossValidationResults](
  dataPreparatorClass
    : Class[_ <: SparkDataPreparator[EDP, TDP, VDP, TD, F, A]],
  validatorClass
    : Class[_ <: BaseValidator[VP, TDP, VDP, F, P, A, VU, VR, CVR]])
  extends BaseEvaluator[EDP, VP, TDP, VDP, TD, F, P, A, VU, VR, CVR](
    dataPreparatorClass, validatorClass) {}


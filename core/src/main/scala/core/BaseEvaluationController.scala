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
    F,
    A]
    //F <: BaseFeature,
    //A <: BaseActual]
  extends AbstractParameterizedDoer[EDP] {

  type BTDP = BaseTrainingDataParams
  type BVDP = BaseValidationDataParams
  //type BF = BaseFeature
  //type BA = BaseActual

  //def init(params: EDP): Unit = {}

  def prepareBase(sc: SparkContext, params: BaseEvaluationDataParams)
  //: Map[Int, (BTDP, BVDP, BTD, RDD[(_, _)])] = {
  : Map[Int, (BTDP, BVDP, BTD, RDD[(F, A)])] = {
    prepare(sc, params.asInstanceOf[EDP]).map { case (ei, e) => {
      //val ee = (e._1, e._2, e._3, e._4.asInstanceOf[RDD[(_,_)]])
      val ee = e
      (ei -> ee)
    }}
  }

  def prepare(sc: SparkContext, params: EDP)
  : Map[Int, (BTDP, BVDP, BTD, RDD[(F, A)])]

}


abstract class SlicedDataPreparator[
    EDP <: BaseEvaluationDataParams : Manifest,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    BTD, 
    F,
    A]
    //F <: BaseFeature,
    //A <: BaseActual]
  extends BaseDataPreparator[EDP, TDP, VDP, BTD, F, A] {

  /*
  type BTDP = BaseTrainingDataParams
  type BVDP = BaseValidationDataParams
  type BF = BaseFeature
  type BA = BaseActual
  */

  def prepare(sc: SparkContext, params: EDP)
  : Map[Int, (BTDP, BVDP, BTD, RDD[(F, A)])] = {
    val localParamsSet
    : Map[Int, (BaseTrainingDataParams, BaseValidationDataParams)] =
      getParamsSetBase(params)
      .zipWithIndex
      .map(_.swap)
      .toMap

    val evalDataMap
    : Map[Int, (BTD, RDD[(F, A)])] = localParamsSet
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
    params: BaseTrainingDataParams): BTD = {
    prepareTraining(sc, params.asInstanceOf[TDP])
  }
  
  def prepareTraining(sc: SparkContext, params: TDP): BTD
  
  def prepareValidationBase(
    sc: SparkContext,
    params: BaseValidationDataParams): RDD[(F, A)] = {
    prepareValidation(sc, params.asInstanceOf[VDP])
  }
  
  def prepareValidation(sc: SparkContext, params: VDP): RDD[(F, A)]


}


abstract class LocalDataPreparator[
    EDP <: BaseEvaluationDataParams : Manifest,
    TDP <: BaseTrainingDataParams : Manifest,
    VDP <: BaseValidationDataParams,
    TD : Manifest,
    F ,
    A ]
    extends SlicedDataPreparator[EDP, TDP, VDP, RDD[TD], F, A] {

  override
  def prepareTrainingBase(
    sc: SparkContext,
    params: BaseTrainingDataParams): RDD[TD] = {
    println("LocalDataPreparator.prepareTrainingBase")
    val tdp = params.asInstanceOf[TDP]
    prepareTraining(sc, tdp)
  }

  def prepareTraining(sc: SparkContext, tdp: TDP): RDD[TD] = {
    val sParams = sc.parallelize(Array(tdp))
    val v = sParams.map(prepareTraining)
    v
    //new RDDTD(v = v)
  }

  def prepareTraining(params: TDP): TD

  override
  def prepareValidationBase(
    sc: SparkContext,
    params: BaseValidationDataParams)
    //: RDD[(BaseFeature, BaseActual)] = {
    : RDD[(F, A)] = {
    val vdp = params.asInstanceOf[VDP]
    prepareValidation(sc, vdp)
    //sc.parallelize(prepareValidation(vdp))
      //.map(e => 
      //  (e._1.asInstanceOf[BaseFeature], e._2.asInstanceOf[BaseActual]))
  }

  def prepareValidation(sc: SparkContext, vdp: VDP): RDD[(F, A)] = {
    sc.parallelize(prepareValidation(vdp))
  }
  
  def prepareValidation(params: VDP): Seq[(F, A)]
}

// In this case, TD may contain multiple RDDs
// But still, F and A cannot contain RDD
    //TD : Manifest,
    //F <: BaseFeature,
    //A <: BaseActual]
abstract class SparkDataPreparator[
    EDP <: BaseEvaluationDataParams : Manifest,
    TDP <: BaseTrainingDataParams : Manifest,
    VDP <: BaseValidationDataParams,
    TD : Manifest,
    F,
    A]
    extends SlicedDataPreparator[EDP, TDP, VDP, TD, F, A] {

  override
  def prepareTrainingBase(
    sc: SparkContext,
    params: BaseTrainingDataParams): TD = {
    println("SparkDataPreparator.prepareTrainingBase")
    val tdp = params.asInstanceOf[TDP]
    prepareTraining(sc, tdp)
  }

  def prepareTraining(sc: SparkContext, params: TDP): TD

  override
  def prepareValidationBase(
    sc: SparkContext,
    params: BaseValidationDataParams): RDD[(F, A)] = {
    //params: BaseValidationDataParams): RDD[(BaseFeature, BaseActual)] = {
    val vdp = params.asInstanceOf[VDP]
    prepareValidation(sc, vdp)
      //.map(e => 
      //  (e._1.asInstanceOf[BaseFeature], e._2.asInstanceOf[BaseActual]))
  }
  
  def prepareValidation(sc: SparkContext, params: VDP): RDD[(F, A)]
}


/*
    F <: BaseFeature,
    P <: BasePrediction,
    A <: BaseActual,
    VU <: BaseValidationUnit,
    VR <: BaseValidationResults,
    CVR <: BaseCrossValidationResults]
*/ 

abstract class BaseValidator[
    VP <: BaseValidationParams : Manifest,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    F,
    P,
    A,
    VU,
    VR,
    CVR <: AnyRef ]
    //CVR ]
  extends AbstractParameterizedDoer[VP] {

  /*
  def validateSeq(predictionSeq: BasePredictionSeq)
    : BaseValidationUnitSeq = {
    val input: Seq[(F, P, A)] = predictionSeq
      .asInstanceOf[PredictionSeq[F, P, A]].data
    val output = input.map(e => validate(e._1, e._2, e._3))
    return new ValidationUnitSeq(data = output)
  }
  */

  //def validateBase(input: (BaseFeature, BasePrediction, BaseActual))
    //: BaseValidationUnit = {
  def validateBase(input: (F, P, A))
    : VU = {
    validate(input._1, input._2, input._3)
  }
 
  /*
  def validateBase(
      feature: BaseFeature, 
      prediction: BasePrediction, 
      actual: BaseActual)
    //: BaseValidationUnit = {
    : VU = {
    validate(
      feature.asInstanceOf[F],
      prediction.asInstanceOf[P],
      actual.asInstanceOf[A])
  }
  */

  def validate(feature: F, predicted: P, actual: A): VU

  def validateSetBase(
    trainingDataParams: BaseTrainingDataParams,
    validationDataParams: BaseValidationDataParams,
    //validationUnits: Seq[BaseValidationUnit]): BaseValidationResults = {
    //validationUnits: Seq[BaseValidationUnit]): VR = {
    validationUnits: Seq[Any]): VR = {
    validateSet(
      trainingDataParams.asInstanceOf[TDP],
      validationDataParams.asInstanceOf[VDP],
      validationUnits.map(_.asInstanceOf[VU]))
  }

  def validateSet(
    trainingDataParams: TDP,
    validationDataParams: VDP,
    validationUnits: Seq[VU]): VR

  def crossValidateBase(
    input: Seq[(BaseTrainingDataParams, BaseValidationDataParams,
      //BaseValidationResults)]): BaseCrossValidationResults = {
      Any)]): CVR = {
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
    TD,
    F,
    P,
    A,
    VU,
    VR,
    CVR <: AnyRef](
  val dataPreparatorClass
    : Class[_ <: BaseDataPreparator[EDP, TDP, VDP, TD, F, A]],
  val validatorClass
    : Class[_ <: BaseValidator[VP, TDP, VDP, F, P, A, VU, VR, CVR]]) {}

/*
class LocalEvaluator[
    EDP <: BaseEvaluationDataParams,
    VP <: BaseValidationParams,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    TD,
    F ,
    P ,
    A ,
    VU ,
    VR ,
    CVR <: AnyRef ](
  dataPreparatorClass
    : Class[_ <: LocalDataPreparator[EDP, TDP, VDP, TD, F, A]],
  validatorClass
    : Class[_ <: BaseValidator[VP, TDP, VDP, F, P, A, VU, VR, CVR]])
  extends BaseEvaluator[EDP, VP, TDP, VDP, RDD[TD], F, P, A, VU, VR, CVR](
    dataPreparatorClass, validatorClass) {}

class SparkEvaluator[
    EDP <: BaseEvaluationDataParams,
    VP <: BaseValidationParams,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    TD,
    F,
    P,
    A,
    VU,
    VR,
    CVR <: AnyRef](
  dataPreparatorClass
    : Class[_ <: SparkDataPreparator[EDP, TDP, VDP, TD, F, A]],
  validatorClass
    : Class[_ <: BaseValidator[VP, TDP, VDP, F, P, A, VU, VR, CVR]])
  extends BaseEvaluator[EDP, VP, TDP, VDP, TD, F, P, A, VU, VR, CVR](
    dataPreparatorClass, validatorClass) {}
*/

package io.prediction.core

import scala.reflect.Manifest
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import scala.collection.JavaConversions._

// FIXME(yipjustin). I am being lazy...
import io.prediction._

abstract class BaseDataPreparator[
    EDP <: BaseParams: Manifest,
    TDP <: BaseParams,
    VDP <: BaseParams,
    BTD, 
    F,
    A]
  extends AbstractParameterizedDoer[EDP] {

  def prepareBase(sc: SparkContext, params: BaseParams)
  : Map[Int, (TDP, VDP, BTD, RDD[(F, A)])]
}


abstract class SlicedDataPreparator[
    EDP <: BaseParams : Manifest,
    TDP <: BaseParams,
    VDP <: BaseParams,
    BTD, 
    F,
    A]
  extends BaseDataPreparator[EDP, TDP, VDP, BTD, F, A] {
  
  def prepareBase(sc: SparkContext, params: BaseParams)
  : Map[Int, (TDP, VDP, BTD, RDD[(F, A)])] = {
    prepare(sc, params.asInstanceOf[EDP])
  }

  def prepare(sc: SparkContext, params: EDP)
  : Map[Int, (TDP, VDP, BTD, RDD[(F, A)])] = {
    val localParamsSet
    : Map[Int, (TDP, VDP)] =
      getParamsSetBase(params)
      .zipWithIndex
      .map(_.swap)
      .toMap

    // May add a param to skip .par
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

  def getParamsSetBase(params: EDP): Iterable[(TDP, VDP)] 

  def prepareTrainingBase(sc: SparkContext, params: TDP): BTD
  
  def prepareValidationBase(sc: SparkContext, params: VDP): RDD[(F, A)]
  
}


abstract class LocalDataPreparator[
    EDP <: BaseParams : Manifest,
    TDP <: BaseParams : Manifest,
    VDP <: BaseParams : Manifest,
    TD : Manifest, F, A]
    extends SlicedDataPreparator[EDP, TDP, VDP, RDD[TD], F, A] {
  
  def getParamsSetBase(params: EDP): Iterable[(TDP, VDP)] = getParamsSet(params)
  
  def getParamsSet(params: EDP): Iterable[(TDP, VDP)] 

  override
  def prepareTrainingBase(sc: SparkContext, tdp: TDP): RDD[TD] = {
    prepareTraining(sc, tdp)
  }

  def prepareTraining(sc: SparkContext, tdp: TDP): RDD[TD] = {
    sc.parallelize(Array(tdp)).map(prepareTraining)
  }

  def prepareTraining(params: TDP): TD

  override
  def prepareValidationBase(sc: SparkContext, vdp: VDP): RDD[(F, A)] = {
    prepareValidation(sc, vdp)
  }

  def prepareValidation(sc: SparkContext, vdp: VDP): RDD[(F, A)] = {
    sc.parallelize(Array(vdp)).flatMap(prepareValidation)
  }
  
  def prepareValidation(params: VDP): Seq[(F, A)]
}

// In this case, TD may contain multiple RDDs
// But still, F and A cannot contain RDD
abstract class SparkDataPreparator[
    EDP <: BaseParams : Manifest,
    TDP <: BaseParams,
    VDP <: BaseParams,
    TD : Manifest, F, A]
  extends SlicedDataPreparator[EDP, TDP, VDP, TD, F, A] {
  
    def getParamsSetBase(params: EDP): Iterable[(TDP, VDP)] = getParamsSet(params)
  
  def getParamsSet(params: EDP): Iterable[(TDP, VDP)] 

  //override
  def prepareTrainingBase(sc: SparkContext, tdp: TDP): TD = {
    prepareTraining(sc, tdp)
  }

  def prepareTraining(sc: SparkContext, params: TDP): TD

  //override
  def prepareValidationBase(sc: SparkContext, vdp: VDP): RDD[(F, A)] = {
    prepareValidation(sc, vdp)
  }
  
  def prepareValidation(sc: SparkContext, params: VDP): RDD[(F, A)]
}

abstract class BaseValidator[
    VP <: BaseParams : Manifest,
    TDP <: BaseParams,
    VDP <: BaseParams,
    F, P, A, VU, VR, CVR <: AnyRef]
  extends AbstractParameterizedDoer[VP] {
  def validateBase(input: (F, P, A)): VU
 
  def validateSetBase(
    trainingDataParams: TDP,
    validationDataParams: VDP,
    validationUnits: Seq[VU]): VR 

  def crossValidateBase(
    input: Seq[(TDP, VDP, VR)]): CVR 
}

/* Evaluator */
class BaseEvaluator[
    EDP <: BaseParams,
    VP <: BaseParams,
    TDP <: BaseParams,
    VDP <: BaseParams,
    TD, F, P, A, VU, VR, CVR <: AnyRef](
  val dataPreparatorClass
    : Class[_ <: BaseDataPreparator[EDP, TDP, VDP, TD, F, A]],
  val validatorClass
    : Class[_ <: BaseValidator[VP, TDP, VDP, F, P, A, VU, VR, CVR]]) {}

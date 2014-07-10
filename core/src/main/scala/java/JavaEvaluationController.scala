package io.prediction.java

import io.prediction.core.LocalDataPreparator
import io.prediction.core.SlicedDataPreparator
import io.prediction.core.BaseDataPreparator
import io.prediction.core.BaseValidator
import io.prediction.BaseParams
import java.util.{ List => JList }
import java.lang.{ Iterable => JIterable }
import scala.collection.JavaConversions._
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import scala.reflect.ClassTag
import io.prediction._

abstract class JavaLocalDataPreparator[
    EDP <: BaseParams,
    TDP <: BaseParams,
    VDP <: BaseParams,
    TD, F, A]
    extends SlicedDataPreparator[EDP, TDP, VDP, RDD[TD], F, A]()(
        // Injecting fake manifest to superclass
        JavaUtils.fakeManifest[EDP]
    ){

  def getParamsSetBase(params: EDP): Iterable[(TDP, VDP)] = {
    getParamsSet(params)
  }
  
  def getParamsSet(params: EDP): JIterable[(TDP, VDP)] 

  override
  def prepareTrainingBase(sc: SparkContext, tdp: TDP): RDD[TD] = {
    implicit val fakeTdpTag: ClassTag[TDP] = JavaUtils.fakeClassTag[TDP]
    implicit val fakeTdTag: ClassTag[TD] = JavaUtils.fakeClassTag[TD]
    sc.parallelize(Array(tdp)).map(prepareTraining)
  }
  
  def prepareTraining(tdp: TDP): TD

  override
  def prepareValidationBase(sc: SparkContext, vdp: VDP): RDD[(F, A)] = {
    implicit val fakeClassTag = JavaUtils.fakeClassTag[VDP]
    sc.parallelize(Array(vdp)).flatMap(prepareValidation)
  }

  def prepareValidation(params: VDP): JIterable[Tuple2[F, A]]
}


abstract class JavaSimpleLocalDataPreparator[
    EDP <: BaseParams, TD, F, A]
    extends BaseDataPreparator[EDP, EmptyParams, EmptyParams, RDD[TD], F, A]()(
        // Injecting fake manifest to superclass
        JavaUtils.fakeManifest[EDP]
    ){

  override def prepareBase(sc: SparkContext, params: BaseParams)
  : Map[Int, (EmptyParams, EmptyParams, RDD[TD], RDD[(F, A)])] = {
    // FIXME. Should parallelize EDP first.
    implicit val fakeTdTag: ClassTag[TD] = JavaUtils.fakeClassTag[TD]
    val (td, faSeq) = prepare(params.asInstanceOf[EDP])
    val faRdd = sc.parallelize(faSeq.toSeq)
    val tdRdd = sc.parallelize(Array(td))
    Map(0 -> (EmptyParams(), EmptyParams(), tdRdd, faRdd))
  }

  def prepare(edp: EDP): (TD, JIterable[Tuple2[F, A]])
}


abstract class JavaValidator[
    VP <: BaseParams,
    TDP <: BaseParams,
    VDP <: BaseParams,
    F, P, A, VU, VR, CVR <: AnyRef]
  extends BaseValidator[VP, TDP, VDP, F, P, A, VU, VR, CVR]()(
    JavaUtils.fakeManifest[VP]) {
  
  def validateBase(input: (F, P, A)): VU = {
    validate(input._1, input._2, input._3)
  }
 
  def validate(feature: F, predicted: P, actual: A): VU

  def validateSetBase(
    trainingDataParams: TDP,
    validationDataParams: VDP,
    validationUnits: Seq[VU]): VR = {
    validateSet(
      trainingDataParams,
      validationDataParams,
      validationUnits)
  }

  def validateSet(
    trainingDataParams: TDP,
    validationDataParams: VDP,
    validationUnits: JIterable[VU]): VR

  def crossValidateBase(
    input: Seq[(TDP, VDP, VR)]): CVR = {
    crossValidate(input)
  }

  def crossValidate(validateResultsSeq: JIterable[Tuple3[TDP, VDP, VR]]): CVR
  

}





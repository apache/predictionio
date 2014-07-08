package io.prediction.java

import io.prediction.core.LocalDataPreparator
import io.prediction.core.SlicedDataPreparator
import io.prediction.BaseParams
import java.util.{ List => JList }
import java.lang.{ Iterable => JIterable }
import scala.collection.JavaConversions._
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import scala.reflect.ClassTag

// Injecting fake manifest to superclass
abstract class JavaLocalDataPreparator[
    EDP <: BaseParams,
    TDP <: BaseParams,
    VDP <: BaseParams,
    TD, F, A]
    extends SlicedDataPreparator[EDP, TDP, VDP, RDD[TD], F, A]()(
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





package io.prediction.core

import scala.reflect.Manifest

// FIXME(yipjustin). I am being lazy...
import io.prediction._
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

abstract
class BaseCleanser[
    TD <: BaseTrainingData,
    CD <: BaseCleansedData,
    CP <: BaseCleanserParams: Manifest]
  extends AbstractParameterizedDoer[CP] {

  def cleanseBase(trainingData: BaseTrainingData): BaseCleansedData
}


abstract
class LocalCleanser[
    TD <: BaseTrainingData,
    CD <: BaseCleansedData : Manifest,
    CP <: BaseCleanserParams: Manifest]
  extends BaseCleanser[RDDTD[TD], RDDCD[CD], CP] {

  def cleanseBase(trainingData: BaseTrainingData): BaseCleansedData = {
    println("Local.cleanseBase.")
    val cd: RDD[CD] = trainingData
      .asInstanceOf[RDDTD[TD]]
      .v
      .map(cleanse)
    new RDDCD[CD](v = cd)
  }

  def cleanse(trainingData: TD): CD
}

abstract
class SparkCleanser[
    TD <: BaseTrainingData,
    CD <: BaseCleansedData,
    CP <: BaseCleanserParams: Manifest]
  extends BaseCleanser[TD, CD, CP] {

  def cleanseBase(trainingData: BaseTrainingData): BaseCleansedData = {
    println("SparkCleanser.cleanseBase")
    cleanse(trainingData.asInstanceOf[TD])
  }

  def cleanse(trainingData: TD): CD
}




/* Algorithm */

abstract class BaseAlgorithm[
    CD <: BaseCleansedData,
    F <: BaseFeature : Manifest,
    P <: BasePrediction,
    M <: BaseModel,
    AP <: BaseAlgoParams: Manifest]
  extends AbstractParameterizedDoer[AP] {

  def trainBase(
    sc: SparkContext,
    cleansedData: BaseCleansedData): RDD[BaseModel]

  def predictBase(baseModel: BaseModel, baseFeature: BaseFeature)
    : BasePrediction = {
    predict(
      baseModel.asInstanceOf[M],
      baseFeature.asInstanceOf[F])
  }

  def predict(model: M, feature: F): P

  def featureClass() = manifest[F]

}

abstract class LocalAlgorithm[
    CD <: BaseCleansedData,
    F <: BaseFeature : Manifest,
    P <: BasePrediction,
    M <: BaseModel : Manifest,
    AP <: BaseAlgoParams: Manifest]
  extends BaseAlgorithm[RDDCD[CD], F, P, M, AP] {

  def trainBase(
    sc: SparkContext,
    cleansedData: BaseCleansedData): RDD[BaseModel] = {
    println("LocalAlgorithm.trainBase")
    val m: RDD[BaseModel] = cleansedData.asInstanceOf[RDDCD[CD]]
      .v
      .map(train)
      .map(_.asInstanceOf[BaseModel])
    m
  }

  def train(cleansedData: CD): M

  def predict(model: M, feature: F): P
}

abstract class SparkAlgorithm[
    CD <: BaseCleansedData,
    F <: BaseFeature : Manifest,
    P <: BasePrediction,
    M <: BaseModel : Manifest,
    AP <: BaseAlgoParams: Manifest]
  extends BaseAlgorithm[CD, F, P, M, AP] {
  // train returns a local object M, and we parallelize it.
  def trainBase(
    sc: SparkContext,
    cleansedData: BaseCleansedData): RDD[BaseModel] = {
    val m: BaseModel = train(cleansedData.asInstanceOf[CD])
    sc.parallelize(Array(m))
  }

  def train(cleansedData: CD): M

  def predict(model: M, feature: F): P
}




/* Server */

abstract class BaseServer[
    F <: BaseFeature,
    P <: BasePrediction,
    SP <: BaseServerParams: Manifest]
  extends AbstractParameterizedDoer[SP] {

  def combineSeqBase(basePredictionSeqSeq: Seq[BasePredictionSeq])
    : BasePredictionSeq = {
    val dataSeq: Seq[Seq[(F, P, BaseActual)]] = basePredictionSeqSeq
      .map(_.asInstanceOf[PredictionSeq[F, P, BaseActual]].data).transpose

    val output: Seq[(F, P, BaseActual)] = dataSeq.map{ input => {
      val f = input(0)._1
      val ps = input.map(_._2)
      val a = input(0)._3
      // TODO(yipjustin). Check all seqs have the same f and a
      val p = combine(f, ps)
      (f, p, a)
    }}
    new PredictionSeq[F, P, BaseActual](data = output)
  }

  def combineBase(
    baseFeature: BaseFeature,
    basePredictions: Seq[BasePrediction])
    : BasePrediction = {
    combine(
      baseFeature.asInstanceOf[F],
      basePredictions.map(_.asInstanceOf[P]))
  }

  def combine(feature: F, predictions: Seq[P]): P

}

/* Engine */

class BaseEngine[
    TD <: BaseTrainingData,
    CD <: BaseCleansedData,
    F <: BaseFeature,
    P <: BasePrediction](
    val cleanserClass
      : Class[_ <: BaseCleanser[TD, CD, _ <: BaseCleanserParams]],
    val algorithmClassMap
      : Map[String,
        Class[_ <:
          BaseAlgorithm[CD, F, P, _ <: BaseModel, _ <: BaseAlgoParams]]],
    val serverClass: Class[_ <: BaseServer[F, P, _ <: BaseServerParams]]) {
  }

class LocalEngine[
    TD <: BaseTrainingData,
    CD <: BaseCleansedData,
    F <: BaseFeature,
    P <: BasePrediction](
    cleanserClass
      : Class[_ <: LocalCleanser[TD, CD, _ <: BaseCleanserParams]],
    algorithmClassMap
      : Map[String,
        Class[_ <:
          LocalAlgorithm[CD, F, P, _ <: BaseModel, _ <: BaseAlgoParams]]],
    serverClass: Class[_ <: BaseServer[F, P, _ <: BaseServerParams]])
    extends BaseEngine(cleanserClass, algorithmClassMap, serverClass)

class SparkEngine[
    TD <: BaseTrainingData,
    CD <: BaseCleansedData,
    F <: BaseFeature,
    P <: BasePrediction](
    cleanserClass
      : Class[_ <: SparkCleanser[TD, CD, _ <: BaseCleanserParams]],
    algorithmClassMap
      : Map[String,
        Class[_ <:
          SparkAlgorithm[CD, F, P, _ <: BaseModel, _ <: BaseAlgoParams]]],
    serverClass: Class[_ <: BaseServer[F, P, _ <: BaseServerParams]])
    extends BaseEngine(cleanserClass, algorithmClassMap, serverClass)

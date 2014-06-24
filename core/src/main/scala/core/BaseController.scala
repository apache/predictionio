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
    TD ,
    CD ,
    CP <: BaseCleanserParams: Manifest]
  extends AbstractParameterizedDoer[CP] {

  //def cleanseBase(trainingData: BaseTrainingData): BaseCleansedData
  def cleanseBase(trainingData: Any): CD
}


abstract 
class LocalCleanser[
    TD ,
    CD : Manifest,
    CP <: BaseCleanserParams: Manifest]
  extends BaseCleanser[RDD[TD], RDD[CD], CP] {

  //def cleanseBase(trainingData: BaseTrainingData): BaseCleansedData = {
  def cleanseBase(trainingData: Any): RDD[CD] = {
    println("Local.cleanseBase.")
    val cd: RDD[CD] = trainingData
      .asInstanceOf[RDD[TD]]
      //.v
      .map(cleanse)
    cd
    //new RDD[CD](v = cd)
  }

  def cleanse(trainingData: TD): CD
}

abstract
class SparkCleanser[
    TD ,
    CD ,
    CP <: BaseCleanserParams: Manifest]
  extends BaseCleanser[TD, CD, CP] {

  //def cleanseBase(trainingData: BaseTrainingData): BaseCleansedData = {
  //def cleanseBase(trainingData: BaseTrainingData): CD = {
  def cleanseBase(trainingData: Any): CD = {
    println("SparkCleanser.cleanseBase")
    cleanse(trainingData.asInstanceOf[TD])
  }

  def cleanse(trainingData: TD): CD
}




/* Algorithm */

abstract class BaseAlgorithm[
    CD,
    F,
    P,
    M,
    AP <: BaseAlgoParams: Manifest]
  extends AbstractParameterizedDoer[AP] {

  def trainBase(
    sc: SparkContext, 
    cleansedData: CD): RDD[Any] 

  def predictBase(baseModel: Any, baseFeature: F)
    : P = {
    predict(
      baseModel.asInstanceOf[M],
      baseFeature.asInstanceOf[F])
  }

  def predict(model: M, feature: F): P
}

abstract class LocalAlgorithm[
    CD,
    F,
    P,
    M: Manifest,
    AP <: BaseAlgoParams: Manifest]
  extends BaseAlgorithm[RDD[CD], F, P, M, AP] {

  def trainBase(
    sc: SparkContext,
    cleansedData: RDD[CD]): RDD[Any] = {
    println("LocalAlgorithm.trainBase")
    val m: RDD[Any] = cleansedData
      //.asInstanceOf[RDD[CD]]
      //.v
      .map(train)
      .map(_.asInstanceOf[Any])
      //.map(_.asInstanceOf[BaseModel])
    m
  }

  def train(cleansedData: CD): M
  
  def predict(model: M, feature: F): P
}

abstract class SparkAlgorithm[
    CD,
    F,
    P,
    M: Manifest,
    AP <: BaseAlgoParams: Manifest]
  extends BaseAlgorithm[CD, F, P, M, AP] {
  // train returns a local object M, and we parallelize it.
  def trainBase(
    sc: SparkContext, 
    cleansedData: CD): RDD[Any] = {
    //val m: M = train(cleansedData.asInstanceOf[CD])
    val m: Any = train(cleansedData.asInstanceOf[CD]).asInstanceOf[Any]
    sc.parallelize(Array(m))
  }

  def train(cleansedData: CD): M

  def predict(model: M, feature: F): P
}




/* Server */

abstract class BaseServer[
    F,
    P,
    SP <: BaseServerParams: Manifest]
  extends AbstractParameterizedDoer[SP] {

  /*
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
  */

  def combineBase(
    baseFeature: F,
    basePredictions: Seq[P])
    : P = {
    combine(
      baseFeature.asInstanceOf[F],
      basePredictions.map(_.asInstanceOf[P]))
  }

  def combine(feature: F, predictions: Seq[P]): P

}

/* Engine */

class BaseEngine[
    TD,
    CD,
    F,
    P](
    val cleanserClass
      : Class[_ <: BaseCleanser[TD, CD, _ <: BaseCleanserParams]],
    val algorithmClassMap
      : Map[String,
        Class[_ <:
          BaseAlgorithm[CD, F, P, _, _]]],
    val serverClass: Class[_ <: BaseServer[F, P, _ <: BaseServerParams]]) {}

class LocalEngine[
    TD,
    CD,
    F,
    P](
    cleanserClass
      : Class[_ <: LocalCleanser[TD, CD, _ <: BaseCleanserParams]],
    algorithmClassMap
      : Map[String,
        Class[_ <:
          LocalAlgorithm[CD, F, P, _, _]]],
    serverClass: Class[_ <: BaseServer[F, P, _ <: BaseServerParams]])
    extends BaseEngine(cleanserClass, algorithmClassMap, serverClass)
    
class SparkEngine[
    TD,
    CD,
    F,
    P](
    cleanserClass
      : Class[_ <: SparkCleanser[TD, CD, _ <: BaseCleanserParams]],
    algorithmClassMap
      : Map[String,
        Class[_ <:
          SparkAlgorithm[CD, F, P, _, _]]],
    serverClass: Class[_ <: BaseServer[F, P, _ <: BaseServerParams]])
    extends BaseEngine(cleanserClass, algorithmClassMap, serverClass)





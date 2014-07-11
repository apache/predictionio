package io.prediction.core

import scala.reflect.Manifest


// FIXME(yipjustin). I am being lazy...
import io.prediction._
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

import org.json4s.Formats

abstract
class BaseCleanser[TD, CD, CP <: BaseParams: Manifest]
  extends AbstractParameterizedDoer[CP] {
  def cleanseBase(trainingData: Any): CD
}


abstract
class LocalCleanser[TD, CD : Manifest, CP <: BaseParams: Manifest]
  extends BaseCleanser[RDD[TD], RDD[CD], CP] {

  def cleanseBase(trainingData: Any): RDD[CD] = {
    println("Local.cleanseBase.")
    trainingData
      .asInstanceOf[RDD[TD]]
      .map(cleanse)
  }

  def cleanse(trainingData: TD): CD
}

abstract
class SparkCleanser[TD, CD, CP <: BaseParams: Manifest]
  extends BaseCleanser[TD, CD, CP] {
  def cleanseBase(trainingData: Any): CD = {
    println("SparkCleanser.cleanseBase")
    cleanse(trainingData.asInstanceOf[TD])
  }

  def cleanse(trainingData: TD): CD
}

/* Algorithm */

trait LocalModelAlgorithm[F, P, M] {
  def getModel(baseModel: Any): RDD[Any] = {
    baseModel.asInstanceOf[RDD[Any]]
  }

  // Batch Prediction, for local algorithms, it calls batchPredict, whose
  // default implementation is a map over "predict". Builders can override
  // batchPredict for performance optimization.
  def batchPredictBase(baseModel: Any, baseFeatures: RDD[(Long, F)])
  : RDD[(Long, P)] = {
    // It is forcing 1 partitions. May expand to more partitions.
    val rddModel: RDD[M] = getModel(baseModel)
      .asInstanceOf[RDD[M]]
      .coalesce(1)

    val rddFeatures: RDD[(Long, F)] = baseFeatures
      .asInstanceOf[RDD[(Long, F)]]
      .coalesce(1)

    rddModel.zipPartitions(rddFeatures)(batchPredictWrapper)
  }

  def batchPredictWrapper(model: Iterator[M], features: Iterator[(Long, F)])
  : Iterator[(Long, P)] = {
    batchPredict(model.next, features)
  }

  // Expected to be overridden
  def batchPredict(model: M, features: Iterator[(Long, F)])
  : Iterator[(Long, P)] = {
    features.map { case (idx, f) => (idx, predict(model, f)) }
  }

  // One Prediction
  def predictBase(baseModel: Any, baseFeature: Any): P = {
    predict(baseModel.asInstanceOf[M], baseFeature.asInstanceOf[F])
  }

  // Expected to be overridden
  def predict(model: M, feature: F): P
}

abstract class BaseAlgorithm[CD, F : Manifest, P, M, AP <: BaseParams: Manifest]
  extends AbstractParameterizedDoer[AP] {
  def trainBase(sc: SparkContext, cleansedData: CD): M

  def predictBase(baseModel: Any, baseFeature: Any): P

  def batchPredictBase(baseModel: Any, baseFeatures: RDD[(Long, F)])
  : RDD[(Long, P)]

  def featureClass() = manifest[F]

}

abstract class LocalAlgorithm[CD, F : Manifest, P, M: Manifest,
    AP <: BaseParams: Manifest]
  extends BaseAlgorithm[RDD[CD], F, P, RDD[M], AP]
  with LocalModelAlgorithm[F, P, M] {
  def trainBase(sc: SparkContext, cleansedData: RDD[CD]): RDD[M] = {
    cleansedData.map(train)
  }

  def train(cleansedData: CD): M

  def predict(model: M, feature: F): P
}

abstract class Spark2LocalAlgorithm[CD, F : Manifest, P, M: Manifest,
    AP <: BaseParams: Manifest]
  extends BaseAlgorithm[CD, F, P, RDD[M], AP]
  with LocalModelAlgorithm[F, P, M] {
  // train returns a local object M, and we parallelize it.
  def trainBase(sc: SparkContext, cleansedData: CD): RDD[M] = {
    val m: M = train(cleansedData)
    sc.parallelize(Array(m))
  }

  def train(cleansedData: CD): M

  def predict(model: M, feature: F): P
}

abstract class ParallelAlgorithm[CD, F : Manifest, P, M: Manifest,
    AP <: BaseParams: Manifest]
  extends BaseAlgorithm[CD, F, P, M, AP] {
  def trainBase(sc: SparkContext, cleansedData: CD): M = {
    train(cleansedData)
  }

  def predictBase(baseModel: Any, baseFeature: Any): P = {
    predict(baseModel.asInstanceOf[M], baseFeature.asInstanceOf[F])
  }

  def predict(model: M, feature: F): P

  def batchPredictBase(baseModel: Any, baseFeatures: RDD[(Long, F)])
  : RDD[(Long, P)] = {
    batchPredict(baseModel.asInstanceOf[M], baseFeatures)
  }

  def batchPredict(model:M, features: RDD[(Long, F)])
  : RDD[(Long, P)]

  def train(cleansedData: CD): M
}


/* Server */
abstract class BaseServer[F, P, SP <: BaseParams: Manifest]
  extends AbstractParameterizedDoer[SP] {

  def combineBase(
    baseFeature: Any,
    basePredictions: Seq[Any])
    : P = {
    combine(
      baseFeature.asInstanceOf[F],
      basePredictions.map(_.asInstanceOf[P]))
  }

  def combine(feature: F, predictions: Seq[P]): P

}

/* Engine */
//trait SingleAlgoEngine[TD, CD, F, P]

class BaseEngine[TD, CD, F, P](
    val cleanserClass
      : Class[_ <: BaseCleanser[TD, CD, _ <: BaseParams]],
    val algorithmClassMap
      : Map[String, Class[_ <: BaseAlgorithm[CD, F, P, _, _ <: BaseParams]]],
    val serverClass: Class[_ <: BaseServer[F, P, _ <: BaseParams]],
    val formats: Formats = Util.json4sDefaultFormats)

  // Simple Engine has only one algo
class SingleAlgoEngine[TD, CD, F, P] (
    cleanserClass
      : Class[_ <: BaseCleanser[TD, CD, _ <: BaseParams]],
    algorithmClass
      : Class[_ <: BaseAlgorithm[CD, F, P, _, _ <: BaseParams]],
    serverClass: Class[_ <: BaseServer[F, P, _ <: BaseParams]],
    formats: Formats = Util.json4sDefaultFormats
) extends BaseEngine (
  cleanserClass,
  Map("" -> algorithmClass),
  serverClass,
  formats
)


/*
class LocalEngine[
    TD,
    CD,
    F,
    P](
    cleanserClass
      : Class[_ <: LocalCleanser[TD, CD, _ <: BaseParams]],
    algorithmClassMap
      : Map[String,
        Class[_ <:
          LocalAlgorithm[CD, F, P, _, _]]],
    serverClass: Class[_ <: BaseServer[F, P, _ <: BaseParams]])
    extends BaseEngine(cleanserClass, algorithmClassMap, serverClass)

class SparkEngine[
    TD,
    CD,
    F,
    P](
    cleanserClass
      : Class[_ <: SparkCleanser[TD, CD, _ <: BaseParams]],
    algorithmClassMap
      : Map[String,
        Class[_ <:
          Spark2LocalAlgorithm[CD, F, P, _, _]]],
    serverClass: Class[_ <: BaseServer[F, P, _ <: BaseParams]])
    extends BaseEngine(cleanserClass, algorithmClassMap, serverClass)
*/

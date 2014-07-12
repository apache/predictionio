package io.prediction.java

import java.lang.{ Iterable => JIterable }
import java.util.{ Map => JMap }

import io.prediction._
import io.prediction.core.LocalCleanser
import io.prediction.core.LocalAlgorithm
import io.prediction.core.BaseServer
import io.prediction.core.BaseEngine
import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseCleanser
import scala.collection.JavaConversions._
import org.apache.spark.rdd.RDD
import io.prediction.util.Util


abstract class JavaLocalCleanser[TD, CD, CP <: BaseParams]
extends LocalCleanser[TD, CD, CP]()(
  JavaUtils.fakeManifest[CD], JavaUtils.fakeManifest[CP]) {
  def cleanse(trainingData: TD): CD
}

abstract class JavaLocalAlgorithm[CD, F, P, M, AP <: BaseParams]
extends LocalAlgorithm[CD, F, P, M, AP]()(
  JavaUtils.fakeManifest[F],
  JavaUtils.fakeManifest[M],
  JavaUtils.fakeManifest[AP]) {

  def train(cleansedData: CD): M

  def predict(model: M, feature: F): P
}

abstract class JavaServer[F, P, SP <: BaseParams]
extends BaseServer[F, P, SP]()(JavaUtils.fakeManifest[SP]) {
  override
  def combineBase(baseFeature: Any,  basePredictions: Seq[Any]): P = {
    val pList = seqAsJavaList(basePredictions.map(_.asInstanceOf[P]))

    combine(baseFeature.asInstanceOf[F], pList)
  }

  def combine(feature: F, predictions: JIterable[P]): P

  final def combine(feature: F, predictions: Seq[P]): P = {
    // Should not reach here
    predictions.head
  }
}

class JavaLocalEngine[TD, CD, F, P](
    cleanserClass : Class[_ <: JavaLocalCleanser[TD, CD, _ <: BaseParams]],
    algorithmClassMap
      : JMap[String, Class[_ <: JavaLocalAlgorithm[CD, F, P, _, _ <: BaseParams]]],
    serverClass: Class[_ <: JavaServer[F, P, _ <: BaseParams]])
extends BaseEngine[RDD[TD], RDD[CD], F, P](
    cleanserClass,
    Map(algorithmClassMap.toSeq:_*),
    serverClass,
    Util.json4sDefaultFormats)


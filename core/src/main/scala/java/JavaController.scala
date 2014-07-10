package io.prediction.java

import java.lang.{ Iterable => JIterable }

import io.prediction._
import io.prediction.core.LocalCleanser
import io.prediction.core.LocalAlgorithm
import io.prediction.core.BaseServer
import scala.collection.JavaConversions._

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

package io.prediction.java

import io.prediction._
import io.prediction.core.LocalCleanser
import io.prediction.core.LocalAlgorithm

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

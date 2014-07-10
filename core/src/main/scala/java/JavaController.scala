package io.prediction.java

import io.prediction._
import io.prediction.core.LocalCleanser

abstract class JavaLocalCleanser[TD, CD, CP <: BaseParams]
extends LocalCleanser[TD, CD, CP]()(
  JavaUtils.fakeManifest[CD], JavaUtils.fakeManifest[CP]) {
  def cleanse(trainingData: TD): CD
}


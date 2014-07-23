package io.prediction.controller.java

import io.prediction.controller.LAlgorithm
import io.prediction.controller.Params

abstract class LJavaAlgorithm[AP <: Params, PD, M, Q, P]
  extends LAlgorithm[AP, PD, M, Q, P]()(
    JavaUtils.fakeClassTag[AP],
    JavaUtils.fakeClassTag[M],
    JavaUtils.fakeManifest[Q]) {
  def train(pd: PD): M

  def predict(model: M, query: Q): P
}

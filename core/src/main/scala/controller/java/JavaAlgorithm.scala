package io.prediction.controller.java

import io.prediction.controller.LAlgorithm
import io.prediction.controller.Params

import net.jodah.typetools.TypeResolver

abstract class LJavaAlgorithm[AP <: Params, PD, M, Q, P]
  extends LAlgorithm[AP, PD, M, Q, P]()(
    JavaUtils.fakeClassTag[AP],
    JavaUtils.fakeClassTag[M],
    JavaUtils.fakeManifest[Q]) {
  def train(pd: PD): M

  def predict(model: M, query: Q): P

  def queryClass(): Class[Q] = {
    val typeArgs = TypeResolver.resolveRawArguments(
      classOf[LJavaAlgorithm[AP, PD, M, Q, P]],
      getClass)
    typeArgs(3).asInstanceOf[Class[Q]]
  }
}

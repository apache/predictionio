package io.prediction.controller.java

import io.prediction.controller.LAlgorithm
import io.prediction.controller.Params

import net.jodah.typetools.TypeResolver

/**
 * Base class of a local algorithm.
 *
 * A local algorithm runs locally within a single machine and produces a model
 * that can fit within a single machine.
 *
 * @param <AP> Algorithm Parameters
 * @param <PD> Prepared Data
 * @param <M> Model
 * @param <Q> Input Query
 * @param <P> Output Prediction
 */
abstract class LJavaAlgorithm[AP <: Params, PD, M, Q, P]
  extends LAlgorithm[AP, PD, M, Q, P]()(
    JavaUtils.fakeClassTag[AP],
    JavaUtils.fakeClassTag[M],
    JavaUtils.fakeManifest[Q]) {
  def train(pd: PD): M

  def predict(model: M, query: Q): P

  /** Returns a Class object of Q for internal use. */
  def queryClass(): Class[Q] = {
    val typeArgs = TypeResolver.resolveRawArguments(
      classOf[LJavaAlgorithm[AP, PD, M, Q, P]],
      getClass)
    typeArgs(3).asInstanceOf[Class[Q]]
  }
}

package io.prediction.controller.java

import io.prediction.core.BaseServing
import io.prediction.core.BaseAlgorithm
import io.prediction.controller.Params
import io.prediction.controller.EmptyParams

import java.util.{ List => JList }
import java.lang.{ Iterable => JIterable }
import scala.collection.JavaConversions._

import scala.reflect._

abstract class LJavaServing[SP <: Params, Q, P]
  extends BaseServing[SP, Q, P]()(JavaUtils.fakeClassTag[SP]) {

  def serveBase(q: Q, ps: Seq[P]): P = serve(q, seqAsJavaList(ps))

  def serve(query: Q, predictions: JIterable[P]): P
}

/****** Helpers ******/
// Return the first prediction.
class LJavaFirstServing[Q, P] extends LJavaServing[EmptyParams, Q, P] {
  override def serve(query: Q, predictions: JIterable[P]): P = {
    predictions.iterator().next()
  }
}

object LJavaFirstServing {
  def apply[Q, P](a: Class[_ <: BaseAlgorithm[_, _, _, Q, P]]) =
    classOf[FirstServing[Q, P]]

  def apply[Q, P, B <: JavaEngineBuilder[_, _, _, Q, P, _]](b: B) =
    classOf[FirstServing[Q, P]]
}

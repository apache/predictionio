package io.prediction.controller.java

import io.prediction.core.BaseServing
import io.prediction.controller.Params

import java.util.{ List => JList }
import java.lang.{ Iterable => JIterable }
import scala.collection.JavaConversions._

import scala.reflect._

abstract class LJavaServing[SP <: Params, Q, P]
  extends BaseServing[SP, Q, P]()(JavaUtils.fakeClassTag[SP]) {

  def serveBase(q: Q, ps: Seq[P]): P = serve(q, seqAsJavaList(ps))
  
  def serve(query: Q, predictions: JIterable[P]): P
}



package io.prediction.controller.java

import io.prediction.controller.Params

trait JavaParams extends Params

class EmptyParams() extends JavaParams {
  override def toString(): String = "Empty"
}

case class EmptyDataSourceParams() extends EmptyParams

case class EmptyDataParams() extends AnyRef

case class EmptyPreparatorParams() extends EmptyParams

case class EmptyAlgorithmParams() extends EmptyParams

case class EmptyServingParams() extends EmptyParams

case class EmptyMetricsParams() extends EmptyParams

case class EmptyTrainingData() extends AnyRef

case class EmptyPreparedData() extends AnyRef

case class EmptyModel() extends AnyRef

case class EmptyQuery() extends AnyRef

case class EmptyPrediction() extends AnyRef

case class EmptyActual() extends AnyRef

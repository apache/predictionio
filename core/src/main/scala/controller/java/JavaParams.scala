package io.prediction.controller.java

import io.prediction.controller.Params

/** Parameters base class in Java. */
trait JavaParams extends Params

/** Empty parameters. */
class EmptyParams() extends JavaParams {
  override def toString(): String = "Empty"
}

/** Empty data source parameters. */
case class EmptyDataSourceParams() extends EmptyParams

/** Empty data parameters. */
case class EmptyDataParams() extends AnyRef

/** Empty preparator parameters. */
case class EmptyPreparatorParams() extends EmptyParams

/** Empty algorithm parameters. */
case class EmptyAlgorithmParams() extends EmptyParams

/** Empty serving parameters. */
case class EmptyServingParams() extends EmptyParams

/** Empty metrics parameters. */
case class EmptyMetricsParams() extends EmptyParams

/** Empty training data. */
case class EmptyTrainingData() extends AnyRef

/** Empty prepared data. */
case class EmptyPreparedData() extends AnyRef

/** Empty model. */
case class EmptyModel() extends AnyRef

/** Empty input query. */
case class EmptyQuery() extends AnyRef

/** Empty output prediction. */
case class EmptyPrediction() extends AnyRef

/** Empty actual value. */
case class EmptyActual() extends AnyRef

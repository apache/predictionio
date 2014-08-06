package io.prediction

import io.prediction.controller.EmptyParams

/** Provides building blocks for writing a complete prediction engine
  * consisting of DataSource, Preparator, Algorithm, Serving, and Metrics.
  */
package object controller {

  type EmptyDataSourceParams = EmptyParams

  type EmptyDataParams = AnyRef

  type EmptyPreparatorParams = EmptyParams

  type EmptyAlgorithmParams = EmptyParams

  type EmptyServingParams = EmptyParams

  type EmptyMetricsParams = EmptyParams

  type EmptyTrainingData = AnyRef

  type EmptyPreparedData = AnyRef

  type EmptyModel = AnyRef

  type EmptyQuery = AnyRef

  type EmptyPrediction = AnyRef

  type EmptyActual = AnyRef

}

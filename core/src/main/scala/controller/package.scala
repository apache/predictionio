package io.prediction

import io.prediction.controller.EmptyParams

/** Provides building blocks for writing a complete prediction engine
  * consisting of DataSource, Preparator, Algorithm, Serving, and Metrics.
  */
package object controller {

  /** Empty data source parameters.
    * @group General
    */
  type EmptyDataSourceParams = EmptyParams

  /** Empty data parameters.
    * @group General
    */
  type EmptyDataParams = AnyRef

  /** Empty preparator parameters.
    * @group General
    */
  type EmptyPreparatorParams = EmptyParams

  /** Empty algorithm parameters.
    * @group General
    */
  type EmptyAlgorithmParams = EmptyParams

  /** Empty serving parameters.
    * @group General
    */
  type EmptyServingParams = EmptyParams

  /** Empty metrics parameters.
    * @group General
    */
  type EmptyMetricsParams = EmptyParams

  /** Empty training data.
    * @group General
    */
  type EmptyTrainingData = AnyRef

  /** Empty prepared data.
    * @group General
    */
  type EmptyPreparedData = AnyRef

  /** Empty model.
    * @group General
    */
  type EmptyModel = AnyRef

  /** Empty input query.
    * @group General
    */
  type EmptyQuery = AnyRef

  /** Empty output prediction.
    * @group General
    */
  type EmptyPrediction = AnyRef

  /** Empty actual value.
    * @group General
    */
  type EmptyActual = AnyRef

}

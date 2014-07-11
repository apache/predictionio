package io.prediction

import org.apache.spark.SparkContext

trait BaseParams extends Serializable {}

// Concrete helper classes
class EmptyParams() extends BaseParams

object EmptyParams {
  def apply(): EmptyParams = new EmptyParams()
}

/**
 * Mix in and implement this trait if you want PredictionIO to persist a model
 * that contains RDD(s).
 */
abstract class PersistentParallelModel {
  /**
   * Save the model to some persistent storage.
   *
   * @param id A globally unique ID provided automatically that identifies a
   *           particular run.
   */
  def save(id: String): Unit

  /**
   * Load the model from some persistent storage.
   *
   * @param sc An Apache SparkContext instance provided automatically.
   * @param id A globally unique ID provided automatically that identifies a
   *           particular run.
   */
  def load(sc: SparkContext, id: String): Unit
}

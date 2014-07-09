package io.prediction

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
abstract class PersistentParallelModel[AI <: Serializable] {
  /**
   * Save the model to some persistent storage.
   *
   * @param id A globally unique ID provided automatically that identifies a
   *           particular run.
   * @return Any additional information that you want PredictionIO to save. It
   *         must be serializable.
   */
  def save(id: String): AI

  /**
   * Load the model from some persistent storage.
   *
   * @param id A globally unique ID provided automatically that identifies a
   *           particular run.
   * @param additionalInfo Any additional information that have been previously
   *                       saved.
   */
  def load(id: String, additionalInfo: AI): this.type
}

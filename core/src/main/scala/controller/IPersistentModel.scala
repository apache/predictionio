package io.prediction.controller

import org.apache.spark.SparkContext

/** Mix in and implement this trait if your model cannot be persisted by
  * PredictionIO automatically. A companion object extending
  * IPersistentModelLoader is required for PredictionIO to load the persisted
  * model automatically during deployment.
  *
  * {{{
  * class MyModel extends IPersistentModel[MyParams] {
  *   def save(id: String, params: MyParams): Unit = {
  *     ...
  *   }
  * }
  *
  * object MyModel extends IPersistentModelLoader[MyParams, MyModel] {
  *   def apply(id: String, params: MyParams, sc: SparkContext): MyModel = {
  *     ...
  *   }
  * }
  * }}}
  *
  * @tparam AP Algorithm parameters class.
  * @see [[IPersistentModelLoader]]
  */
trait IPersistentModel[AP <: Params] {
  /** Save the model to some persistent storage.
    *
    * This method should return true if the model has been saved successfully so
    * that PredictionIO knows that it can be restored later during deployment.
    * This method should return false if the model cannot be saved (or should
    * not be saved due to configuration) so that PredictionIO will re-train the
    * model during deployment. All arguments of this method are provided by
    * automatically by PredictionIO.
    *
    * @param id ID of the run that trained this model.
    * @param params Algorithm parameters that were used to train this model.
    */
  def save(id: String, params: AP): Boolean
}

/** Implement an object that extends this trait for PredictionIO to support
  * loading a persisted model during serving deployment.
  *
  * @tparam AP Algorithm parameters class.
  * @tparam M Model class.
  * @see [[IPersistentModel]]
  */
trait IPersistentModelLoader[AP <: Params, M] {
  /** Implement this method to restore a persisted model that extends the
    * [[IPersistentModel]] trait. All arguments of this method are provided
    * automatically by PredictionIO.
    *
    * @param id ID of the run that trained this model.
    * @param params Algorithm parameters that were used to train this model.
    * @param sc An Apache Spark context.
    */
  def apply(id: String, params: AP, sc: SparkContext): M
}

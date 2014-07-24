package io.prediction.controller

/** Mix in and implement this trait if your model cannot be persisted by
  * PredictionIO automatically. A companion object extending IPersistentModel is
  * required for PredictionIO to load the persisted model automatically during
  * deployment.
  *
  * {{{
  * class MyModel extends PersistentModel[MyParams] {
  *   def save(id: String, params: MyParams): Unit = {
  *     ...
  *   }
  * }
  *
  * object MyModel extends IPersistentModel[MyParams, MyModel] {
  *   def apply(id: String, params: MyParams, sc: SparkContext): MyModel = {
  *     ...
  *   }
  * }
  * }}}
  */
trait PersistentModel[AP <: Params] {
  /** Save the model to some persistent storage.
    *
    * This method should return true if the model has been saved successfully so
    * that PredictionIO knows that it can be restored later during deployment.
    * This method should return false if the model cannot be saved (or should
    * not be saved due to configuration) so that PredictionIO will re-train the
    * model during deployment.
    *
    * @param id ID of the run that trained this model.
    * @param params Algorithm parameters that were used to train this model.
    */
  def save(id: String, params: AP): Boolean
}

package io.prediction.commons.modeldata

/** Base trait for all model data classes. */
trait ModelData {
  /**
   * Delete model data of the specified Algo ID and set.
   *
   * @param algoid Algo ID.
   * @param modelset Set of model data.
   */
  def delete(algoid: Int, modelset: Boolean)

  /**
   * Whether model data of the specified Algo ID and set is empty.
   *
   * @param algoid Algo ID.
   * @param modelset Set of model data.
   */
  def empty(algoid: Int, modelset: Boolean): Boolean

  /**
   * Logic to be performed before batch writing model data.
   *
   * @param algoid Algo ID.
   * @param modelset Set of model data.
   */
  def before(algoid: Int, modelset: Boolean)

  /**
   * Logic to be performed after batch writing model data.
   *
   * @param algoid Algo ID.
   * @param modelset Set of model data.
   */
  def after(algoid: Int, modelset: Boolean)
}

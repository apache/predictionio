package io.prediction

trait BaseParams extends Serializable {}

// Concrete helper classes
class EmptyParams() extends BaseParams

object EmptyParams {
  def apply(): EmptyParams = new EmptyParams()
}

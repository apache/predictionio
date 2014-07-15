package io.prediction.api

//trait BaseParams extends Serializable {}
// Will remove BaseParams eventually.
import io.prediction.BaseParams
trait Params extends BaseParams {}

// Concrete helper classes
class EmptyParams() extends Params

object EmptyParams {
  def apply(): EmptyParams = new EmptyParams()
}


package io.prediction.api

//trait BaseParams extends Serializable {}
// Will remove BaseParams eventually.
import io.prediction.BaseParams
trait Params extends BaseParams {}

// Concrete helper classes
case class EmptyParams() extends Params {
  override
  def toString(): String = "Empty"
}


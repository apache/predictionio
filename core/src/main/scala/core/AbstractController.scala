package io.prediction.core

// FIXME(yipjustin). I am lazy...
import scala.reflect.Manifest

import io.prediction.BaseParams

abstract class AbstractParameterizedDoer[P <: BaseParams : Manifest]
extends Serializable {
  def initBase(params: BaseParams): Unit = {
    init(params.asInstanceOf[P])
  }

  def init(params: P): Unit =  {}
  def paramsClass() = manifest[P]
}

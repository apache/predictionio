package io.prediction

import org.apache.spark.SparkContext

trait BaseParams extends Serializable {}

// Concrete helper classes
@deprecated("Please use api.EmptyParams", "20140715")
class EmptyParams() extends BaseParams

object EmptyParams {
  @deprecated("Please use api.EmptyParams", "20140715")
  def apply(): EmptyParams = new EmptyParams()
}

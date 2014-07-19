package io.prediction.controller

import org.json4s._
import org.json4s.ext.JodaTimeSerializers

/** Controller utilities. */
object Utils {
  /** Default JSON4S serializers for PredictionIO controllers. */
  val json4sDefaultFormats = DefaultFormats.lossless ++ JodaTimeSerializers.all
}

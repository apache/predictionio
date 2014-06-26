package io.prediction

import org.json4s._
import org.json4s.ext.JodaTimeSerializers

object Util {
  val json4sDefaultFormats = new DefaultFormats {
    override def dateFormatter = new java.text.SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSSX")
  } ++ JodaTimeSerializers.all
}

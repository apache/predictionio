package io.prediction.data

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

object Utils {

  // use dateTime() for strict ISO8601 format
  val dateTimeFormatter = ISODateTimeFormat.dateTime()

  def stringToDateTime(dt: String): DateTime =
    dateTimeFormatter.parseDateTime(dt)

  def dateTimeToString(dt: DateTime): String = dateTimeFormatter.print(dt)
    //dt.toString

}

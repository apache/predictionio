package io.prediction.data

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import java.lang.IllegalArgumentException

object Utils {

  // use dateTime() for strict ISO8601 format
  val dateTimeFormatter = ISODateTimeFormat.dateTime().withOffsetParsed()

  val dateTimeNoMillisFormatter = 
    ISODateTimeFormat.dateTimeNoMillis().withOffsetParsed()

  def stringToDateTime(dt: String): DateTime = {
    // We accept two formats.
    // 1. "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
    // 2. "yyyy-MM-dd'T'HH:mm:ssZZ"
    // The first one also takes milliseconds into account. 
    try {
      // formatting for "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
      dateTimeFormatter.parseDateTime(dt)
    } catch {
      case e: IllegalArgumentException => {
        // handle when the datetime string doesn't specify milliseconds.
        dateTimeNoMillisFormatter.parseDateTime(dt)
      }
    }
  }

  def dateTimeToString(dt: DateTime): String = dateTimeFormatter.print(dt)
    //dt.toString

}

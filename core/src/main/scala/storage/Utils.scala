package io.prediction.storage

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

/** Backend-agnostic storage utilities. */
object Utils {
  /**
   * Add prefix to custom attribute keys.
   */
  def addPrefixToAttributeKeys[T](
      attributes: Map[String, T],
      prefix: String = "ca_"): Map[String, T] = {
    attributes map { case (k, v) => (prefix + k, v) }
  }

  /** Remove prefix from custom attribute keys. */
  def removePrefixFromAttributeKeys[T](
      attributes: Map[String, T],
      prefix: String = "ca_"): Map[String, T] = {
    attributes map { case (k, v) => (k.stripPrefix(prefix), v) }
  }

  /**
   * Appends App ID to any ID.
   * Used for distinguishing different app's data within a single collection.
   */
  def idWithAppid(appid: Int, id: String) = appid + "_" + id

  def stringToDateTime(dt: String): DateTime =
    ISODateTimeFormat.dateTimeParser.parseDateTime(dt)
}

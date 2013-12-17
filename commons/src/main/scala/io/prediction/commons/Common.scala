package io.prediction.commons

/** Base trait for classes in this module. */
trait Common {
  /** Backup all data as a byte array. */
  def backup(): Array[Byte]

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[Any]]
}

/** Collection of common utilities used by this module. */
object Common {
  /**
   * Sanitize parameters retrieved from deserializing JSON.
   *
   * @param params Only values of this mapping will be sanitized.
   */
  def sanitize(params: Map[String, Any]) = {
    params.mapValues { v =>
      v match {
        case x: scala.math.BigInt => x.toInt
        case _ => v
      }
    }
  }
}

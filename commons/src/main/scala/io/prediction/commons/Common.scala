package io.prediction.commons

trait Common {
  /** Backup all data as a byte array. */
  def backup(): Array[Byte]

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], upgrade: Boolean = false): Option[Seq[Any]]
}

package io.prediction.commons.settings

import io.prediction.commons.Common

import org.json4s._
import org.json4s.native.Serialization

/**
 * SystemInfo object.
 *
 * @param id Unique identifier of the info entry.
 * @param value Value of the info entry.
 * @param description A long description of the info entry.
 */
case class SystemInfo(
  id: String,
  value: String,
  description: Option[String])

/** Base trait for implementations that interact with system info in the backend data store. */
trait SystemInfos extends Common {
  /** Inserts a system info entry. */
  def insert(systemInfo: SystemInfo): Unit

  /** Get system info entry by its ID. */
  def get(id: String): Option[SystemInfo]

  /** Get all system info entries. */
  def getAll(): Seq[SystemInfo]

  /** Updates a system info entry. */
  def update(systemInfo: SystemInfo, upsert: Boolean = false): Unit

  /** Delete a system info entry by its ID. */
  def delete(id: String): Unit

  implicit val formats = Serialization.formats(NoTypeHints)

  /** Backup all data as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll()).getBytes("UTF-8")

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[SystemInfo]] = {
    try {
      val rdata = Serialization.read[Seq[SystemInfo]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { update(_, true) }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}

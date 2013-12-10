package io.prediction.commons.settings

import io.prediction.commons.Common

import com.github.nscala_time.time.Imports._
import org.json4s._
import org.json4s.native.Serialization

/**
 * OfflineTune Object
 *
 * @param id Id
 * @param engineid The Engine ID
 * @param loops Number of offline tune loops
 * @param createtime The Creation time of the offline tune
 * @param starttime The Starting time of the offline tune
 * @param endtime The End time of the the offline tune
 */
case class OfflineTune(
  id: Int,
  engineid: Int,
  loops: Int,
  createtime: Option[DateTime],
  starttime: Option[DateTime],
  endtime: Option[DateTime])

trait OfflineTunes extends Common {

  /** Insert an OfflineTune and return its ID. */
  def insert(offlineTune: OfflineTune): Int

  /** Get OfflineTune by its ID. */
  def get(id: Int): Option[OfflineTune]

  /** Get all OfflineTunes. */
  def getAll(): Iterator[OfflineTune]

  /** Get OfflineTune's by Engine ID. */
  def getByEngineid(engineid: Int): Iterator[OfflineTune]

  /** Update OfflineTune (create new one if the it doesn't exist). */
  def update(offlineTune: OfflineTune, upsert: Boolean = false)

  /** Delete OfflineTune by its ID. */
  def delete(id: Int)

  implicit val formats = Serialization.formats(NoTypeHints) ++ org.json4s.ext.JodaTimeSerializers.all

  /** Backup all data as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll().toSeq).getBytes("UTF-8")

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[OfflineTune]] = {
    try {
      val rdata = Serialization.read[Seq[OfflineTune]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { update(_, true) }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}

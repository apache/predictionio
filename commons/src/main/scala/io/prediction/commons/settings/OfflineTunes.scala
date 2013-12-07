package io.prediction.commons.settings

import io.prediction.commons.Common

import com.github.nscala_time.time.Imports._
import com.twitter.chill.KryoInjection

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

  /** Backup all data as a byte array. */
  def backup(): Array[Byte] = {
    val backup = getAll().toSeq.map { b =>
      Map(
        "id" -> b.id,
        "engineid" -> b.engineid,
        "loops" -> b.loops,
        "createtime" -> b.createtime,
        "starttime" -> b.starttime,
        "endtime" -> b.endtime)
    }
    KryoInjection(backup)
  }

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[OfflineTune]] = {
    KryoInjection.invert(bytes) map { r =>
      val rdata = r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        OfflineTune(
          id = data("id").asInstanceOf[Int],
          engineid = data("engineid").asInstanceOf[Int],
          loops = data("loops").asInstanceOf[Int],
          createtime = data("createtime").asInstanceOf[Option[DateTime]],
          starttime = data("starttime").asInstanceOf[Option[DateTime]],
          endtime = data("endtime").asInstanceOf[Option[DateTime]])
      }

      if (inplace) rdata foreach { update(_, true) }

      rdata
    }
  }
}

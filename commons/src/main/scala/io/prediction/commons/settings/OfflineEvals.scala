package io.prediction.commons.settings

import io.prediction.commons.Common

import com.twitter.chill.KryoInjection

import com.github.nscala_time.time.Imports._

/** OfflineEval object
 *
 * @param id ID.
 * @param engineid The id of Engine object which owns this OfflineEval.
 * @param name The name of the this Offline Evaluation.
 * @param tuneid The OfflineTune ID
 * @param createtime The Creation time of the evaluation
 * @param starttime The Starting time of the evaluation
 * @param endtime The End time of the the evaluation. It's still running if it's None.
 * @param iterations Number of iterations. Default to 1.
 */
case class OfflineEval(
  id: Int,
  engineid: Int,
  name: String,
  iterations: Int = 1,
  tuneid: Option[Int],
  createtime: Option[DateTime],
  starttime: Option[DateTime],
  endtime: Option[DateTime]
)

trait OfflineEvals extends Common {

  /** Insert an OfflineEval and return id
   *
   * NOTE: can't use id of the offlineEval parameter
   */
  def insert(offlineEval: OfflineEval): Int

  /** Get OfflineEval by its id */
  def get(id: Int): Option[OfflineEval]

  /** Get all OfflineEvals. */
  def getAll(): Iterator[OfflineEval]

  /** Get OfflineEval by engine id */
  def getByEngineid(engineid: Int): Iterator[OfflineEval]

  /** Get OfflineEval by offline tune id */
  def getByTuneid(tuneid: Int): Iterator[OfflineEval]

  /** Update OfflineEval (create new one if the it doesn't exist) */
  def update(offlineEval: OfflineEval, upsert: Boolean = false)

  /** delete OfflineEval by it's id) */
  def delete(id: Int)

  /** Backup all data as a byte array. */
  def backup(): Array[Byte] = {
    val backup = getAll().toSeq.map { b =>
      Map(
        "id" -> b.id,
        "engineid" -> b.engineid,
        "name" -> b.name,
        "iterations" -> b.iterations,
        "tuneid" -> b.tuneid,
        "createtime" -> b.createtime,
        "starttime" -> b.starttime,
        "endtime" -> b.endtime)
    }
    KryoInjection(backup)
  }

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], upgrade: Boolean = false): Option[Seq[OfflineEval]] = {
    KryoInjection.invert(bytes) map { r =>
      r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        OfflineEval(
          id = data("id").asInstanceOf[Int],
          engineid = data("engineid").asInstanceOf[Int],
          name = data("name").asInstanceOf[String],
          iterations = data("iterations").asInstanceOf[Int],
          tuneid = data("tuneid").asInstanceOf[Option[Int]],
          createtime = data("createtime").asInstanceOf[Option[DateTime]],
          starttime = data("starttime").asInstanceOf[Option[DateTime]],
          endtime = data("endtime").asInstanceOf[Option[DateTime]])
      }
    }
  }
}

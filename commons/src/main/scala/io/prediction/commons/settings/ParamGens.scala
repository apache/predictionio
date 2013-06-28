package io.prediction.commons.settings

import io.prediction.commons.Common

import com.twitter.chill.KryoInjection

/** ParamGen Object
 *
 * @param id ID
 * @param infoid param gen info id
 * @param tuneid ID of the OfflineTune
 * @param params param gen parameters as key-value pairs
 */
case class ParamGen(
  id: Int,
  infoid: String,
  tuneid: Int,
  params: Map[String, Any]
)

trait ParamGens extends Common {

  /** Insert a paramGen and return ID */
  def insert(paramGen: ParamGen): Int

  /** Get a paramGen by its ID */
  def get(id: Int): Option[ParamGen]

  /** Get all parameter generators. */
  def getAll(): Iterator[ParamGen]

  /** Get paramGen by offline tune ID */
  def getByTuneid(tuneid: Int): Iterator[ParamGen]

  /** Update paramGen */
  def update(paramGen: ParamGen, upsert: Boolean = false)

  /** Delete paramGen by its ID */
  def delete(id: Int)

  /** Backup all data as a byte array. */
  def backup(): Array[Byte] = {
    val backup = getAll().toSeq.map { b =>
      Map(
        "id" -> b.id,
        "infoid" -> b.infoid,
        "tuneid" -> b.tuneid,
        "params" -> b.params)
    }
    KryoInjection(backup)
  }

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[ParamGen]] = {
    KryoInjection.invert(bytes) map { r =>
      val rdata = r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        ParamGen(
          id = data("id").asInstanceOf[Int],
          infoid = data("infoid").asInstanceOf[String],
          tuneid = data("tuneid").asInstanceOf[Int],
          params = data("params").asInstanceOf[Map[String, Any]])
      }

      if (inplace) rdata foreach { update(_, true) }

      rdata
    }
  }
}

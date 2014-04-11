package io.prediction.commons.settings

import io.prediction.commons.Common

import org.json4s._
import org.json4s.native.Serialization

/**
 * AlgoInfo object.
 *
 * @param id Unique identifier. Usually identical to the algorithm's namespace.
 * @param name Algorithm name.
 * @param description A long description of the algorithm.
 * @param batchcommands Command templates for running the algorithm in batch mode.
 * @param offlineevalcommands Command templates for running the algorithm in offline evaluation mode.
 * @param params Map of Param objects, with keys equal to IDs of Param objects it contains.
 * @param paramorder The display order of parameters.
 * @param engineinfoid The EngineInfo ID of the engine that can run this algorithm.
 * @param techreq Technology requirement for this algorithm to run.
 * @param datareq Data requirement for this algorithm to run.
 * @param capabilities Engine features that this algorithm can handle.
 */
case class AlgoInfo(
  id: String,
  name: String,
  description: Option[String],
  batchcommands: Option[Seq[String]],
  offlineevalcommands: Option[Seq[String]],
  params: Map[String, Param],
  paramsections: Seq[ParamSection],
  paramorder: Seq[String],
  engineinfoid: String,
  techreq: Seq[String],
  datareq: Seq[String],
  capabilities: Seq[String] = Seq()) extends Info

/** Base trait for implementations that interact with algo info in the backend data store. */
trait AlgoInfos extends Common {
  /** Insert an algo info. */
  def insert(algoInfo: AlgoInfo): Unit

  /** Get algo info by its ID. */
  def get(id: String): Option[AlgoInfo]

  /** Get all algo info. */
  def getAll(): Seq[AlgoInfo]

  /** Get algo info by their engine type. */
  def getByEngineInfoId(engineinfoid: String): Seq[AlgoInfo]

  /** Update an algo info. */
  def update(algoInfo: AlgoInfo, upsert: Boolean = false): Unit

  /** Delete an algo info by its ID. */
  def delete(id: String): Unit

  implicit val formats = Serialization.formats(NoTypeHints) + new ParamSerializer

  /** Backup all AlgoInfos as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll()).getBytes("UTF-8")

  /** Restore AlgoInfos from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[AlgoInfo]] = {
    try {
      val rdata = Serialization.read[Seq[AlgoInfo]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { update(_, true) }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}

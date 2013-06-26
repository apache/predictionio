package io.prediction.commons.settings

import io.prediction.commons.Common

import com.twitter.chill.KryoInjection

/** AlgoInfo object.
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
  */
case class AlgoInfo(
  id: String,
  name: String,
  description: Option[String],
  batchcommands: Option[Seq[String]],
  offlineevalcommands: Option[Seq[String]],
  params: Map[String, Param],
  paramorder: Seq[String],
  engineinfoid: String,
  techreq: Seq[String],
  datareq: Seq[String]
)

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
  def update(algoInfo: AlgoInfo): Unit

  /** Delete an algo info by its ID. */
  def delete(id: String): Unit

  /** Backup all AlgoInfos as a byte array. */
  def backup(): Array[Byte] = {
    val algoinfos = getAll().map { algoinfo =>
      Map(
        "id" -> algoinfo.id,
        "name" -> algoinfo.name,
        "description" -> algoinfo.description,
        "batchcommands" -> algoinfo.batchcommands,
        "offlineevalcommands" -> algoinfo.offlineevalcommands,
        "params" -> algoinfo.params,
        "paramorder" -> algoinfo.paramorder,
        "engineinfoid" -> algoinfo.engineinfoid,
        "techreq" -> algoinfo.techreq,
        "datareq" -> algoinfo.datareq)
    }
    KryoInjection(algoinfos)
  }

  /** Restore AlgoInfos from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], upgrade: Boolean = false): Option[Seq[AlgoInfo]] = {
    KryoInjection.invert(bytes) map { r =>
      r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        AlgoInfo(
          id = data("id").asInstanceOf[String],
          name = data("name").asInstanceOf[String],
          description = data("description").asInstanceOf[Option[String]],
          batchcommands = data("batchcommands").asInstanceOf[Option[Seq[String]]],
          offlineevalcommands = data("offlineevalcommands").asInstanceOf[Option[Seq[String]]],
          params = data("params").asInstanceOf[Map[String, Param]],
          paramorder = data("paramorder").asInstanceOf[Seq[String]],
          engineinfoid = data("engineinfoid").asInstanceOf[String],
          techreq = data("techreq").asInstanceOf[Seq[String]],
          datareq = data("datareq").asInstanceOf[Seq[String]])
      }
    }
  }
}

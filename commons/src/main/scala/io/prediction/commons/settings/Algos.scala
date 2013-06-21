package io.prediction.commons.settings

import com.github.nscala_time.time.Imports._
import com.twitter.chill.KryoInjection

/** Algo object.
  *
  * @param id ID.
  * @param engineid App ID that owns this engine.
  * @param name Algo name.
  * @param infoid AlgoInfo ID
  * @param command Command template for running the algo.
  * @param params Algo parameters as key-value pairs.
  * @param settings Algo settings as key-value pairs.
  * @param modelset Indicates which model output set to be used by the API.
  * @param status The status of the algo. eg "ready", "tuning".
  * @param offlineevalid The id of OfflineEval which uses this algo for offline evaluation
  * @param offlinetuneid The id of OfflineTune
  * @param loop The iteration number used by auto tune. (NOTE: loop=0 reserved for baseline algo)
  * @param paramset The param generation set number
  */
case class Algo(
  id: Int,
  engineid: Int,
  name: String,
  infoid: String,
  command: String,
  params: Map[String, Any],
  settings: Map[String, Any],
  modelset: Boolean,
  createtime: DateTime,
  updatetime: DateTime,
  status: String = "",
  offlineevalid: Option[Int],
  offlinetuneid: Option[Int] = None,
  loop: Option[Int] = None,
  paramset: Option[Int] = None
)

/** Base trait for implementations that interact with algos in the backend data store. */
trait Algos {
  /** Inserts an algo. */
  def insert(algo: Algo): Int

  /** Get an algo by its ID. */
  def get(id: Int): Option[Algo]

  /** Get all algos. */
  def getAll(): Iterator[Algo]

  /** Get algos by engine ID. */
  def getByEngineid(engineid: Int): Iterator[Algo]

  /** Get deployed algos by engine ID. */
  def getDeployedByEngineid(engineid: Int): Iterator[Algo]

  /** Get by OfflineEval ID. */
  def getByOfflineEvalid(evalid: Int, loop: Option[Int] = None, paramset: Option[Int] = None): Iterator[Algo]

  /** Get the auto tune subject by OfflineTune ID. */
  def getTuneSubjectByOfflineTuneid(tuneid: Int): Option[Algo]

  /** Update an algo. */
  def update(algo: Algo)

  /** Delete an algo by its ID. */
  def delete(id: Int)

  /** Check existence of an algo by its engine ID and name.
    * Algos that are part of an offline evaluation or tuning are not counted.
    */
  def existsByEngineidAndName(engineid: Int, name: String): Boolean

  def backup(): Array[Byte] = {
    val algos = getAll().toSeq.map { algo =>
      Map(
        "id" -> algo.id,
        "engineid" -> algo.engineid,
        "name" -> algo.name,
        "infoid" -> algo.infoid,
        "command" -> algo.command,
        "params" -> algo.params,
        "settings" -> algo.settings,
        "modelset" -> algo.modelset,
        "createtime" -> algo.createtime,
        "updatetime" -> algo.updatetime,
        "status" -> algo.status,
        "offlineevalid" -> algo.offlineevalid,
        "offlinetuneid" -> algo.offlinetuneid,
        "loop" -> algo.loop,
        "paramset" -> algo.paramset)
    }
    KryoInjection(algos)
  }

  def restore(bytes: Array[Byte], upgrade: Boolean = false): Option[Seq[Algo]] = {
    KryoInjection.invert(bytes) map { r =>
      r.asInstanceOf[Seq[Map[String, Any]]] map { stuff =>
        Algo(
          id = stuff("id").asInstanceOf[Int],
          engineid = stuff("engineid").asInstanceOf[Int],
          name = stuff("name").asInstanceOf[String],
          infoid = stuff("infoid").asInstanceOf[String],
          command = stuff("command").asInstanceOf[String],
          params = stuff("params").asInstanceOf[Map[String, Any]],
          settings = stuff("settings").asInstanceOf[Map[String, Any]],
          modelset = stuff("modelset").asInstanceOf[Boolean],
          createtime = stuff("createtime").asInstanceOf[DateTime],
          updatetime = stuff("updatetime").asInstanceOf[DateTime],
          status = stuff("status").asInstanceOf[String],
          offlineevalid = stuff("offlineevalid").asInstanceOf[Option[Int]],
          offlinetuneid = stuff("offlinetuneid").asInstanceOf[Option[Int]],
          loop = stuff("loop").asInstanceOf[Option[Int]],
          paramset = stuff("paramset").asInstanceOf[Option[Int]])
      }
    }
  }
}

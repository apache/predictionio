package io.prediction.commons.settings

import io.prediction.commons.Common

import com.github.nscala_time.time.Imports._
import org.json4s._
import org.json4s.native.Serialization

/**
 * Algo object.
 *
 * @param id ID.
 * @param engineid App ID that owns this engine.
 * @param name Algo name.
 * @param infoid AlgoInfo ID
 * @param command Command template for running the algo.
 * @param params Algo parameters as key-value pairs.
 * @param settings Algo settings as key-value pairs.
 * @param modelset Indicates which model output set to be used by the API.
 * @param createtime Creation time of this Algo.
 * @param updatetime Last update time of this Algo's settings.
 * @param status The status of the algo. eg "ready", "tuning".
 * @param offlineevalid The id of OfflineEval which uses this algo for offline evaluation
 * @param offlinetuneid The id of OfflineTune
 * @param loop The iteration number used by auto tune. (NOTE: loop=0 reserved for baseline algo)
 * @param paramset The param generation set number
 * @param lasttraintime Time of last successful training.
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
  paramset: Option[Int] = None,
  lasttraintime: Option[DateTime] = None)

/** Base trait for implementations that interact with algos in the backend data store. */
trait Algos extends Common {
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

  /** Get the algo by its ID and engine ID. */
  def getByIdAndEngineid(id: Int, engineid: Int): Option[Algo]

  /** Update an algo. */
  def update(algo: Algo, upsert: Boolean = false)

  /** Delete an algo by its ID. */
  def delete(id: Int)

  /**
   * Check existence of an algo by its engine ID and name.
   * Algos that are part of an offline evaluation or tuning are not counted.
   */
  def existsByEngineidAndName(engineid: Int, name: String): Boolean

  implicit val formats = Serialization.formats(NoTypeHints) + new AlgoSerializer

  /** Backup all Algos as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll().toSeq).getBytes("UTF-8")

  /** Restore Algos from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[Algo]] = {
    try {
      val rdata = Serialization.read[Seq[Algo]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { update(_, true) }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}

/** json4s serializer for the Algo class. */
class AlgoSerializer extends CustomSerializer[Algo](format => (
  {
    case x: JObject =>
      implicit val formats = Serialization.formats(NoTypeHints) ++ org.json4s.ext.JodaTimeSerializers.all
      Algo(
        id = (x \ "id").extract[Int],
        engineid = (x \ "engineid").extract[Int],
        name = (x \ "name").extract[String],
        infoid = (x \ "infoid").extract[String],
        command = (x \ "command").extract[String],
        params = Common.sanitize((x \ "params").asInstanceOf[JObject].values),
        settings = Common.sanitize((x \ "settings").asInstanceOf[JObject].values),
        modelset = (x \ "modelset").extract[Boolean],
        createtime = (x \ "createtime").extract[DateTime],
        updatetime = (x \ "updatetime").extract[DateTime],
        status = (x \ "status").extract[String],
        offlineevalid = (x \ "offlineevalid").extract[Option[Int]],
        offlinetuneid = (x \ "offlinetuneid").extract[Option[Int]],
        loop = (x \ "loop").extract[Option[Int]],
        paramset = (x \ "paramset").extract[Option[Int]],
        lasttraintime = (x \ "lasttraintime").extract[Option[DateTime]])
  },
  {
    case x: Algo =>
      implicit val formats = Serialization.formats(NoTypeHints) ++ org.json4s.ext.JodaTimeSerializers.all
      JObject(
        JField("id", Extraction.decompose(x.id)) ::
          JField("engineid", Extraction.decompose(x.engineid)) ::
          JField("name", Extraction.decompose(x.name)) ::
          JField("infoid", Extraction.decompose(x.infoid)) ::
          JField("command", Extraction.decompose(x.command)) ::
          JField("params", Extraction.decompose(x.params)) ::
          JField("settings", Extraction.decompose(x.settings)) ::
          JField("modelset", Extraction.decompose(x.modelset)) ::
          JField("createtime", Extraction.decompose(x.createtime)) ::
          JField("updatetime", Extraction.decompose(x.updatetime)) ::
          JField("status", Extraction.decompose(x.status)) ::
          JField("offlineevalid", Extraction.decompose(x.offlineevalid)) ::
          JField("offlinetuneid", Extraction.decompose(x.offlinetuneid)) ::
          JField("loop", Extraction.decompose(x.loop)) ::
          JField("paramset", Extraction.decompose(x.paramset)) ::
          JField("lasttraintime", Extraction.decompose(x.lasttraintime)) :: Nil)
  })
)

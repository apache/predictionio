package io.prediction.commons.settings

import io.prediction.commons.Common

import org.json4s._
import org.json4s.native.Serialization

/**
 * Engine object.
 *
 * @param id ID.
 * @param appid App ID that owns this engine.
 * @param name Engine name.
 * @param infoid EngineInfo ID.
 * @param itypes List of item types.
 * @param params Engine parameters as key-value pairs.
 * @param trainingdisabled Whether training is disabled or not. If value is undefined, assume training is not disabled.
 * @param trainingschedule Training schedule of this engine in cron expression. Default to an hourly schedule at 0 minute.
 */
case class Engine(
  id: Int,
  appid: Int,
  name: String,
  infoid: String,
  itypes: Option[Seq[String]],
  params: Map[String, Any],
  trainingdisabled: Option[Boolean] = None,
  trainingschedule: Option[String] = None)

/** Base trait for implementations that interact with engines in the backend data store. */
trait Engines extends Common {
  /** Inserts an engine. */
  def insert(engine: Engine): Int

  /** Get an engine by its ID. */
  def get(id: Int): Option[Engine]

  /** Get all engines. */
  def getAll(): Iterator[Engine]

  /** Get engines by app ID. */
  def getByAppid(appid: Int): Iterator[Engine]

  /** Get an engine by app ID and name. */
  def getByAppidAndName(appid: Int, name: String): Option[Engine]

  /** Get an engine by its ID and app ID. */
  def getByIdAndAppid(id: Int, appid: Int): Option[Engine]

  /** Update an engine. */
  def update(engine: Engine, upsert: Boolean = false)

  /** Delete an engine by its ID and app ID. */
  def deleteByIdAndAppid(id: Int, appid: Int)

  /** Check existence of an engine by its app ID and name. */
  def existsByAppidAndName(appid: Int, name: String): Boolean

  implicit val formats = Serialization.formats(NoTypeHints) + new EngineSerializer

  /** Backup all Engines as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll().toSeq).getBytes("UTF-8")

  /** Restore Engines from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[Engine]] = {
    try {
      val rdata = Serialization.read[Seq[Engine]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { update(_, true) }
      Some(rdata)
    } catch {
      case e: MappingException => { println(e.getMessage()); None }
    }
  }
}

/** json4s serializer for the Engine class. */
class EngineSerializer extends CustomSerializer[Engine](format => (
  {
    case x: JObject =>
      implicit val formats = Serialization.formats(NoTypeHints)
      Engine(
        id = (x \ "id").extract[Int],
        appid = (x \ "appid").extract[Int],
        name = (x \ "name").extract[String],
        infoid = (x \ "infoid").extract[String],
        itypes = (x \ "itypes").extract[Option[Seq[String]]],
        params = Common.sanitize((x \ "params").asInstanceOf[JObject].values),
        trainingdisabled = (x \ "trainingdisabled").extract[Option[Boolean]],
        trainingschedule = (x \ "trainingschedule").extract[Option[String]])
  },
  {
    case x: Engine =>
      implicit val formats = Serialization.formats(NoTypeHints)
      JObject(
        JField("id", Extraction.decompose(x.id)) ::
          JField("appid", Extraction.decompose(x.appid)) ::
          JField("name", Extraction.decompose(x.name)) ::
          JField("infoid", Extraction.decompose(x.infoid)) ::
          JField("itypes", Extraction.decompose(x.itypes)) ::
          JField("params", Extraction.decompose(x.params)) ::
          JField("trainingdisabled", Extraction.decompose(x.trainingdisabled)) ::
          JField("trainingschedule", Extraction.decompose(x.trainingschedule)) :: Nil)
  })
)

package io.prediction.commons.settings

import io.prediction.commons.Common

import com.twitter.chill.KryoInjection

/** Engine object.
  *
  * @param id ID.
  * @param appid App ID that owns this engine.
  * @param name Engine name.
  * @param infoid EngineInfo ID.
  * @param itypes List of item types.
  * @param params Engine parameters as key-value pairs.
  */
case class Engine(
  id: Int,
  appid: Int,
  name: String,
  infoid: String,
  itypes: Option[Seq[String]],
  params: Map[String, Any]
)

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

  /** Backup all Engines as a byte array. */
  def backup(): Array[Byte] = {
    val engines = getAll().toSeq.map { engine =>
      Map(
        "id" -> engine.id,
        "appid" -> engine.appid,
        "name" -> engine.name,
        "infoid" -> engine.infoid,
        "itypes" -> engine.itypes,
        "params" -> engine.params)
    }
    KryoInjection(engines)
  }

  /** Restore Engines from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[Engine]] = {
    KryoInjection.invert(bytes) map { r =>
      val rdata = r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        Engine(
          id = data("id").asInstanceOf[Int],
          appid = data("appid").asInstanceOf[Int],
          name = data("name").asInstanceOf[String],
          infoid = data("infoid").asInstanceOf[String],
          itypes = data("itypes").asInstanceOf[Option[List[String]]],
          params = data("params").asInstanceOf[Map[String, Any]])
      }

      if (inplace) rdata foreach { update(_, true) }

      rdata
    }
  }
}

package io.prediction.commons.settings

/** Engine object.
  *
  * @param id ID.
  * @param appid App ID that owns this engine.
  * @param name Engine name.
  * @param enginetype Engine type (itemrec, itemsim, etc).
  * @param itypes List of item types.
  * @param settings Engine settings as key-value pairs.
  */
case class Engine(
  id: Int,
  appid: Int,
  name: String,
  enginetype: String,
  itypes: Option[List[String]],
  settings: Map[String, Any]
)

/** Base trait for implementations that interact with engines in the backend data store. */
trait Engines {
  /** Inserts an engine. */
  def insert(engine: Engine): Int

  /** Get an engine by its ID. */
  def get(id: Int): Option[Engine]

  /** Get engines by app ID. */
  def getByAppid(appid: Int): Iterator[Engine]

  /** Get an engine by its ID and app ID. */
  def getByAppidAndName(appid: Int, name: String): Option[Engine]

  /** Update an engine. */
  def update(engine: Engine)

  /** Delete an engine by its ID and app ID. */
  def deleteByIdAndAppid(id: Int, appid: Int)

  /** Check existence of an engine by its app ID and name. */
  def existsByAppidAndName(appid: Int, name: String): Boolean
}

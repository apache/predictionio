package io.prediction.storage

/**
 * EngineManifest object.
 *
 * The system should be able to host multiple versions of an engine with the
 * same ID.
 *
 * @param id Unique identifier of an engine.
 * @param version Engine version string.
 * @param name A short and descriptive name for the engine.
 * @param description A long description of the engine.
 * @param jar Absolute path to the engine JAR assembly with all dependencies.
 * @param engineFactory Engine's factory class name.
 * @param evaluatorFactory Engine's evaluator factory class name. (TODO: May refactor.)
 */
case class EngineManifest(
  id: String,
  version: String,
  name: String,
  description: Option[String],
  jar: String,
  engineFactory: String,
  evaluatorFactory: String)

/** Base trait for implementations that interact with engine manifests in the backend data store. */
trait EngineManifests {
  /** Inserts an engine manifest. */
  def insert(engineManifest: EngineManifest): Unit

  /** Get an engine manifest by its ID. */
  def get(id: String, version: String): Option[EngineManifest]

  /** Get all engine manifest. */
  def getAll(): Seq[EngineManifest]

  /** Updates an engine manifest. */
  def update(engineInfo: EngineManifest, upsert: Boolean = false): Unit

  /** Delete an engine manifest by its ID. */
  def delete(id: String, version: String): Unit
}

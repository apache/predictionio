package io.prediction.storage

import com.typesafe.config._
import grizzled.slf4j.Logging

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._

/**
 * Settings accessors.
 *
 * This class ensures its users that the configuration is free of error, and
 * provides default values as necessary.
 */
object Settings extends Logging {
  private val config: Config = try {
    ConfigFactory.load()
  } catch {
    case e: ConfigException =>
      error(e.getMessage)
      System.exit(1)
      throw e // won't actually throw. just to pass type safety check
  }

  private var errors = 0

  private def prefixPath(prefix: String, body: String) = s"${prefix}.${body}"

  private val storageSourcesPrefix = "io.prediction.storage.sources"
  private def storageSourcesPrefixPath(body: String) =
    prefixPath(storageSourcesPrefix, body)

  private val storageSourcesKeys: Seq[String] = try {
    config.getObject(storageSourcesPrefix).keySet.toSeq
  } catch {
    case e: ConfigException =>
      error(s"Configuration has no valid storage sources! (${e.getMessage})")
      errors += 1
      Seq[String]()
  }

  private case class Tagged(tag: String, stuff: Seq[AnyRef])
  private case class TaggedClient(tag: String, client: BaseStorageClient)

  private val storageSources: Map[String, TaggedClient] =
    storageSourcesKeys.map(k =>
      try {
        val keyedPath = storageSourcesPrefixPath(k)
        val sourceTypePath = prefixPath(keyedPath, "type")
        val sourceType = config.getString(sourceTypePath)
        val hosts = config.getStringList(prefixPath(keyedPath, "hosts"))
        val ports = config.getIntList(prefixPath(keyedPath, "ports")).
          map(_.intValue)
        val clientConfig = StorageClientConfig(hosts = hosts, ports = ports)
        val client = getClient(clientConfig, sourceType)
        k -> TaggedClient(sourceType, client)
      } catch {
        case e: ConfigException =>
          error(e.getMessage)
          errors += 1
          k -> TaggedClient("", null)
      }
    ).toMap

  /** Reference to the app data repository. */
  val AppDataRepository = "appdata"
  /** Reference to the settings repository. */
  val SettingsRepository = "settings"

  private val repositoriesPrefix = "io.prediction.storage.repositories"
  private def repositoriesPrefixPath(body: String) =
    prefixPath(repositoriesPrefix, body)
  private val requiredRepositories = Seq(AppDataRepository, SettingsRepository)
  private val repositories: Seq[String] = try {
    config.getObject(repositoriesPrefix).keySet.toSeq
  } catch {
    case e: ConfigException =>
      error(s"Configuration has no valid repositories! (${e.getMessage})")
      errors += 1
      Seq[String]()
  }
  requiredRepositories foreach { r =>
    if (!repositories.contains(r)) {
      error(s"Required repository (${r}) configuration is missing.")
      errors += 1
    }
  }
  private val repositoriesDatabase: Map[String, Tagged] =
    repositories.map(r =>
      try {
        val keyedPath = repositoriesPrefixPath(r)
        val name = config.getString(prefixPath(keyedPath, "name"))
        val sourceName = config.getString(prefixPath(keyedPath, "source"))
        val source = storageSources.get(sourceName)
        source map { s =>
          r -> Tagged(sourceName, Seq(s.client.client, name))
        } getOrElse {
          throw new ConfigException.BadValue(
            config.getValue(prefixPath(keyedPath, "source")).origin,
            prefixPath(keyedPath, "source"),
            s"$sourceName is not a configured storage source.")
        }
      } catch {
        case e: ConfigException =>
          error(e.getMessage)
          errors += 1
          r -> Tagged("", Seq())
      }
    ).toMap

  private def getClient(
      clientConfig: StorageClientConfig,
      pkg: String,
      prefix: String = "io.prediction.storage"): BaseStorageClient = {
    val className = if (prefix == "") pkg else prefix + "." + pkg +
      ".StorageClient"
    Class.forName(className).getConstructors()(0).newInstance(clientConfig).
      asInstanceOf[BaseStorageClient]
  }

  private def getDataObject[T](
      repo: String,
      pkg: String = "io.prediction.storage")
      (implicit tag: TypeTag[T]): T = {
    val repoSource = repositoriesDatabase(repo)
    val repoSourceType = repoSource.tag
    val classPrefix = storageSources(repoSourceType).client.prefix
    val originalClassName = tag.tpe.toString.split('.')
    // below is an attempt to use relative package namespace
    val className = pkg +
      (if (pkg == "") ""  else ".") +
      repoSourceType + "." + classPrefix + originalClassName.last
    try {
      Class.forName(className).getConstructors()(0).
        newInstance(repoSource.stuff: _*).asInstanceOf[T]
    } catch {
      case e: IllegalArgumentException =>
        error(s"Unable to instantiate data object with class ${className}." +
          s" Config keys: ${repoSource}." +
          s" Exception message: ${e.getMessage}).")
        errors += 1
        throw e
    }
  }

  /** The base directory of PredictionIO deployment/repository. */
  val base: String = config.getString("io.prediction.base")

  def getSettingsEngineManifests(): EngineManifests = {
    getDataObject[EngineManifests](SettingsRepository)
  }

  def getSettingsRuns(): Runs = {
    getDataObject[Runs](SettingsRepository)
  }

  /** Obtains an ItemTrends object with configured backend type. */
  def getAppdataItemTrends(): ItemTrends = {
    getDataObject[ItemTrends](AppDataRepository)
  }

  /** Obtains a Users object with configured backend type. */
  def getAppdataUsers(): Users = {
    getDataObject[Users](AppDataRepository)
  }

  /** Obtains an Items object with configured backend type. */
  def getAppdataItems(): Items = {
    getDataObject[Items](AppDataRepository)
  }

  /** Obtains a U2IActions object with configured backend type. */
  def getAppdataU2IActions(): U2IActions = {
    getDataObject[U2IActions](AppDataRepository)
  }

  def getAppdataItemSets(): ItemSets = {
    getDataObject[ItemSets](AppDataRepository)
  }

  if (errors > 0) {
    error(s"There were $errors configuration errors. Exiting.")
    System.exit(1)
  }
}

trait BaseStorageClient {
  val config: StorageClientConfig
  val client: AnyRef
  val prefix: String = ""
}

case class StorageClientConfig(
  hosts: Seq[String],
  ports: Seq[Int])

class StorageClientException(msg: String) extends RuntimeException(msg)

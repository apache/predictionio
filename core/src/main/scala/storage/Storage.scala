package io.prediction.storage

import com.typesafe.config._
import grizzled.slf4j.Logging

import scala.collection.JavaConversions._
import scala.language.existentials
import scala.reflect.runtime.universe._

/**
 * Settings accessors.
 *
 * This class ensures its users that the configuration is free of error, and
 * provides default values as necessary.
 */
object Storage extends Logging {
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

  private val sourcesPrefix = "io.prediction.storage.sources"
  private def sourcesPrefixPath(body: String) =
    prefixPath(sourcesPrefix, body)

  private val sourcesKeys: Seq[String] = try {
    config.getObject(sourcesPrefix).keySet.toSeq
  } catch {
    case e: ConfigException =>
      error(s"Configuration has no valid storage sources! (${e.getMessage})")
      errors += 1
      Seq[String]()
  }

  private case class ClientMeta(sourceType: String, client: BaseStorageClient)
  private case class DataObjectMeta(sourceName: String, databaseName: String)

  private val sourcesToClientMeta: Map[String, ClientMeta] =
    sourcesKeys.map(k =>
      try {
        val keyedPath = sourcesPrefixPath(k)
        val sourceTypePath = prefixPath(keyedPath, "type")
        val sourceType = config.getString(sourceTypePath)
        val hosts = config.getStringList(prefixPath(keyedPath, "hosts"))
        val ports = config.getIntList(prefixPath(keyedPath, "ports")).
          map(_.intValue)
        val clientConfig = StorageClientConfig(hosts = hosts, ports = ports)
        val client = getClient(clientConfig, sourceType)
        k -> ClientMeta(sourceType, client)
      } catch {
        case e: ConfigException =>
          error(e.getMessage)
          errors += 1
          k -> ClientMeta("", null)
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
  private val repositoriesToDataObjectMeta: Map[String, DataObjectMeta] =
    repositories.map(r =>
      try {
        val keyedPath = repositoriesPrefixPath(r)
        val name = config.getString(prefixPath(keyedPath, "name"))
        val sourceName = config.getString(prefixPath(keyedPath, "source"))
        val clientMeta = sourcesToClientMeta.get(sourceName)
        clientMeta map { cm =>
          r -> DataObjectMeta(
            sourceName = sourceName,
            databaseName = name)
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
          r -> DataObjectMeta("", "")
      }
    ).toMap

  def getClient(
      clientConfig: StorageClientConfig,
      pkg: String): BaseStorageClient = {
    val className = "io.prediction.storage." + pkg + ".StorageClient"
    try {
      Class.forName(className).getConstructors()(0).newInstance(clientConfig).
        asInstanceOf[BaseStorageClient]
    } catch {
      case e: ClassNotFoundException =>
        val originalClassName = pkg + ".StorageClient"
        Class.forName(originalClassName).getConstructors()(0).
          newInstance(clientConfig).asInstanceOf[BaseStorageClient]
    }
  }

  def getClient(sourceName: String): Option[BaseStorageClient] =
    sourcesToClientMeta.get(sourceName).map(_.client)

  def getDataObject[T](repo: String)(implicit tag: TypeTag[T]): T = {
    val repoDOMeta = repositoriesToDataObjectMeta(repo)
    val repoDOSourceName = repoDOMeta.sourceName
    getDataObject[T](repoDOSourceName, repoDOMeta.databaseName)
  }

  def getDataObject[T](
      sourceName: String,
      databaseName: String)(implicit tag: TypeTag[T]): T = {
    val clientMeta = sourcesToClientMeta(sourceName)
    val sourceType = clientMeta.sourceType
    val ctorArgs = dataObjectCtorArgs(clientMeta.client, databaseName)
    val classPrefix = clientMeta.client.prefix
    val originalClassName = tag.tpe.toString.split('.')
    val rawClassName = sourceType + "." + classPrefix + originalClassName.last
    val className = "io.prediction.storage." + rawClassName
    val clazz = try {
      Class.forName(className)
    } catch {
      case e: ClassNotFoundException => Class.forName(rawClassName)
    }
    val constructor = clazz.getConstructors()(0)
    try {
      constructor.newInstance(ctorArgs: _*).
        asInstanceOf[T]
    } catch {
      case e: IllegalArgumentException =>
        error(
          "Unable to instantiate data object with class '" +
          constructor.getDeclaringClass.getName + " because its constructor" +
          " does not have the right number of arguments." +
          " Number of required constructor arguments: " +
          ctorArgs.size + "." +
          " Number of existing constructor arguments: " +
          constructor.getParameterTypes.size + "." +
          s" Storage source name: ${sourceName}." +
          s" Exception message: ${e.getMessage}).")
        errors += 1
        throw e
    }
  }

  private def dataObjectCtorArgs(
      client: BaseStorageClient,
      dbName: String): Seq[AnyRef] = {
    Seq(client.client, dbName)
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

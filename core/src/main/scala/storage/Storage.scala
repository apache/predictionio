package io.prediction.storage

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
  private var errors = 0

  private def prefixPath(prefix: String, body: String) = s"${prefix}_${body}"

  private val sourcesPrefix = "PIO_STORAGE_SOURCES"
  private def sourcesPrefixPath(body: String) =
    prefixPath(sourcesPrefix, body)

  private val sourceTypesRegex = """PIO_STORAGE_SOURCES_([^_]+)_TYPE""".r

  private val sourceKeys: Seq[String] = sys.env.keys.toSeq.flatMap { k =>
    sourceTypesRegex findFirstIn k match {
      case Some(sourceTypesRegex(sourceType)) => Seq(sourceType)
      case None => Nil
    }
  }

  if (sourceKeys.size == 0) warn("There is no properly configured data source.")

  private case class ClientMeta(sourceType: String, client: BaseStorageClient)
  private case class DataObjectMeta(sourceName: String, databaseName: String)

  private val sourcesToClientMeta: Map[String, ClientMeta] =
    sourceKeys.map(k =>
      try {
        val keyedPath = sourcesPrefixPath(k)
        val sourceType = sys.env(prefixPath(keyedPath, "TYPE"))
        val hosts = sys.env(prefixPath(keyedPath, "HOSTS")).split(',')
        val ports = sys.env(prefixPath(keyedPath, "PORTS")).split(',').
          map(_.toInt)
        val clientConfig = StorageClientConfig(hosts = hosts, ports = ports)
        val client = getClient(clientConfig, sourceType)
        k -> ClientMeta(sourceType, client)
      } catch {
        case e: Throwable =>
          error(e.getMessage)
          errors += 1
          k -> ClientMeta("", null)
      }
    ).toMap

  /** Reference to the app data repository. */
  val AppDataRepository = "APPDATA"
  val ModelDataRepository = "MODELDATA"
  val MetaDataRepository = "METADATA"

  private val repositoriesPrefix = "PIO_STORAGE_REPOSITORIES"
  private def repositoriesPrefixPath(body: String) =
    prefixPath(repositoriesPrefix, body)

  private val repositoryNamesRegex =
    """PIO_STORAGE_REPOSITORIES_([^_]+)_NAME""".r

  private val repositoryKeys: Seq[String] = sys.env.keys.toSeq.flatMap { k =>
    repositoryNamesRegex findFirstIn k match {
      case Some(repositoryNamesRegex(repositoryName)) => Seq(repositoryName)
      case None => Nil
    }
  }

  if (repositoryKeys.size == 0)
    warn("There is no properly configured repository.")

  private val requiredRepositories = Seq(MetaDataRepository)

  requiredRepositories foreach { r =>
    if (!repositoryKeys.contains(r)) {
      error(s"Required repository (${r}) configuration is missing.")
      errors += 1
    }
  }
  private val repositoriesToDataObjectMeta: Map[String, DataObjectMeta] =
    repositoryKeys.map(r =>
      try {
        val keyedPath = repositoriesPrefixPath(r)
        val name = sys.env(prefixPath(keyedPath, "NAME"))
        val sourceName = sys.env(prefixPath(keyedPath, "SOURCE"))
        val clientMeta = sourcesToClientMeta.get(sourceName)
        clientMeta map { cm =>
          r -> DataObjectMeta(
            sourceName = sourceName,
            databaseName = name)
        } getOrElse {
          error(s"$sourceName is not a configured storage source.")
          r -> DataObjectMeta("", "")
        }
      } catch {
        case e: Throwable =>
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

  def getMetaDataEngineManifests(): EngineManifests =
    getDataObject[EngineManifests](MetaDataRepository)

  def getMetaDataEngineInstances(): EngineInstances =
    getDataObject[EngineInstances](MetaDataRepository)

  def getModelDataModels(): Models =
    getDataObject[Models](ModelDataRepository)

  /** Obtains an ItemTrends object with configured backend type. */
  def getAppdataItemTrends(): ItemTrends =
    getDataObject[ItemTrends](AppDataRepository)

  /** Obtains a Users object with configured backend type. */
  def getAppdataUsers(): Users =
    getDataObject[Users](AppDataRepository)

  /** Obtains an Items object with configured backend type. */
  def getAppdataItems(): Items =
    getDataObject[Items](AppDataRepository)

  /** Obtains a U2IActions object with configured backend type. */
  def getAppdataU2IActions(): U2IActions =
    getDataObject[U2IActions](AppDataRepository)

  def getAppdataItemSets(): ItemSets =
    getDataObject[ItemSets](AppDataRepository)

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

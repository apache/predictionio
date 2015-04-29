/** Copyright 2015 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.data.storage

import grizzled.slf4j.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.existentials
import scala.reflect.runtime.universe._

@deprecated("Use StorageException", "0.9.2")
private[prediction] case class StorageError(message: String)

private[prediction] class StorageException(message: String, cause: Throwable)
  extends Exception(message, cause) {

  def this(message: String) = this(message, null)
}

/**
 * Backend-agnostic data storage layer with lazy initialization and connection
 * pooling. Use this object when you need to interface with Event Store in your
 * engine.
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

  private case class ClientMeta(
    sourceType: String,
    client: BaseStorageClient,
    config: StorageClientConfig)
  private case class DataObjectMeta(sourceName: String, namespace: String)

  private val s2cm = scala.collection.mutable.Map[String, Option[ClientMeta]]()
  private def updateS2CM(k: String, parallel: Boolean, test: Boolean):
    Option[ClientMeta] = {
    try {
      val keyedPath = sourcesPrefixPath(k)
      val sourceType = sys.env(prefixPath(keyedPath, "TYPE"))
      val props = sys.env.filter(t => t._1.startsWith(keyedPath)).map(
        t => t._1.replace(s"${keyedPath}_", "") -> t._2)
      val clientConfig = StorageClientConfig(
        properties = props,
        parallel = parallel,
        test = test)
      val client = getClient(clientConfig, sourceType)
      Some(ClientMeta(sourceType, client, clientConfig))
    } catch {
      case e: Throwable =>
        error(s"Error initializing storage client for source ${k}", e)
        errors += 1
        None
    }
  }
  private def sourcesToClientMeta(
      source: String,
      parallel: Boolean,
      test: Boolean): Option[ClientMeta] = {
    val sourceName = if (parallel) s"parallel-${source}" else source
    s2cm.getOrElseUpdate(sourceName, updateS2CM(source, parallel, test))
  }

  /** Reference to the app data repository. */
  private val EventDataRepository = "EVENTDATA"
  private val ModelDataRepository = "MODELDATA"
  private val MetaDataRepository = "METADATA"

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

  if (repositoryKeys.size == 0) {
    warn("There is no properly configured repository.")
  }

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
        if (sourceKeys.contains(sourceName)) {
          r -> DataObjectMeta(
            sourceName = sourceName,
            namespace = name)
        } else {
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

  private def getClient(
      clientConfig: StorageClientConfig,
      pkg: String): BaseStorageClient = {
    val className = "io.prediction.data.storage." + pkg + ".StorageClient"
    try {
      Class.forName(className).getConstructors()(0).newInstance(clientConfig).
        asInstanceOf[BaseStorageClient]
    } catch {
      case e: ClassNotFoundException =>
        val originalClassName = pkg + ".StorageClient"
        Class.forName(originalClassName).getConstructors()(0).
          newInstance(clientConfig).asInstanceOf[BaseStorageClient]
      case e: java.lang.reflect.InvocationTargetException =>
        throw e.getCause
    }
  }

  private[prediction]
  def getDataObject[T](repo: String, test: Boolean = false)
    (implicit tag: TypeTag[T]): T = {
    val repoDOMeta = repositoriesToDataObjectMeta(repo)
    val repoDOSourceName = repoDOMeta.sourceName
    getDataObject[T](repoDOSourceName, repoDOMeta.namespace, test = test)
  }

  private[prediction]
  def getPDataObject[T](repo: String)(implicit tag: TypeTag[T]): T = {
    val repoDOMeta = repositoriesToDataObjectMeta(repo)
    val repoDOSourceName = repoDOMeta.sourceName
    getPDataObject[T](repoDOSourceName, repoDOMeta.namespace)
  }

  private[prediction] def getDataObject[T](
      sourceName: String,
      namespace: String,
      parallel: Boolean = false,
      test: Boolean = false)(implicit tag: TypeTag[T]): T = {
    val clientMeta = sourcesToClientMeta(sourceName, parallel, test) getOrElse {
      throw new StorageClientException(
        s"Data source $sourceName was not properly initialized.")
    }
    val sourceType = clientMeta.sourceType
    val ctorArgs = dataObjectCtorArgs(clientMeta.client, namespace)
    val classPrefix = clientMeta.client.prefix
    val originalClassName = tag.tpe.toString.split('.')
    val rawClassName = sourceType + "." + classPrefix + originalClassName.last
    val className = "io.prediction.data.storage." + rawClassName
    val clazz = try {
      Class.forName(className)
    } catch {
      case e: ClassNotFoundException =>
        try {
          Class.forName(rawClassName)
        } catch {
          case e: ClassNotFoundException =>
            throw new StorageClientException("No storage backend " +
              "implementation can be found (tried both " +
              s"$className and $rawClassName)")
        }
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
          s" Exception message: ${e.getMessage}).", e)
        errors += 1
        throw e
      case e: java.lang.reflect.InvocationTargetException =>
        throw e.getCause
    }
  }

  private def getPDataObject[T](
      sourceName: String,
      databaseName: String)(implicit tag: TypeTag[T]): T =
    getDataObject[T](sourceName, databaseName, true)

  private def dataObjectCtorArgs(
      client: BaseStorageClient,
      namespace: String): Seq[AnyRef] = {
    Seq(client.client, client.config, namespace)
  }

  private[prediction] def verifyAllDataObjects(): Unit = {
    println("  Verifying Meta Data Backend")
    getMetaDataEngineManifests()
    getMetaDataEngineInstances()
    getMetaDataEvaluationInstances()
    getMetaDataApps()
    getMetaDataAccessKeys()
    println("  Verifying Model Data Backend")
    getModelDataModels()
    println("  Verifying Event Data Backend")
    val eventsDb = getLEvents(test = true)
    println("  Test write Event Store (App Id 0)")
    // use appId=0 for testing purpose
    eventsDb.init(0)
    eventsDb.insert(Event(
      event="test",
      entityType="test",
      entityId="test"), 0)
    eventsDb.remove(0)
    eventsDb.close()
  }

  private[prediction] def getMetaDataEngineManifests(): EngineManifests =
    getDataObject[EngineManifests](MetaDataRepository)

  private[prediction] def getMetaDataEngineInstances(): EngineInstances =
    getDataObject[EngineInstances](MetaDataRepository)

  private[prediction] def getMetaDataEvaluationInstances(): EvaluationInstances =
    getDataObject[EvaluationInstances](MetaDataRepository)

  private[prediction] def getMetaDataApps(): Apps =
    getDataObject[Apps](MetaDataRepository)

  private[prediction] def getMetaDataAccessKeys(): AccessKeys =
    getDataObject[AccessKeys](MetaDataRepository)

  private[prediction] def getMetaDataChannels(): Channels =
    getDataObject[Channels](MetaDataRepository)

  private[prediction] def getModelDataModels(): Models =
    getDataObject[Models](ModelDataRepository)

  /** Obtains a data access object that returns [[Event]] related local data
    * structure.
    */
  def getLEvents(test: Boolean = false): LEvents =
    getDataObject[LEvents](EventDataRepository, test = test)

  /** Obtains a data access object that returns [[Event]] related RDD data
    * structure.
    */
  def getPEvents(): PEvents =
    getPDataObject[PEvents](EventDataRepository)

  if (errors > 0) {
    error(s"There were $errors configuration errors. Exiting.")
    sys.exit(errors)
  }
}

private[prediction] trait BaseStorageClient {
  val config: StorageClientConfig
  val client: AnyRef
  val prefix: String = ""
}

private[prediction] case class StorageClientConfig(
  parallel: Boolean = false, // parallelized access (RDD)?
  test: Boolean = false, // test mode config
  properties: Map[String, String] = Map())

private[prediction] class StorageClientException(msg: String)
    extends RuntimeException(msg)

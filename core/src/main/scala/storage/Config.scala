package io.prediction.storage

import com.mongodb.casbah.Imports._
//import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config._
import grizzled.slf4j.Logging
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.ConnectTransportException

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
  private val MongoDBSource = "mongodb"
  private val ElasticSearchSource = "elasticsearch"
  private val sourceClassPrefix = Map(
    MongoDBSource -> "Mongo",
    ElasticSearchSource -> "ES")

  private val storageSources: Map[String, Tagged] = storageSourcesKeys.map(k =>
    try {
      val keyedPath = storageSourcesPrefixPath(k)
      val sourceTypePath = prefixPath(keyedPath, "type")
      val sourceType = config.getString(sourceTypePath)

      sourceType match {
        case MongoDBSource =>
          val host = config.getString(prefixPath(keyedPath, "host"))
          val port = config.getInt(prefixPath(keyedPath, "port"))
          k -> Tagged(MongoDBSource, Seq(MongoClient(host, port)))
        case ElasticSearchSource =>
          val host = config.getString(prefixPath(keyedPath, "host"))
          val port = config.getInt(prefixPath(keyedPath, "port"))
          val client = new TransportClient().addTransportAddress(
            new InetSocketTransportAddress(host, port))
          k -> Tagged(ElasticSearchSource, Seq(client))
        case _ => throw new ConfigException.BadValue(
          config.getValue(sourceTypePath).origin,
          sourceTypePath,
          s"Unspported data storage type: ${sourceType}")
      }
    } catch {
      case e: ConfigException =>
        error(e.getMessage)
        errors += 1
        k -> Tagged("", Seq())
      case e: ConnectTransportException =>
        error(e.getMessage)
        errors += 1
        k -> Tagged("", Seq())
    }
  ).toMap

  /** Reference to the app data repository. */
  val AppDataRepository = "appdata"
  /** Reference to the settings repository. */
  val SettingsRepository = "settings"

  private val repositoriesPrefix = "io.prediction.storage.repositories"
  private def repositoriesPrefixPath(body: String) =
    prefixPath(repositoriesPrefix, body)
  private val repositories = Seq(AppDataRepository, SettingsRepository)
  private val repositoriesDatabase: Map[String, Tagged] = repositories.map(r =>
    try {
      val keyedPath = repositoriesPrefixPath(r)
      val name = config.getString(prefixPath(keyedPath, "name"))
      val sourceName = config.getString(prefixPath(keyedPath, "source"))
      val source = storageSources.get(sourceName)
      source map { s =>
        s.tag match {
          case MongoDBSource =>
            r -> Tagged(sourceName,
              Seq(s.stuff(0).asInstanceOf[MongoClient](name)))
          case ElasticSearchSource =>
            r -> Tagged(sourceName,
              Seq(s.stuff(0).asInstanceOf[Client], name))
        }
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

  private def getDataObject[T](repo: String)(implicit tag: TypeTag[T]): T = {
    val repoSource = repositoriesDatabase(repo)
    val repoSourceType = repoSource.tag
    val originalClassName = tag.tpe.toString.split('.')
    val className = (originalClassName.dropRight(1) ++ Seq(
      sourceClassPrefix(repoSourceType) + originalClassName.last)).mkString(".")
    Class.forName(className).getConstructors()(0).
      newInstance(repoSource.stuff: _*).asInstanceOf[T]
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

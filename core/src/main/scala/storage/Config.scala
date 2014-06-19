package io.prediction.storage

import scala.collection.JavaConversions._

import com.mongodb.casbah.Imports._
import com.typesafe.config._

/**
 * Configuration accessors.
 *
 * This class ensures its users that the config is free of error, and provides default values as necessary.
 */
class Config {
  private val config = ConfigFactory.load()

  /** The base directory of PredictionIO deployment/repository. */
  val base: String = config.getString("io.prediction.base")

  /** The database type that stores PredictionIO settings. */
  val settingsDbType: String = config.getString("io.prediction.commons.settings.db.type")

  /** The database host that stores PredictionIO settings. */
  val settingsDbHost: String = settingsDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.settings.db.host") } catch { case _: Throwable => "127.0.0.1" }
  }

  /** The database port that stores PredictionIO settings. */
  val settingsDbPort: Int = settingsDbType match {
    case dbTypeMongoDb => try { config.getInt("io.prediction.commons.settings.db.port") } catch { case _: Throwable => 27017 }
  }

  /** The database name that stores PredictionIO settings. */
  val settingsDbName: String = settingsDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.settings.db.name") } catch { case _: Throwable => "predictionio" }
  }

  /** The database user that stores PredictionIO settings. */
  val settingsDbUser: Option[String] = try { Some(config.getString("io.prediction.commons.settings.db.user")) } catch { case _: Throwable => None }

  /** The database password that stores PredictionIO settings. */
  val settingsDbPassword: Option[String] = try { Some(config.getString("io.prediction.commons.settings.db.password")) } catch { case _: Throwable => None }

  /** If settingsDbType is "mongodb", this will contain a Some[MongoDB] object. */
  val settingsMongoDb: Option[MongoDB] = if (settingsDbType == "mongodb") {
    val db = MongoClient(settingsDbHost, settingsDbPort)(settingsDbName)
    settingsDbUser map { db.authenticate(_, settingsDbPassword.getOrElse("")) }
    Some(db)
  } else None

  /** The database type that stores PredictionIO appdata. */
  val appdataDbType: String = config.getString("io.prediction.commons.appdata.db.type")

  /** The database host that stores PredictionIO appdata. */
  val appdataDbHost: String = appdataDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.appdata.db.host") } catch { case _: Throwable => "127.0.0.1" }
  }

  /** The database port that stores PredictionIO appdata. */
  val appdataDbPort: Int = appdataDbType match {
    case dbTypeMongoDb => try { config.getInt("io.prediction.commons.appdata.db.port") } catch { case _: Throwable => 27017 }
  }

  /** The database name that stores PredictionIO appdata. */
  val appdataDbName: String = appdataDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.appdata.db.name") } catch { case _: Throwable => "predictionio_appdata" }
  }

  /** The database user that stores PredictionIO appdata. */
  val appdataDbUser: Option[String] = try { Some(config.getString("io.prediction.commons.appdata.db.user")) } catch { case _: Throwable => None }

  /** The database password that stores PredictionIO appdata. */
  val appdataDbPassword: Option[String] = try { Some(config.getString("io.prediction.commons.appdata.db.password")) } catch { case _: Throwable => None }

  /** If appdataDbType is "mongodb", this will contain a Some[MongoDB] object. */
  val appdataMongoDb: Option[MongoDB] = if (appdataDbType == "mongodb") {
    val db = MongoClient(appdataDbHost, appdataDbPort)(appdataDbName)
    appdataDbUser map { db.authenticate(_, appdataDbPassword.getOrElse("")) }
    Some(db)
  } else None

  def getSettingsEngineManifests(): EngineManifests = {
    settingsDbType match {
      case "mongodb" => {
        //new MongoItemTrends(appdataMongoDb.get)
        MongoEngineManifests(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  def getSettingsRuns(): Runs = {
    settingsDbType match {
      case "mongodb" => {
        //new MongoItemTrends(appdataMongoDb.get)
        MongoRuns(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an ItemTrends object with configured backend type. */
  def getAppdataItemTrends(): ItemTrends = {
    appdataDbType match {
      case "mongodb" => {
        //new MongoItemTrends(appdataMongoDb.get)
        MongoItemTrends(appdataMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataDbType)
    }
  }

  /** Obtains a Users object with configured backend type. */
  def getAppdataUsers(): Users = {
    appdataDbType match {
      case "mongodb" => {
        new MongoUsers(appdataMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataDbType)
    }
  }

  /** Obtains an Items object with configured backend type. */
  def getAppdataItems(): Items = {
    appdataDbType match {
      case "mongodb" => {
        new MongoItems(appdataMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataDbType)
    }
  }

  /** Obtains a U2IActions object with configured backend type. */
  def getAppdataU2IActions(): U2IActions = {
    appdataDbType match {
      case "mongodb" => {
        new MongoU2IActions(appdataMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataDbType)
    }
  }

  def getAppdataItemSets(): ItemSets = {
    appdataDbType match {
      case "mongodb" => {
        new MongoItemSets(appdataMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataDbType)
    }
  }

  /** The database type that stores PredictionIO workflowdata. */
  val workflowdataDbType: String = config.getString("io.prediction.commons.workflowdata.db.type")

  /** The database host that stores PredictionIO workflowdata. */
  val workflowdataDbHost: String = workflowdataDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.workflowdata.db.host") } catch { case _: Throwable => "127.0.0.1" }
  }

  /** The database port that stores PredictionIO workflowdata. */
  val workflowdataDbPort: Int = workflowdataDbType match {
    case dbTypeMongoDb => try { config.getInt("io.prediction.commons.workflowdata.db.port") } catch { case _: Throwable => 27017 }
  }

  /** The database name that stores PredictionIO workflowdata. */
  val workflowdataDbName: String = workflowdataDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.workflowdata.db.name") } catch { case _: Throwable => "predictionio_workflowdata" }
  }

  /** The database user that stores PredictionIO workflowdata. */
  val workflowdataDbUser: Option[String] = try { Some(config.getString("io.prediction.commons.workflowdata.db.user")) } catch { case _: Throwable => None }

  /** The database password that stores PredictionIO workflowdata. */
  val workflowdataDbPassword: Option[String] = try { Some(config.getString("io.prediction.commons.workflowdata.db.password")) } catch { case _: Throwable => None }

  /** If workflowdataDbType is "mongodb", this will contain a Some[MongoDB] object. */
  val workflowdataMongoDb: Option[MongoDB] = if (workflowdataDbType == "mongodb") {
    val db = MongoClient(workflowdataDbHost, workflowdataDbPort)(workflowdataDbName)
    workflowdataDbUser map { db.authenticate(_, workflowdataDbPassword.getOrElse("")) }
    Some(db)
  } else None


}

package io.prediction.commons.settings

import com.mongodb.casbah.Imports._
import com.typesafe.config._

/** Configuration accessors.
  *
  * This class ensures its users that the config is free of error, and provides default values as necessary.
  */
class Config {
  private var config = ConfigFactory.load()

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

  /** The HDFS root location for PredictionIO data. */
  val settingsHdfsRoot: String = try { config.getString("io.prediction.commons.settings.hdfs.root") } catch { case _: Throwable => "predictionio/" }

  /** PredictionIO Scheduler base URL. */
  val settingsSchedulerUrl: String = try { config.getString("io.prediction.commons.settings.scheduler.url") } catch { case _: Throwable => "http://localhost:7000" }

  /** The database user that stores PredictionIO settings. */
  val settingsDbUser: Option[String] = try { Some(config.getString("io.prediction.commons.settings.db.user")) } catch { case _: Throwable => None }

  /** The database password that stores PredictionIO settings. */
  val settingsDbPassword: Option[String] = try { Some(config.getString("io.prediction.commons.settings.db.password")) } catch { case _: Throwable => None }

  /** If settingsDbType is "mongodb", this will contain a Some[MongoDB] object. */
  val mongoDb: Option[MongoDB] = settingsDbType match {
    case "mongodb" => {
      val db = MongoConnection(settingsDbHost, settingsDbPort)(settingsDbName)
      settingsDbUser map { db.authenticate(_, settingsDbPassword.getOrElse("")) }
      Some(db)
    }
    case _ => None
  }

  /** Reloads configuration. */
  def reload(): Unit = {
    config = ConfigFactory.load()
  }

  /** Obtains a Users object with configured backend type. */
  def getUsers(): Users = {
    settingsDbType match {
      case "mongodb" => {
        new mongodb.MongoUsers(mongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains a Apps object with configured backend type. */
  def getApps(): Apps = {
    settingsDbType match {
      case "mongodb" => {
        new mongodb.MongoApps(mongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

    /** Obtains a Engines object with configured backend type. */
  def getEngines(): Engines = {
    settingsDbType match {
      case "mongodb" => {
        new mongodb.MongoEngines(mongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains a Apps object with configured backend type. */
  def getAlgos(): Algos = {
    settingsDbType match {
      case "mongodb" => {
        new mongodb.MongoAlgos(mongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains the JAR filename for a specific algorithm package name. */
  def getJar(pkgname: String): Option[String] = {
    try { Some(config.getString(pkgname + ".jar")) } catch { case _: Throwable => None }
  }

  /** Obtains an OfflineEvals object with configured backend type. */
  def getOfflineEvals(): OfflineEvals = {
    settingsDbType match {
      case "mongodb" => {
        new mongodb.MongoOfflineEvals(mongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an OfflineEvalMetrics object with configured backend type. */
  def getOfflineEvalMetrics(): OfflineEvalMetrics = {
    settingsDbType match {
      case "mongodb" => {
        new mongodb.MongoOfflineEvalMetrics(mongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an OfflineEvalResults object with configured backend type. */
  def getOfflineEvalResults(): OfflineEvalResults = {
    settingsDbType match {
      case "mongodb" => {
        new mongodb.MongoOfflineEvalResults(mongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }
}

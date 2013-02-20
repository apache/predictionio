package io.prediction.commons.appdata

import com.mongodb.casbah.Imports._
import com.typesafe.config._

/** Configuration accessors.
  *
  * This class ensures its users that the config is free of error, and provides default values as necessary.
  */
class Config {
  private var config = ConfigFactory.load()

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

  /** The database type that stores PredictionIO training appdata. */
  val appdataTrainingDbType: String = config.getString("io.prediction.commons.appdata.training.db.type")

  /** The database host that stores PredictionIO training appdata. */
  val appdataTrainingDbHost: String = appdataTrainingDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.appdata.training.db.host") } catch { case _: Throwable => "127.0.0.1" }
  }

  /** The database port that stores PredictionIO training appdata. */
  val appdataTrainingDbPort: Int = appdataTrainingDbType match {
    case dbTypeMongoDb => try { config.getInt("io.prediction.commons.appdata.training.db.port") } catch { case _: Throwable => 27017 }
  }

  /** The database name that stores PredictionIO training appdata. */
  val appdataTrainingDbName: String = appdataTrainingDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.appdata.training.db.name") } catch { case _: Throwable => "predictionio_training_appdata" }
  }

  /** The database type that stores PredictionIO test appdata. */
  val appdataTestDbType: String = config.getString("io.prediction.commons.appdata.test.db.type")

  /** The database host that stores PredictionIO test appdata. */
  val appdataTestDbHost: String = appdataTestDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.appdata.test.db.host") } catch { case _: Throwable => "127.0.0.1" }
  }

  /** The database port that stores PredictionIO test appdata. */
  val appdataTestDbPort: Int = appdataTestDbType match {
    case dbTypeMongoDb => try { config.getInt("io.prediction.commons.appdata.test.db.port") } catch { case _: Throwable => 27017 }
  }

  /** The database name that stores PredictionIO test appdata. */
  val appdataTestDbName: String = appdataTestDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.appdata.test.db.name") } catch { case _: Throwable => "predictionio_test_appdata" }
  }

  /** If appdataDbType is "mongodb", this will contain a Some[MongoDB] object. */
  val mongoDb: Option[MongoDB] = appdataDbType match {
    case "mongodb" => {
      val db = MongoConnection(appdataDbHost, appdataDbPort)(appdataDbName)
      appdataDbUser map { db.authenticate(_, appdataDbPassword.getOrElse("")) }
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
    appdataDbType match {
      case "mongodb" => {
        new mongodb.MongoUsers(mongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataDbType)
    }
  }

  /** Obtains a Items object with configured backend type. */
  def getItems(): Items = {
    appdataDbType match {
      case "mongodb" => {
        new mongodb.MongoItems(mongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataDbType)
    }
  }

  /** Obtains a U2IActions object with configured backend type. */
  def getU2IActions(): U2IActions = {
    appdataDbType match {
      case "mongodb" => {
        new mongodb.MongoU2IActions(mongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataDbType)
    }
  }

}
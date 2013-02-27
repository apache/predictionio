package io.prediction.commons.modeldata

import com.mongodb.casbah.Imports._
import com.typesafe.config._

/** Configuration accessors.
  *
  * This class ensures its users that the config is free of error, and provides default values as necessary.
  */
class TrainingSetConfig {
  protected var config = ConfigFactory.load()

  /** The database type that stores PredictionIO modeldata. */
  val modeldataDbType: String = config.getString("io.prediction.commons.modeldata.training.db.type")

  /** The database host that stores PredictionIO modeldata. */
  val modeldataDbHost: String = modeldataDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.modeldata.training.db.host") } catch { case _: Throwable => "127.0.0.1" }
  }

  /** The database port that stores PredictionIO modeldata. */
  val modeldataDbPort: Int = modeldataDbType match {
    case dbTypeMongoDb => try { config.getInt("io.prediction.commons.modeldata.training.db.port") } catch { case _: Throwable => 27017 }
  }

  /** The database name that stores PredictionIO modeldata. */
  val modeldataDbName: String = modeldataDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.modeldata.training.db.name") } catch { case _: Throwable => "predictionio_training_modeldata" }
  }

  /** The database user that stores PredictionIO modeldata. */
  val modeldataDbUser: Option[String] = try { Some(config.getString("io.prediction.commons.modeldata.training.db.user")) } catch { case _: Throwable => None }

  /** The database password that stores PredictionIO modeldata. */
  val modeldataDbPassword: Option[String] = try { Some(config.getString("io.prediction.commons.modeldata.training.db.password")) } catch { case _: Throwable => None }

  /** If modeldataDbType is "mongodb", this will contain a Some[MongoDB] object. */
  val mongoDb: Option[MongoDB] = modeldataDbType match {
    case "mongodb" => {
      val db = MongoConnection(modeldataDbHost, modeldataDbPort)(modeldataDbName)
      modeldataDbUser map { db.authenticate(_, modeldataDbPassword.getOrElse("")) }
      Some(db)
    }
    case _ => None
  }

  /** Reloads configuration. */
  def reload(): Unit = {
    config = ConfigFactory.load()
  }

  /** Obtains an ItemRecScores object with configured backend type. */
  def getItemRecScores(): ItemRecScores = {
    modeldataDbType match {
      case "mongodb" => {
        new mongodb.MongoItemRecScores(mongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid modeldata database type: " + modeldataDbType)
    }
  }
}

package io.prediction.commons

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

  /** The installation location of Hadoop (equivalent to HADOOP_HOME). */
  val settingsHadoopHome: Option[String] = try { Some(config.getString("io.prediction.commons.settings.hadoop.home")) } catch { case _: Throwable => None }

  /** The HDFS root location for PredictionIO data. */
  val settingsHdfsRoot: String = try { config.getString("io.prediction.commons.settings.hdfs.root") } catch { case _: Throwable => "predictionio/" }

  /** The local temporary root location for PredictionIO data. */
  val settingsLocalTempRoot: String = try { config.getString("io.prediction.commons.settings.local.temp.root") } catch { case _: Throwable => "/tmp/" }

  /** PredictionIO Scheduler base URL. */
  val settingsSchedulerUrl: String = try { config.getString("io.prediction.commons.settings.scheduler.url") } catch { case _: Throwable => "http://localhost:7000" }

  /** Whether the scheduler should check for new releases regularly. */
  val settingsSchedulerUpdatecheck: Boolean = try { config.getBoolean("io.prediction.commons.settings.scheduler.updatecheck") } catch { case _: Throwable => true }

  /** The database user that stores PredictionIO settings. */
  val settingsDbUser: Option[String] = try { Some(config.getString("io.prediction.commons.settings.db.user")) } catch { case _: Throwable => None }

  /** The database password that stores PredictionIO settings. */
  val settingsDbPassword: Option[String] = try { Some(config.getString("io.prediction.commons.settings.db.password")) } catch { case _: Throwable => None }

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

  /** The database user that stores PredictionIO training appdata. */
  val appdataTrainingDbUser: Option[String] = try { Some(config.getString("io.prediction.commons.appdata.training.db.user")) } catch { case _: Throwable => None }

  /** The database password that stores PredictionIO training appdata. */
  val appdataTrainingDbPassword: Option[String] = try { Some(config.getString("io.prediction.commons.appdata.training.db.password")) } catch { case _: Throwable => None }

  /** The database type that stores PredictionIO validation appdata. */
  val appdataValidationDbType: String = config.getString("io.prediction.commons.appdata.validation.db.type")

  /** The database host that stores PredictionIO validation appdata. */
  val appdataValidationDbHost: String = appdataValidationDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.appdata.validation.db.host") } catch { case _: Throwable => "127.0.0.1" }
  }

  /** The database port that stores PredictionIO validation appdata. */
  val appdataValidationDbPort: Int = appdataValidationDbType match {
    case dbTypeMongoDb => try { config.getInt("io.prediction.commons.appdata.validation.db.port") } catch { case _: Throwable => 27017 }
  }

  /** The database name that stores PredictionIO validation appdata. */
  val appdataValidationDbName: String = appdataValidationDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.appdata.validation.db.name") } catch { case _: Throwable => "predictionio_validation_appdata" }
  }

  /** The database user that stores PredictionIO validation appdata. */
  val appdataValidationDbUser: Option[String] = try { Some(config.getString("io.prediction.commons.appdata.validation.db.user")) } catch { case _: Throwable => None }

  /** The database password that stores PredictionIO validation appdata. */
  val appdataValidationDbPassword: Option[String] = try { Some(config.getString("io.prediction.commons.appdata.validation.db.password")) } catch { case _: Throwable => None }

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

  /** The database user that stores PredictionIO test appdata. */
  val appdataTestDbUser: Option[String] = try { Some(config.getString("io.prediction.commons.appdata.test.db.user")) } catch { case _: Throwable => None }

  /** The database password that stores PredictionIO test appdata. */
  val appdataTestDbPassword: Option[String] = try { Some(config.getString("io.prediction.commons.appdata.test.db.password")) } catch { case _: Throwable => None }

  /** The database type that stores PredictionIO modeldata. */
  val modeldataDbType: String = config.getString("io.prediction.commons.modeldata.db.type")

  /** The database host that stores PredictionIO modeldata. */
  val modeldataDbHost: String = modeldataDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.modeldata.db.host") } catch { case _: Throwable => "127.0.0.1" }
  }

  /** The database port that stores PredictionIO modeldata. */
  val modeldataDbPort: Int = modeldataDbType match {
    case dbTypeMongoDb => try { config.getInt("io.prediction.commons.modeldata.db.port") } catch { case _: Throwable => 27017 }
  }

  /** The database name that stores PredictionIO modeldata. */
  val modeldataDbName: String = modeldataDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.modeldata.db.name") } catch { case _: Throwable => "predictionio_modeldata" }
  }

  /** The database user that stores PredictionIO modeldata. */
  val modeldataDbUser: Option[String] = try { Some(config.getString("io.prediction.commons.modeldata.db.user")) } catch { case _: Throwable => None }

  /** The database password that stores PredictionIO modeldata. */
  val modeldataDbPassword: Option[String] = try { Some(config.getString("io.prediction.commons.modeldata.db.password")) } catch { case _: Throwable => None }

  /** MongoDB-specific. Enable sharding on modeldata collections. */
  val modeldataDbSharding: Boolean = try { config.getBoolean("io.prediction.commons.modeldata.db.sharding") } catch { case _: Throwable => false }

  /** MongoDB-specific. Shard keys for sharding modeldata collections. */
  val modeldataDbShardKeys: Option[Seq[String]] = try { Some(config.getStringList("io.prediction.commons.modeldata.db.shardkeys").toSeq) } catch { case _: Throwable => None }

  /** The database type that stores PredictionIO modeldata. */
  val modeldataTrainingDbType: String = config.getString("io.prediction.commons.modeldata.training.db.type")

  /** The database host that stores PredictionIO modeldata. */
  val modeldataTrainingDbHost: String = modeldataTrainingDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.modeldata.training.db.host") } catch { case _: Throwable => "127.0.0.1" }
  }

  /** The database port that stores PredictionIO modeldata. */
  val modeldataTrainingDbPort: Int = modeldataTrainingDbType match {
    case dbTypeMongoDb => try { config.getInt("io.prediction.commons.modeldata.training.db.port") } catch { case _: Throwable => 27017 }
  }

  /** The database name that stores PredictionIO modeldata. */
  val modeldataTrainingDbName: String = modeldataTrainingDbType match {
    case dbTypeMongoDb => try { config.getString("io.prediction.commons.modeldata.training.db.name") } catch { case _: Throwable => "predictionio_training_modeldata" }
  }

  /** The database user that stores PredictionIO modeldata. */
  val modeldataTrainingDbUser: Option[String] = try { Some(config.getString("io.prediction.commons.modeldata.training.db.user")) } catch { case _: Throwable => None }

  /** The database password that stores PredictionIO modeldata. */
  val modeldataTrainingDbPassword: Option[String] = try { Some(config.getString("io.prediction.commons.modeldata.training.db.password")) } catch { case _: Throwable => None }

  /** If settingsDbType is "mongodb", this will contain a Some[MongoDB] object. */
  val settingsMongoDb: Option[MongoDB] = if (settingsDbType == "mongodb") {
    val db = MongoClient(settingsDbHost, settingsDbPort)(settingsDbName)
    settingsDbUser map { db.authenticate(_, settingsDbPassword.getOrElse("")) }
    Some(db)
  } else None

  /** If appdataDbType is "mongodb", this will contain a Some[MongoDB] object. */
  val appdataMongoDb: Option[MongoDB] = if (appdataDbType == "mongodb") {
    val db = MongoClient(appdataDbHost, appdataDbPort)(appdataDbName)
    appdataDbUser map { db.authenticate(_, appdataDbPassword.getOrElse("")) }
    Some(db)
  } else None

  /** If appdataTrainingDbType is "mongodb", this will contain a Some[MongoDB] object. */
  val appdataTrainingMongoDb: Option[MongoDB] = if (appdataTrainingDbType == "mongodb") {
    val db = MongoClient(appdataTrainingDbHost, appdataTrainingDbPort)(appdataTrainingDbName)
    appdataTrainingDbUser map { db.authenticate(_, appdataTrainingDbPassword.getOrElse("")) }
    Some(db)
  } else None

  /** If appdataTestDbType is "mongodb", this will contain a Some[MongoDB] object. */
  val appdataTestMongoDb: Option[MongoDB] = if (appdataTestDbType == "mongodb") {
    val db = MongoClient(appdataTestDbHost, appdataTestDbPort)(appdataTestDbName)
    appdataTestDbUser map { db.authenticate(_, appdataTestDbPassword.getOrElse("")) }
    Some(db)
  } else None

  /** If appdataValidationDbType is "mongodb", this will contain a Some[MongoDB] object. */
  val appdataValidationMongoDb: Option[MongoDB] = if (appdataValidationDbType == "mongodb") {
    val db = MongoClient(appdataValidationDbHost, appdataValidationDbPort)(appdataValidationDbName)
    appdataValidationDbUser map { db.authenticate(_, appdataValidationDbPassword.getOrElse("")) }
    Some(db)
  } else None

  /** If modeldataDbType is "mongodb", this will contain a Some[MongoDB] object. */
  val modeldataMongoDb: Option[MongoDB] = if (modeldataDbType == "mongodb") {
    val db = MongoClient(modeldataDbHost, modeldataDbPort)(modeldataDbName)
    modeldataDbUser map { db.authenticate(_, modeldataDbPassword.getOrElse("")) }
    Some(db)
  } else None

  /** If modeldataTrainingDbType is "mongodb", this will contain a Some[MongoDB] object. */
  val modeldataTrainingMongoDb: Option[MongoDB] = if (modeldataTrainingDbType == "mongodb") {
    val db = MongoClient(modeldataTrainingDbHost, modeldataTrainingDbPort)(modeldataTrainingDbName)
    modeldataTrainingDbUser map { db.authenticate(_, modeldataTrainingDbPassword.getOrElse("")) }
    Some(db)
  } else None

  private val jarsR = """^io\.prediction\.jars\.(.*)""".r

  /** Returns all PredictionIO job JARs found in the configuration object. */
  val jars: Map[String, String] = (Map[String, String]() /: (config.entrySet filter { e =>
    jarsR findPrefixOf e.getKey map { _ => true } getOrElse { false }
  })) { (x, y) =>
    val jarsR(key) = y.getKey
    x + (key -> config.getString(y.getKey))
  }

  /** Check whether settings database can be connected. */
  def settingsDbConnectable() = {
    settingsDbType match {
      case "mongodb" => {
        try {
          settingsMongoDb.get.getCollectionNames()
          true
        } catch {
          case _: Throwable => false
        }
      }
      case _ => false
    }
  }

  /** Check whether app data database can be connected. */
  def appdataDbConnectable() = {
    appdataDbType match {
      case "mongodb" => {
        try {
          appdataMongoDb.get.getCollectionNames()
          true
        } catch {
          case _: Throwable => false
        }
      }
      case _ => false
    }
  }

  /** Check whether app data training database can be connected. */
  def appdataTrainingDbConnectable() = {
    appdataTrainingDbType match {
      case "mongodb" => {
        try {
          appdataTrainingMongoDb.get.getCollectionNames()
          true
        } catch {
          case _: Throwable => false
        }
      }
      case _ => false
    }
  }

  /** Check whether app data test database can be connected. */
  def appdataTestDbConnectable() = {
    appdataTestDbType match {
      case "mongodb" => {
        try {
          appdataTestMongoDb.get.getCollectionNames()
          true
        } catch {
          case _: Throwable => false
        }
      }
      case _ => false
    }
  }

  /** Check whether model data database can be connected. */
  def modeldataDbConnectable() = {
    modeldataDbType match {
      case "mongodb" => {
        try {
          modeldataMongoDb.get.getCollectionNames()
          true
        } catch {
          case _: Throwable => false
        }
      }
      case _ => false
    }
  }

  /** Check whether model data training database can be connected. */
  def modeldataTrainingDbConnectable() = {
    modeldataTrainingDbType match {
      case "mongodb" => {
        try {
          modeldataTrainingMongoDb.get.getCollectionNames()
          true
        } catch {
          case _: Throwable => false
        }
      }
      case _ => false
    }
  }

  /** Obtains a SystemInfos object with configured backend type. */
  def getSettingsSystemInfos(): settings.SystemInfos = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoSystemInfos(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains a Users object with configured backend type. */
  def getSettingsUsers(): settings.Users = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoUsers(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an Apps object with configured backend type. */
  def getSettingsApps(): settings.Apps = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoApps(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an Engines object with configured backend type. */
  def getSettingsEngines(): settings.Engines = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoEngines(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an EngineInfos object with configured backend type. */
  def getSettingsEngineInfos(): settings.EngineInfos = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoEngineInfos(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an Algos object with configured backend type. */
  def getSettingsAlgos(): settings.Algos = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoAlgos(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an AlgoInfos object with configured backend type. */
  def getSettingsAlgoInfos(): settings.AlgoInfos = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoAlgoInfos(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains a OfflineEvalSplitterInfos object with configured backend type. */
  def getSettingsOfflineEvalSplitterInfos(): settings.OfflineEvalSplitterInfos = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoOfflineEvalSplitterInfos(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains a OfflineEvalMetricInfos object with configured backend type. */
  def getSettingsOfflineEvalMetricInfos(): settings.OfflineEvalMetricInfos = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoOfflineEvalMetricInfos(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an OfflineEvals object with configured backend type. */
  def getSettingsOfflineEvals(): settings.OfflineEvals = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoOfflineEvals(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an OfflineEvalMetrics object with configured backend type. */
  def getSettingsOfflineEvalMetrics(): settings.OfflineEvalMetrics = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoOfflineEvalMetrics(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an OfflineEvalResults object with configured backend type. */
  def getSettingsOfflineEvalResults(): settings.OfflineEvalResults = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoOfflineEvalResults(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an OfflineEvalSplitters object with configured backend type. */
  def getSettingsOfflineEvalSplitters(): settings.OfflineEvalSplitters = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoOfflineEvalSplitters(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an OfflineTunes object with configured backend type. */
  def getSettingsOfflineTunes(): settings.OfflineTunes = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoOfflineTunes(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains an ParamGens object with configured backend type. */
  def getSettingsParamGens(): settings.ParamGens = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoParamGens(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains a ParamGenInfos object with configured backend type. */
  def getSettingsParamGenInfos(): settings.ParamGenInfos = {
    settingsDbType match {
      case "mongodb" => {
        new settings.mongodb.MongoParamGenInfos(settingsMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid settings database type: " + settingsDbType)
    }
  }

  /** Obtains a Users object with configured backend type. */
  def getAppdataUsers(): appdata.Users = {
    appdataDbType match {
      case "mongodb" => {
        new appdata.mongodb.MongoUsers(appdataMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataDbType)
    }
  }

  /** Obtains an Items object with configured backend type. */
  def getAppdataItems(): appdata.Items = {
    appdataDbType match {
      case "mongodb" => {
        new appdata.mongodb.MongoItems(appdataMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataDbType)
    }
  }

  /** Obtains a U2IActions object with configured backend type. */
  def getAppdataU2IActions(): appdata.U2IActions = {
    appdataDbType match {
      case "mongodb" => {
        new appdata.mongodb.MongoU2IActions(appdataMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataDbType)
    }
  }

  /** Obtains a Users object with configured backend type. */
  def getAppdataTrainingUsers(): appdata.Users = {
    appdataTrainingDbType match {
      case "mongodb" => {
        new appdata.mongodb.MongoUsers(appdataTrainingMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataTrainingDbType)
    }
  }

  /** Obtains an Items object with configured backend type. */
  def getAppdataTrainingItems(): appdata.Items = {
    appdataTrainingDbType match {
      case "mongodb" => {
        new appdata.mongodb.MongoItems(appdataTrainingMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataTrainingDbType)
    }
  }

  /** Obtains a U2IActions object with configured backend type. */
  def getAppdataTrainingU2IActions(): appdata.U2IActions = {
    appdataTrainingDbType match {
      case "mongodb" => {
        new appdata.mongodb.MongoU2IActions(appdataTrainingMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataTrainingDbType)
    }
  }

  /** Obtains a Users object with configured backend type. */
  def getAppdataTestUsers(): appdata.Users = {
    appdataTestDbType match {
      case "mongodb" => {
        new appdata.mongodb.MongoUsers(appdataTestMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataTestDbType)
    }
  }

  /** Obtains an Items object with configured backend type. */
  def getAppdataTestItems(): appdata.Items = {
    appdataTestDbType match {
      case "mongodb" => {
        new appdata.mongodb.MongoItems(appdataTestMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataTestDbType)
    }
  }

  /** Obtains a U2IActions object with configured backend type. */
  def getAppdataTestU2IActions(): appdata.U2IActions = {
    appdataTestDbType match {
      case "mongodb" => {
        new appdata.mongodb.MongoU2IActions(appdataTestMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataTestDbType)
    }
  }

  /** Obtains a Users object of the validation set with configured backend type. */
  def getAppdataValidationUsers(): appdata.Users = {
    appdataValidationDbType match {
      case "mongodb" => {
        new appdata.mongodb.MongoUsers(appdataValidationMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataValidationDbType)
    }
  }

  /** Obtains an Items object of the validation set with configured backend type. */
  def getAppdataValidationItems(): appdata.Items = {
    appdataValidationDbType match {
      case "mongodb" => {
        new appdata.mongodb.MongoItems(appdataValidationMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataValidationDbType)
    }
  }

  /** Obtains a U2IActions object of the validation set with configured backend type. */
  def getAppdataValidationU2IActions(): appdata.U2IActions = {
    appdataValidationDbType match {
      case "mongodb" => {
        new appdata.mongodb.MongoU2IActions(appdataValidationMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid appdata database type: " + appdataValidationDbType)
    }
  }

  /** Obtains a generic ModelData object with configured backend type. */
  def getModeldata(engineinfoid: String): modeldata.ModelData = {
    modeldataDbType match {
      case "mongodb" => {
        val thisObj = this
        engineinfoid match {
          case "itemrec" => getModeldataItemRecScores
          case "itemsim" => getModeldataItemSimScores
          case _ => new modeldata.mongodb.MongoModelData {
            val config = thisObj
            val mongodb = modeldataMongoDb.get
          }
        }
      }
      case _ => throw new RuntimeException("Invalid modeldata database type: " + modeldataDbType)
    }
  }

  /** Obtains an ItemRecScores object with configured backend type. */
  def getModeldataItemRecScores(): modeldata.ItemRecScores = {
    modeldataDbType match {
      case "mongodb" => {
        new modeldata.mongodb.MongoItemRecScores(this, modeldataMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid modeldata database type: " + modeldataDbType)
    }
  }

  /** Obtains an ItemSimScores object with configured backend type. */
  def getModeldataItemSimScores(): modeldata.ItemSimScores = {
    modeldataDbType match {
      case "mongodb" => {
        new modeldata.mongodb.MongoItemSimScores(this, modeldataMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid modeldata database type: " + modeldataDbType)
    }
  }

  /** Obtains a generic ModelData object for training with configured backend type. */
  def getModeldataTraining(engineinfoid: String): modeldata.ModelData = {
    modeldataTrainingDbType match {
      case "mongodb" => {
        val thisObj = this
        engineinfoid match {
          case "itemrec" => getModeldataTrainingItemRecScores
          case "itemsim" => getModeldataTrainingItemSimScores
          case _ => new modeldata.mongodb.MongoModelData {
            val config = thisObj
            val mongodb = modeldataTrainingMongoDb.get
          }
        }
      }
      case _ => throw new RuntimeException("Invalid modeldata database type: " + modeldataTrainingDbType)
    }
  }

  /** Obtains an ItemRecScores object for training with configured backend type. */
  def getModeldataTrainingItemRecScores(): modeldata.ItemRecScores = {
    modeldataTrainingDbType match {
      case "mongodb" => {
        new modeldata.mongodb.MongoItemRecScores(this, modeldataTrainingMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid modeldata database type: " + modeldataTrainingDbType)
    }
  }

  /** Obtains an ItemSimScores object for training with configured backend type. */
  def getModeldataTrainingItemSimScores(): modeldata.ItemSimScores = {
    modeldataTrainingDbType match {
      case "mongodb" => {
        new modeldata.mongodb.MongoItemSimScores(this, modeldataTrainingMongoDb.get)
      }
      case _ => throw new RuntimeException("Invalid modeldata database type: " + modeldataTrainingDbType)
    }
  }
}

package io.prediction.tools.softwaremanager

import io.prediction.commons._

case class RestoreConfig(backupDir: String = "", upgrade: Boolean = false)

object Restore {
  val config = new Config()

  val settingsMap = Map(
    "algoInfos" -> config.getSettingsAlgoInfos,
    "algos" -> config.getSettingsAlgos,
    "apps" -> config.getSettingsApps,
    "engineInfos" -> config.getSettingsEngineInfos,
    "engines" -> config.getSettingsEngines,
    "offlineEvalMetricInfos" -> config.getSettingsOfflineEvalMetricInfos,
    "offlineEvalMetrics" -> config.getSettingsOfflineEvalMetrics,
    "offlineEvalResults" -> config.getSettingsOfflineEvalResults,
    "offlineEvals" -> config.getSettingsOfflineEvals,
    "offlineEvalSplitterInfos" -> config.getSettingsOfflineEvalSplitterInfos,
    "offlineEvalSplitters" -> config.getSettingsOfflineEvalSplitters,
    "offlineTunes" -> config.getSettingsOfflineTunes,
    "paramGenInfos" -> config.getSettingsParamGenInfos,
    "paramGens" -> config.getSettingsParamGens,
    "systemInfos" -> config.getSettingsSystemInfos,
    "users" -> config.getSettingsUsers)

  def main(args: Array[String]) {
    val parser = new scopt.OptionParser[RestoreConfig]("restore") {
      head("PredictionIO Restore Utility", "0.6.7")
      help("help") text ("prints this usage text")
      opt[Unit]("upgrade") action { (_, c) =>
        c.copy(upgrade = true)
      } text ("upgrade from previous version backup data")
      arg[String]("<backup directory>") action { (x, c) =>
        c.copy(backupDir = x)
      } text ("directory containing backup files")
    }

    parser.parse(args, RestoreConfig()) map { restoreConfig =>
      println("PredictionIO Restore Utility")
      println()

      val backupDir = restoreConfig.backupDir
      val upgrade = restoreConfig.upgrade

      if (upgrade) {
        println("Upgrading from data backed up by a previous version software...")
        println()
      }

      settingsMap map { s =>
        val fn = s"${backupDir}/${s._1}.json"
        try {
          s._2.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.ISO8859).map(_.toByte).toArray, true, upgrade) map { x =>
            println(s"Restored from ${fn}")
          } getOrElse {
            println(s"Cannot restore from ${fn}. Skipping...")
          }
        } catch {
          case e: java.io.FileNotFoundException => println(s"Error: ${e.getMessage}. Skipping...")
        }
      }

      config.settingsDbType match {
        case "mongodb" => {
          val metadata = new settings.mongodb.MongoMetadata(config.settingsMongoDb.get)
          val fn = s"${backupDir}/metadata.json"
          try {
            metadata.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.ISO8859).map(_.toByte).toArray, true, upgrade) map { x =>
              println(s"Restored metadata from ${fn}")
            } getOrElse {
              println(s"Cannot restore metadata from ${fn}. Skipping...")
            }
          } catch {
            case e: java.io.FileNotFoundException => println(s"Error: ${e.getMessage}. Skipping...")
          }
        }
        case _ => println(s"Unknown settings database type ${config.settingsDbType}. Skipping metadata restore.")
      }

      println()
      println("Restore finished.")
    }
  }
}

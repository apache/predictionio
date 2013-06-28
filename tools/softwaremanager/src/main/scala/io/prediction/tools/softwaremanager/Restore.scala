package io.prediction.tools.softwaremanager

import io.prediction.commons._

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
    println("PredictionIO Restore Utility")
    println()

    val backupDir = try {
      args(0)
    } catch {
      case e: ArrayIndexOutOfBoundsException => {
      	println("Usage: restore <directory_of_backup_files>")
      	sys.exit(1)
      }
    }

  	settingsMap map { s =>
      val fn = s"${backupDir}/${s._1}.bin"
      try {
        s._2.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.ISO8859).map(_.toByte).toArray, true) map { x =>
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
        val fn = s"${backupDir}/metadata.bin"
        try {
          metadata.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.ISO8859).map(_.toByte).toArray, true) map { x =>
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

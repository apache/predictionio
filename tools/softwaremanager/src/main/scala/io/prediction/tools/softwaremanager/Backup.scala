package io.prediction.tools.softwaremanager

import io.prediction.commons._

case class BackupConfig(backupDir: String = "")

object Backup {
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
    val parser = new scopt.OptionParser[BackupConfig]("backup") {
      head("PredictionIO Backup Utility", "0.6.7")
      help("help") text ("prints this usage text")
      arg[String]("<backup directory>") action { (x, c) =>
        c.copy(backupDir = x)
      } text ("directory containing backup files")
    }

    parser.parse(args, BackupConfig()) map { backupConfig =>
      println("PredictionIO Backup Utility")
      println()

      val backupDir = backupConfig.backupDir

      val backupDirFile = new java.io.File(backupDir)
      if (!backupDirFile.exists && !backupDirFile.mkdirs) {
        println(s"Unable to create directory ${backupDir}. Aborting...")
        sys.exit(1)
      }

      settingsMap map { s =>
        val fn = s"${backupDir}/${s._1}.json"
        val fos = new java.io.FileOutputStream(fn)
        try {
          fos.write(s._2.backup())
          println(s"Backed up to ${fn}")
        } finally {
          fos.close()
        }
      }

      config.settingsDbType match {
        case "mongodb" => {
          val metadata = new settings.mongodb.MongoMetadata(config.settingsMongoDb.get)
          val fn = s"${backupDir}/metadata.json"
          val fos = new java.io.FileOutputStream(fn)
          try {
            fos.write(metadata.backup())
            println(s"Metadata backed up to ${fn}")
          } finally {
            fos.close()
          }
        }
        case _ => println(s"Unknown settings database type ${config.settingsDbType}. Skipping metadata backup.")
      }

      println()
      println("Backup completed.")
    }
  }
}

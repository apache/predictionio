package io.prediction.tools.softwaremanager

import io.prediction.commons._

import scala.collection.JavaConversions._
import scala.sys.process._

import java.io.File

import com.typesafe.config._
import org.apache.commons.io.FileUtils._

case class UpgradeConfig(current: File = new File("."), latest: File = new File("."), nomigrate: Boolean = false)

/** Upgrades previous version to current version. */
object Upgrade {
  def main(args: Array[String]) {
    val parser = new scopt.OptionParser[UpgradeConfig]("upgrade") {
      head("PredictionIO Software Upgrade Utility", "0.4.3-SNAPSHOT")
      help("help") text("prints this usage text")
      opt[Unit]("nomigrate") action { (_, c) =>
        c.copy(nomigrate = true)
      } text("upgrade from previous version backup data")
      arg[File]("<current>") action { (x, c) =>
        c.copy(current = x)
      } text("directory containing current PredictionIO setup")
      arg[File]("<latest>") action { (x, c) =>
        c.copy(latest = x)
      } text("directory containing latest PredictionIO files")
    }

    parser.parse(args, UpgradeConfig()) map { upgradeConfig =>
      var stepcount = 1
      val steps = 7
      val currentDir = upgradeConfig.current
      val latestDir = upgradeConfig.latest
      val current = currentDir.getCanonicalPath
      val latest = latestDir.getCanonicalPath
      val nomigrate = upgradeConfig.nomigrate

      println("PredictionIO Software Upgrade Utility")
      println()

      if (!currentDir.exists) {
        println(s"${current} does not exist. Aborting.")
        sys.exit(1)
      }

      if (!latestDir.exists) {
        println(s"${latest} does not exist. Aborting.")
        sys.exit(1)
      }

      val dirs = Seq("bin", "conf", "lib")

      /** Make a backup. */
      println(s"Step ${stepcount} of ${steps}: Creating backup of PredictionIO settings...")
      println()
      val backupBin = s"${current}/bin/backup"
      val backupBinFile = new File(backupBin)
      if (!backupBinFile.exists && !nomigrate) {
        println("Backup utility cannot be found. Possible causes:")
        println("- PredictionIO version <= 0.4.2")
        println("- the binary is missing")
        println()
        println("To force upgrading without migrating settings, add --nomigrate to the command. Aborting.")
        sys.exit(1)
      } else if (!nomigrate) {
        val backupCode = s"${current}/bin/backup ${current}/backup/settings".!
        if (backupCode != 0) {
          println("Backup utility returned non-zero exit code. Aborting.")
          sys.exit(1)
        }
      } else {
        println("Not migrating settings. Skipping backup.")
      }
      println()
      stepcount += 1

      println(s"Step ${stepcount} of ${steps}: Creating backup of PredictionIO installation files...")
      println()
      for (backupDir <- dirs) {
        println(s"Backing up ${backupDir}...")
        moveDirectoryToDirectory(getFile(currentDir, backupDir), getFile(currentDir, "backup"), true)
      }
      println()
      stepcount += 1

      /** Copy new files. */
      println(s"Step ${stepcount} of ${steps}: Placing new files...")
      println()
      for (dir <- dirs) {
        println(s"Placing ${dir}...")
        moveDirectoryToDirectory(getFile(latestDir, dir), currentDir, true)
      }
      println()
      stepcount += 1

      /** Merge old config to new config. */
      println(s"Step ${stepcount} or ${steps}: Merging old configuration to new configuration...")
      System.setProperty("io.prediction.base", "${io.prediction.base}")
      System.setProperty("config.file", s"${current}/backup/conf/predictionio.conf")
      val currentConfig = ConfigFactory.load()
      System.setProperty("config.file", s"${current}/conf/predictionio.conf")
      ConfigFactory.invalidateCaches()
      val latestConfig = ConfigFactory.load()

      val newConfig = currentConfig.withOnlyPath("io.prediction.commons").withFallback(latestConfig)
        .withoutPath("config")
        .withoutPath("file")
        .withoutPath("io.prediction.base")
        .withoutPath("java")
        .withoutPath("line")
        .withoutPath("os")
        .withoutPath("path")
        .withoutPath("prog")
        .withoutPath("sun")
        .withoutPath("user")

      val newConfFile = getFile(currentDir, "conf", "predictionio.conf")
      val newConfWriter = new java.io.PrintWriter(newConfFile)

      try {
        newConfig.entrySet.toSeq sortBy { entry => entry.getKey } foreach { entry =>
          val v = entry.getValue.render.replace("\"${io.prediction.base}", "${io.prediction.base}\"")
          newConfWriter.println(s"${entry.getKey}=${v}")
        }
      } finally {
        newConfWriter.close
      }
      println()
      stepcount += 1

      println(s"Step ${stepcount} of ${steps}: Retaining old logger configuration...")
      println()
      val loggerConfs = Seq("admin-logger.xml", "api-logger.xml", "scheduler-logger.xml")
      loggerConfs foreach { conf =>
        println(s"Retaining ${conf}...")
        copyFileToDirectory(getFile(currentDir, "backup", "conf", conf), getFile(currentDir, "conf"))
      }
      println()
      stepcount += 1

      println(s"Step ${stepcount} of ${steps}: Clean up temporary files...")
      deleteDirectory(latestDir)
      println()
      stepcount += 1

      println(s"Step ${stepcount} of ${steps}: Upgrading settings to new version...")
      println()
      if (nomigrate) {
        println("Not migrating settings. Skipping restore.")
      } else {
        val restoreCode = s"${current}/bin/restore --upgrade ${current}/backup/settings".!
        if (restoreCode != 0) {
          println("Restore utility returned non-zero exit code. Aborting.")
          sys.exit(1)
        }
      }
      println()

      println("Upgrade completed.")
    }
  }
}

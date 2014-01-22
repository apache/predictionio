package io.prediction.tools.softwaremanager

import scala.collection.JavaConversions._
import scala.sys.process._

import java.io.File

import com.typesafe.config._
import org.apache.commons.io.FileUtils._

case class UpgradeConfig(
  current: File = new File("."),
  latest: File = new File("."),
  nomigrate: Boolean = false,
  localVersion: String = "")

/** Upgrades previous version to current version. */
object Upgrade {
  def main(args: Array[String]) {
    val thisVersion = "0.6.7"
    val parser = new scopt.OptionParser[UpgradeConfig]("upgrade") {
      head("PredictionIO Software Upgrade Utility", thisVersion)
      help("help") text ("prints this usage text")
      opt[Unit]("nomigrate") action { (_, c) =>
        c.copy(nomigrate = true)
      } text ("upgrade from previous version backup data")
      opt[String]("localVersion") action { (x, c) =>
        c.copy(localVersion = x)
      } text ("use a local file for version information")
      arg[File]("<current>") action { (x, c) =>
        c.copy(current = x)
      } text ("directory containing current PredictionIO setup")
      arg[File]("<latest>") action { (x, c) =>
        c.copy(latest = x)
      } text ("directory containing latest PredictionIO files")
    }

    parser.parse(args, UpgradeConfig()) map { upgradeConfig =>
      var stepcount = 1
      val steps = 9
      val currentDir = upgradeConfig.current
      val latestDir = upgradeConfig.latest
      val current = currentDir.getCanonicalPath
      val latest = latestDir.getCanonicalPath
      var nomigrate = upgradeConfig.nomigrate

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

      if ((getFile(currentDir, "backup")).exists) {
        println(s"${current}/backup already exists. Please move away any previous backups and try again.")
        sys.exit(1)
      }

      val dirs = Seq("bin", "conf", "lib")

      /** Determine current version */
      System.setProperty("config.file", s"${current}/conf/predictionio.conf")
      val config = new io.prediction.commons.Config()
      val systemInfos = config.getSettingsSystemInfos
      val installed = systemInfos.get("version") map { _.value }
      installed getOrElse {
        println("Cannot detect any previous version. Possible causes:")
        println("- PredictionIO version < 0.5.0")
        println("- misconfiguration (wrong settings database pointers)")
        println("- settings database has been corrupted")
        println()
        println("No migration of settings will be performed if you choose to continue the upgrade.")
        val input = readLine("Enter 'YES' to proceed: ")
        val interrupt = input match {
          case "YES" => false
          case _ => true
        }
        if (interrupt) { sys.exit(1) } else { nomigrate = true }
      }
      val installedVersion = installed.get

      /** Backup existing data. */
      println(s"Step ${stepcount} of ${steps}: Backup existing settings...")
      println()

      if (nomigrate) {
        println("Not migrating settings. Skipping backup.")
      } else {
        backup(s"${current}/bin/backup", s"${current}/backup/settings/${installedVersion}", current)
      }

      println()
      stepcount += 1

      /** Determine updaters to download. */
      println(s"Step ${stepcount} of ${steps}: Downloading any additional settings updaters...")
      println()

      val versions = if (upgradeConfig.localVersion == "") { Versions() } else { Versions(upgradeConfig.localVersion) }
      val updateSequence = versions.updateSequence(installedVersion, thisVersion)

      if (nomigrate) {
        println("Not migrating settings. Skipping...")
      } else {
        if (updateSequence.size == 0) {
          println(s"Upgrading from ${installedVersion} to ${thisVersion} requires no additional updaters.")
        } else {
          val updaterDirFile = new java.io.File(s"${current}/updaters")
          if (!updaterDirFile.exists && !updaterDirFile.mkdirs) {
            println(s"Unable to create directory ${updaterDirFile}. Aborting...")
            sys.exit(1)
          }
          var i = 0
          updateSequence foreach { v =>
            versions.updater(v) map { u =>
              println(s"Downloading ${u}...")
              if (Process(s"curl -O ${u}", updaterDirFile).! != 0) {
                println("Download failed. Aborting...")
                sys.exit(1)
              }
              val filename = u.split('/').reverse.head
              val dirname = filename.split('.').dropRight(1).mkString(".")
              val extractedUpdaterDir = getFile(updaterDirFile, dirname)
              if (extractedUpdaterDir.exists) { deleteDirectory(extractedUpdaterDir) }
              if (Process(s"unzip ${filename}", updaterDirFile).! != 0) {
                println("Updater extraction failed. Aborting...")
                sys.exit(1)
              }
              if (i == 0)
                restore(s"${updaterDirFile}/${dirname}/bin/restore", s"${current}/backup/settings/${installedVersion}", current)
              else
                restore(s"${updaterDirFile}/${dirname}/bin/restore", s"${current}/backup/settings/${updateSequence(i - 1)}", current)

              backup(s"${updaterDirFile}/${dirname}/bin/backup", s"${current}/backup/settings/${v}", current)
            }
            i += 1
          }
        }
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
        if (updateSequence.size > 0)
          restore(s"${current}/bin/restore", s"${current}/backup/settings/${updateSequence.last}", current)
        else
          restore(s"${current}/bin/restore", s"${current}/backup/settings/${installedVersion}", current)
      }
      println()
      stepcount += 1

      println(s"Step ${stepcount} of ${steps}: Running setup for new version...")
      println()
      s"${current}/bin/setup.sh".!
      println()

      println("Upgrade completed.")
    }
  }

  private def backup(backupBin: String, backupDir: String, base: String) = {
    val backupBinFile = new File(backupBin)
    if (!backupBinFile.exists) {
      println("Backup utility cannot be found. Possible causes:")
      println("- PredictionIO version < 0.5.0")
      println("- the binary is missing")
      println()
      println("To force upgrading without migrating settings, add --nomigrate to the command. Aborting.")
      sys.exit(1)
    } else {
      if (Process(
        s"${backupBin} ${backupDir}",
        None,
        ("JVM_OPT", s"-Dconfig.file=${base}/conf/predictionio.conf -Dio.prediction.base=${base}")).! != 0) {
        println("Backup utility returned non-zero exit code. Aborting.")
        sys.exit(1)
      }
    }
  }

  private def restore(restoreBin: String, restoreDir: String, base: String) = {
    val restoreBinFile = new File(restoreBin)
    if (!restoreBinFile.exists) {
      println("Restore utility cannot be found. Possible causes:")
      println("- PredictionIO version < 0.5.0")
      println("- the binary is missing")
      println()
      println("To force upgrading without migrating settings, add --nomigrate to the command. Aborting.")
      sys.exit(1)
    } else {
      if (Process(
        s"${restoreBin} --upgrade ${restoreDir}",
        None,
        ("JVM_OPT", s"-Dconfig.file=${base}/conf/predictionio.conf -Dio.prediction.base=${base}")).! != 0) {
        println("Restore utility returned non-zero exit code. Aborting.")
        sys.exit(1)
      }
    }
  }
}

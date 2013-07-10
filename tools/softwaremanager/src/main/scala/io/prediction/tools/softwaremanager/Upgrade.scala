package io.prediction.tools.softwaremanager

import io.prediction.commons._

import scala.collection.JavaConversions._
import scala.sys.process._

import com.twitter.scalding.Args
import com.typesafe.config._
import org.apache.commons.io.FileUtils._

/** Upgrades previous version to current version. */
object Upgrade {
  def main(cargs: Array[String]) {
    val args = Args(cargs)

    println("PredictionIO Software Upgrade")
    println()

    val steps = 5
    val current = args("current")
    val latest = args("latest")

    /** Make sure both current and latest directories exist. */
    val currentDir = getFile(current)
    val latestDir = getFile(latest)

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
    println(s"Step 1 of ${steps}: Creating backup...")
    println()
    for (backupDir <- dirs) {
      println(s"Backing up ${backupDir}...")
      moveDirectoryToDirectory(getFile(currentDir, backupDir), getFile(currentDir, "backup"), true)
    }
    println()

    /** Copy new files. */
    println(s"Step 2 of ${steps}: Placing new files...")
    println()
    for (dir <- dirs) {
      println(s"Placing ${dir}...")
      moveDirectoryToDirectory(getFile(latestDir, dir), currentDir, true)
    }
    println()

    /** Merge old config to new config. */
    println(s"Step 3 or ${steps}: Merging old configuration to new configuration...")
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

    println(s"Step 4 of ${steps}: Retaining old logger configuration...")
    println()
    val loggerConfs = Seq("admin-logger.xml", "api-logger.xml", "scheduler-logger.xml")
    loggerConfs foreach { conf =>
      println(s"Retaining ${conf}...")
      copyFileToDirectory(getFile(currentDir, "backup", "conf", conf), getFile(currentDir, "conf"))
    }
    println()

    println(s"Step 5 of ${steps}: Clean up temporary files...")
    deleteDirectory(latestDir)
    println()

    println("Upgrade completed.")
  }
}

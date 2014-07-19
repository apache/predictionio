package io.prediction.tools

import grizzled.slf4j.Logging
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.json4s._
import org.json4s.native.Serialization.{ read, write }

import scala.io.Source

import java.io.File

import io.prediction.storage.Storage
import io.prediction.storage.EngineManifest
import io.prediction.storage.EngineManifestSerializer

object RegisterEngine extends Logging {
  case class Args(
    jsonManifest: String = "",
    engineFiles: Seq[File] = Seq())

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Args]("RegisterEngine") {
      arg[String]("<engine manifest JSON file>") action { (x, c) =>
        c.copy(jsonManifest = x)
      } text("the JSON file that contains the engine's manifest")
      arg[File]("<engine files>...") unbounded() action { (x, c) =>
        c.copy(engineFiles = c.engineFiles :+ x)
      } text("engine files (JARs)")
    }

    parser.parse(args, Args()) map { config =>
      implicit val formats = DefaultFormats + new EngineManifestSerializer
      val jsonString = try {
        Source.fromFile(config.jsonManifest).mkString
      } catch {
        case e: java.io.FileNotFoundException =>
          error(s"Engine manifest file not found: ${e.getMessage}. Aborting.")
          sys.exit(1)
      }
      val engineManifest = read[EngineManifest](jsonString)

      // Configure local FS or HDFS
      val conf = new Configuration
      val localFs = FileSystem.getLocal(conf)
      val fs = FileSystem.get(conf)
      val enginesdir = sys.env.get("PIO_FS_ENGINESDIR") match {
        case Some(s) => s
        case None =>
          error("Environmental variable PIO_FS_ENGINESDIR is not set!")
          sys.exit(1)
      }

      val destDir = Seq(enginesdir, engineManifest.id, engineManifest.version)
      val destPath = new Path(destDir.mkString(Path.SEPARATOR_CHAR + ""))
      fs.mkdirs(destPath)
      localFs.mkdirs(destPath)
      val files = config.engineFiles.flatMap { f =>
        val destFilePath =
          new Path(destDir.:+(f.getName).mkString(Path.SEPARATOR_CHAR + ""))
        val destPathString = fs.makeQualified(destFilePath).toString
        if (fs.exists(destFilePath) &&
          f.length == fs.getFileStatus(destFilePath).getLen)
          info(s"Skip copying ${f.toURI} because ${destPathString} exists and their file sizes are equal")
        else {
          info(s"Copying ${f.toURI} to ${destPathString}")
          fs.copyFromLocalFile(new Path(f.toURI), destPath)
        }
        val localDestPathString = localFs.makeQualified(destFilePath).toString
        if (localFs.exists(destFilePath) &&
          f.length == localFs.getFileStatus(destFilePath).getLen)
          info(s"Skip copying ${f.toURI} because ${destPathString} exists and their file sizes are equal")
        else {
          info(s"Copying ${f.toURI} to ${localDestPathString}")
          localFs.copyFromLocalFile(new Path(f.toURI), destPath)
        }
        Seq(destPathString, localDestPathString)
      }

      info(s"Registering engine ${engineManifest.id} ${engineManifest.version}")
      val engineManifests = Storage.getSettingsEngineManifests
      engineManifests.update(engineManifest.copy(files = files), true)

    }
  }
}

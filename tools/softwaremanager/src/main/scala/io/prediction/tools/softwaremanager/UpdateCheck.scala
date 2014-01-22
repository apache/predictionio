package io.prediction.tools.softwaremanager

import io.prediction.commons._

import scala.reflect.ClassTag
import scala.sys.process._
import scala.util.parsing.json.JSON

import org.apache.commons.io.FileUtils._

case class UpdateCheckConfig(localVersion: String = "", answer: String = "")

object UpdateCheck {
  val config = new Config()
  val systemInfos = config.getSettingsSystemInfos

  def main(args: Array[String]) {
    val parser = new scopt.OptionParser[UpdateCheckConfig]("updatecheck") {
      head("PredictionIO Update Checker", "0.6.7")
      help("help") text ("prints this usage text")
      opt[String]("localVersion") action { (x, c) =>
        c.copy(localVersion = x)
      } text ("read version information from a local file instead")
      opt[String]("answer") action { (x, c) =>
        c.copy(answer = x)
      } text ("'y' to proceed downloading update if found; 'n' to skip")
    }

    parser.parse(args, UpdateCheckConfig()) map { updateCheckConfig =>
      println("PredictionIO Update Checker")
      println()

      val installed = systemInfos.get("version") map { _.value } getOrElse {
        println("Cannot detect any previous version. Possible causes:")
        println("- PredictionIO version < 0.5.0")
        println("- misconfiguration (wrong settings database pointers)")
        println("- settings database has been corrupted")
        println()
        println("Update check aborted.")
        sys.exit(1)
      }

      val localVersion = updateCheckConfig.localVersion
      val versions = if (localVersion != "") {
        println(s"Using local version file ${localVersion}...")
        println()
        try {
          Versions(localVersion)
        } catch {
          case e: java.io.FileNotFoundException => {
            println(s"Error: ${e.getMessage}. Aborting.")
            sys.exit(1)
          }
        }
      } else {
        try {
          println(s"Using ${Versions.versionsUrl}...")
          println()
          Versions()
        } catch {
          case e: java.net.UnknownHostException => {
            println(s"Error: Unknown host: ${e.getMessage}. Aborting.")
            sys.exit(1)
          }
          case e: java.io.FileNotFoundException => {
            println(s"Error: File not found: ${e.getMessage}. Aborting.")
            sys.exit(1)
          }
        }
      }

      val latest = versions.latestVersion
      println(s"   Latest version: ${latest}")
      println(s"Installed version: ${installed}")
      println()

      if (latest != installed) {
        val answer = updateCheckConfig.answer
        val choice = if (answer == "") {
          println("Your PredictionIO is not the latest version. Do you want to download and install the latest binaries?")
          val input = readLine("Enter 'YES' to proceed: ")
          input match {
            case "YES" => "y"
            case _ => "a"
          }
        } else answer
        choice match {
          case "y" => {
            val binaries = versions.binaries(latest) map { b =>
              println(s"Retrieving ${b}...")
              s"curl -O ${b}".!
              val filename = b.split('/').reverse.head
              val dirname = filename.split('.').dropRight(1).mkString(".")
              val extractedDir = getFile(dirname)
              if (extractedDir.exists) { deleteDirectory(extractedDir) }
              s"unzip ${filename}".!
              if (localVersion != "")
                s"${dirname}/bin/upgrade --localVersion ${localVersion} ${config.base} ${dirname}".!
              else
                s"${dirname}/bin/upgrade ${config.base} ${dirname}".!
            }
          }
          case "n" => println(s"Your PredictionIO is not the latest version. A new version ${latest} is available.")
          case "a" => println("Aborting.")
        }
      } else {
        println("Your PredictionIO version is already the latest.")
      }
    }
  }
}

package io.prediction.tools.softwaremanager

import io.prediction.commons._

import scala.reflect.ClassTag
import scala.sys.process._
import scala.util.parsing.json.JSON

import com.twitter.scalding.Args

object UpdateCheck {
  val config = new Config()
  val systemInfos = config.getSettingsSystemInfos

  def main(cargs: Array[String]) {
    val args = Args(cargs)

    println("PredictionIO Update Checker")
    println()

    val installed = systemInfos.get("version") map { _.value } getOrElse {
      println("Cannot detect any previous version. Possible causes:")
      println("- PredictionIO version is too old (<= 0.4.2)")
      println("- misconfiguration (wrong settings database pointers)")
      println("- settings database has been corrupted")
      println()
      println("Update check aborted.")
      sys.exit(1)
    }

    val versions = args.optional("localVersion") map { lv =>
      println(s"Using local version file ${lv}...")
      println()
      try {
        Versions(lv)
      } catch {
        case e: java.io.FileNotFoundException => {
          println(s"Error: ${e.getMessage}. Aborting.")
          sys.exit(1)
        }
      }
    } getOrElse {
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
      val choice = args.optional("answer") getOrElse {
        println("Your PredictionIO is not the latest version. Do you want to download the latest binaries?")
        val input = readLine("Enter 'YES' to proceed: ")
        input match {
          case "YES" => "y"
          case _ => "a"
        }
      }
      choice match {
        case "y" => {
          val binaries = versions.binaries(latest) map { b =>
            println(s"Retrieving ${b}...")
            s"curl -O ${b}".!
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

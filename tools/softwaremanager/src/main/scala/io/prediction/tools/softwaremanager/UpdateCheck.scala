package io.prediction.tools.softwaremanager

import io.prediction.commons._

import scala.reflect.ClassTag
import scala.sys.process._
import scala.util.parsing.json.JSON

/** Extractors: http://stackoverflow.com/questions/4170949/how-to-parse-json-in-scala-using-standard-scala-classes */
class CC[T : ClassTag] { def unapply(a: Any)(implicit e: ClassTag[T]): Option[T] = {
  try { Some(e.runtimeClass.cast(a).asInstanceOf[T]) } catch { case _: Throwable => None } }
}

object M extends CC[Map[String, Any]]
object MSS extends CC[Map[String, String]]
object SS extends CC[Seq[String]]
object OSS extends CC[Option[Seq[String]]]
object S extends CC[String]
object OS extends CC[Option[String]]

object UpdateCheck {
  val config = new Config()
  val systemInfos = config.getSettingsSystemInfos

  def main(args: Array[String]) {
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

    val versionsString = try {
      println(s"Using local version file ${args(0)}...")
      println()
      scala.io.Source.fromFile(args(0)).mkString
    } catch {
      case e: java.io.FileNotFoundException => {
        println(s"Error: ${e.getMessage}. Aborting.")
        sys.exit(1)
      }
      case e: Throwable => try {
        println(s"Using http://download.prediction.io/versions.json...")
        println()
        scala.io.Source.fromURL("http://download.prediction.io/versions.json").mkString
      } catch { case e: Throwable =>
        println(s"Error: ${e.getMessage}. Aborting.")
        sys.exit(1)
      }
    }

    val versionsJson = JSON.parseFull(versionsString) getOrElse {
      println(s"Unable to parse version file. Aborting.")
    }

    M.unapply(versionsJson) map { meta =>
      S.unapply(meta("latest")) map { latest =>
        println(s"   Latest version: ${latest}")
        println(s"Installed version: ${installed}")
        println()

        if (latest != installed) {
          println("Your PredictionIO is not the latest version. Do you want to download the latest binaries?")
          val choice = readLine("Enter 'YES' to proceed: ")
          choice match {
            case "YES" => {
              M.unapply(meta("versions")) map { versions =>
                MSS.unapply(versions(latest)) map { latestVersion =>
                  val binaries = latestVersion("binaries")
                  println(s"Retrieving ${binaries}...")
                  s"curl -O ${binaries}".!
                }
              }
            }
            case _ => println("Aborting.")
          }
        } else {
          println("Your PredictionIO version is already the latest.")
        }
      } getOrElse println("Cannot find the latest version. Aborting.")
    } getOrElse println("Root level is not a valid JSON object. Aborting.")
  }
}

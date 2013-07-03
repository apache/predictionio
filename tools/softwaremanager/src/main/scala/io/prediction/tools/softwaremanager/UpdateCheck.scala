package io.prediction.tools.softwaremanager

import io.prediction.commons._

import scala.reflect.ClassTag
import scala.sys.process._
import scala.util.parsing.json.JSON

import com.twitter.scalding.Args

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

    val versionsString = args.optional("localVersion") map { lv =>
      println(s"Using local version file ${lv}...")
      println()
      try {
        scala.io.Source.fromFile(lv).mkString
      } catch {
        case e: java.io.FileNotFoundException => {
          println(s"Error: ${e.getMessage}. Aborting.")
          sys.exit(1)
        }
      }
    } getOrElse {
      try {
        println(s"Using http://direct.prediction.io/versions.json...")
        println()
        scala.io.Source.fromURL("http://direct.prediction.io/versions.json").mkString
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
          val choice = args.optional("answer") getOrElse {
            println("Your PredictionIO is not the latest version. Do you want to download the latest binaries?")
            val input = readLine("Enter 'YES' to proceed: ")
            input match {
              case "YES" => "y"
              case _ => "a"
            }
          }
          choice match {
            case "y" =>
              M.unapply(meta("versions")) map { versions =>
                MSS.unapply(versions(latest)) map { latestVersion =>
                  val binaries = latestVersion("binaries")
                  println(s"Retrieving ${binaries}...")
                  s"curl -O ${binaries}".!
                }
              }
            case "n" => println(s"Your PredictionIO is not the latest version. A new version ${latest} is available.")
            case "a" => println("Aborting.")
          }
        } else {
          println("Your PredictionIO version is already the latest.")
        }
      } getOrElse println("Cannot find the latest version. Aborting.")
    } getOrElse println("Root level is not a valid JSON object. Aborting.")
  }
}

package io.prediction.tools

import io.prediction.storage.Run
import io.prediction.storage.Storage

import grizzled.slf4j.Logging
import org.json4s._
import org.json4s.native.Serialization.{ read, write }

import scala.sys.process._

import java.io.File

object RunServer extends Logging {
  def main(args: Array[String]): Unit = {
    case class RunServerConfig(
      sparkHome: String = "",
      sparkMaster: String = "local",
      sparkDeployMode: String = "client",
      runId: String = "",
      engineId: Option[String] = None,
      engineVersion: Option[String] = None,
      ip: String = "localhost",
      port: Int = 8000)

    val parser = new scopt.OptionParser[RunServerConfig]("RunServer") {
      opt[String]("sparkHome") action { (x, c) =>
        c.copy(sparkHome = x)
      } text("Path to a Apache Spark installation. If not specified, will " +
        "try to use the SPARK_HOME environmental variable. If this fails as " +
        "well, default to current directory.")
      opt[String]("sparkMaster") action { (x, c) =>
        c.copy(sparkMaster = x)
      } text("Apache Spark master URL. If not specified, default to local.")
      opt[String]("sparkDeployMode") action { (x, c) =>
        c.copy(sparkDeployMode = x)
      } text("Apache Spark deploy mode. If not specified, default to client.")
      opt[String]("runId") required() action { (x, c) =>
        c.copy(runId = x)
      } text("Run ID.")
      opt[String]("engineId") action { (x, c) =>
        c.copy(engineId = Some(x))
      } text("Engine ID.")
      opt[String]("engineVersion") action { (x, c) =>
        c.copy(engineVersion = Some(x))
      } text("Engine version.")
      opt[String]("ip") action { (x, c) =>
        c.copy(ip = x)
      } text("IP to bind to (default: localhost).")
      opt[Int]("port") action { (x, c) =>
        c.copy(port = x)
      } text("Port to bind to (default: 8000).")
    }

    parser.parse(args, RunServerConfig()) map { sc =>
      val runs = Storage.getMetaDataRuns
      val engineManifests = Storage.getMetaDataEngineManifests
      runs.get(sc.runId).map { run =>
        engineManifests.get(run.engineId, run.engineVersion).map { em =>
          val pioEnvVars = sys.env.filter(kv => kv._1.startsWith("PIO_")).map(kv =>
            s"${kv._1}=${kv._2}"
          ).mkString(",")
          val sparkHome =
            if (sc.sparkHome != "") sc.sparkHome
            else sys.env.get("SPARK_HOME").getOrElse(".")
          val sparkSubmit = Seq(
            s"${sparkHome}/bin/spark-submit",
            "--verbose",
            "--deploy-mode",
            sc.sparkDeployMode,
            "--master",
            sc.sparkMaster,
            "--class",
            "io.prediction.deploy.CreateServer") ++ (
              if (em.files.size > 1) Seq(
                "--jars",
                em.files.drop(1).mkString(","))
              else Nil) ++ Seq(
            em.files.head,
            //"--env",
            //pioEnvVars,
            "--runId",
            sc.runId,
            "--ip",
            sc.ip,
            "--port",
            sc.port.toString) ++
            sc.engineId.map(x => Seq("--engineId", x)).getOrElse(Seq()) ++
            sc.engineVersion.map(x => Seq("--engineVersion", x)).getOrElse(Seq())

          if (sc.sparkDeployMode == "cluster")
            Process(sparkSubmit, None, "SPARK_YARN_USER_ENV" -> pioEnvVars).!
          else
            Process(sparkSubmit).!
        } getOrElse {
          error(s"Engine ${run.engineId} ${run.engineVersion} is not registered.")
        }
      } getOrElse {
        error(s"Run ID ${sc.runId} is not valid!")
      }
    }
  }
}

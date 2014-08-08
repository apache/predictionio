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
      core: String = "",
      sparkHome: String = "",
      sparkMaster: String = "local",
      sparkDeployMode: String = "client",
      runId: Option[String] = None,
      engineId: Option[String] = None,
      engineVersion: Option[String] = None,
      ip: String = "localhost",
      port: Int = 8000)

    val parser = new scopt.OptionParser[RunServerConfig]("RunServer") {
      opt[String]("core") required() action { (x, c) =>
        c.copy(core = x)
      } text("PredictionIO core assembly.")
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
      opt[String]("runId") action { (x, c) =>
        c.copy(runId = Some(x))
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
      val run = sc.runId.map { runId =>
        runs.get(runId).getOrElse {
          error(s"${runId} is not a valid run ID!")
          sys.exit(1)
        }
      } getOrElse {
        (sc.engineId, sc.engineVersion) match {
          case (Some(engineId), Some(engineVersion)) =>
            runs.getLatestCompleted(engineId, engineVersion).getOrElse {
              error(s"No valid run found for ${engineId} ${engineVersion}!")
              sys.exit(1)
            }
          case _ =>
            error("If --runId was not specified, both --engineId and --engineVersion must be specified.")
            sys.exit(1)
        }
      }
      val em = engineManifests.get(run.engineId, run.engineVersion).getOrElse {
        error(s"${run.engineId} ${run.engineVersion} is not registered!")
        sys.exit(1)
      }

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
        "io.prediction.workflow.CreateServer",
        "--jars",
        em.files.mkString(","),
        sc.core,
        //"--env",
        //pioEnvVars,
        "--runId",
        run.id,
        "--ip",
        sc.ip,
        "--port",
        sc.port.toString) ++
        sc.engineId.map(x => Seq("--engineId", x)).getOrElse(Seq()) ++
        sc.engineVersion.map(x => Seq("--engineVersion", x)).getOrElse(Seq())

      val proc = if (sc.sparkDeployMode == "cluster")
        Process(sparkSubmit, None, "SPARK_YARN_USER_ENV" -> pioEnvVars).run
      else
        Process(sparkSubmit).run
      Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
        def run(): Unit = {
          proc.destroy
        }
      }))
      proc.exitValue
    }
  }

  def runServer(
      ca: ConsoleArgs,
      runId: String,
      core: File,
      files: Seq[File]): Unit = {
    val pioEnvVars = sys.env.filter(kv => kv._1.startsWith("PIO_")).map(kv =>
      s"${kv._1}=${kv._2}"
    ).mkString(",")

    val sparkHome = ca.sparkHome.getOrElse(
      sys.env.get("SPARK_HOME").getOrElse("."))

    val sparkSubmit = Seq(
      s"${sparkHome}/bin/spark-submit") ++ ca.passThrough ++ Seq(
      "--class",
      "io.prediction.workflow.CreateServer",
      "--jars",
      files.map(_.getCanonicalPath).mkString(","),
      core.getCanonicalPath,
      "--runId",
      runId,
      "--ip",
      ca.ip,
      "--port",
      ca.port.toString)

    val proc =
      Process(sparkSubmit, None, "SPARK_YARN_USER_ENV" -> pioEnvVars).run
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      def run(): Unit = {
        proc.destroy
      }
    }))
    proc.exitValue
  }
}

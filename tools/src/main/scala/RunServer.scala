package io.prediction.tools

import io.prediction.storage.EngineInstance
import io.prediction.storage.EngineManifest
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
      engineInstanceId: Option[String] = None,
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
      opt[String]("engineInstanceId") action { (x, c) =>
        c.copy(engineInstanceId = Some(x))
      } text("Engine instance ID.")
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
      val engineInstances = Storage.getMetaDataEngineInstances
      val engineManifests = Storage.getMetaDataEngineManifests
      val engineInstance = sc.engineInstanceId.map { engineInstanceId =>
        engineInstances.get(engineInstanceId).getOrElse {
          error(s"${engineInstanceId} is not a valid engine instance ID!")
          sys.exit(1)
        }
      } getOrElse {
        (sc.engineId, sc.engineVersion) match {
          case (Some(engineId), Some(engineVersion)) =>
            engineInstances.getLatestCompleted(engineId, engineVersion).
              getOrElse {
                error(s"No valid engine instance found for ${engineId} ${engineVersion}!")
                sys.exit(1)
              }
          case _ =>
            error("If --engineInstanceId was not specified, both --engineId " +
              "and --engineVersion must be specified.")
            sys.exit(1)
        }
      }
      val em = engineManifests.get(
        engineInstance.engineId,
        engineInstance.engineVersion).getOrElse {
        error(s"${engineInstance.engineId} ${engineInstance.engineVersion} " +
          "is not registered!")
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
        "--engineInstanceId",
        engineInstance.id,
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
      core: File,
      em: EngineManifest,
      engineInstanceId: String): Unit = {
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
      em.files.mkString(","),
      core.getCanonicalPath,
      "--engineInstanceId",
      engineInstanceId,
      "--ip",
      ca.ip,
      "--port",
      ca.port.toString)

    info(s"Submission command: ${sparkSubmit.mkString(" ")}")

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

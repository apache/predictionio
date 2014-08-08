package io.prediction.tools

import io.prediction.storage.EngineManifest
import io.prediction.storage.Run
import io.prediction.storage.Storage

import grizzled.slf4j.Logging

import scala.sys.process._

import java.io.File

object RunWorkflow extends Logging {
  def main(args: Array[String]): Unit = {
    case class RunWorkflowConfig(
      core: String = "",
      sparkHome: String = "",
      sparkMaster: String = "local",
      sparkDeployMode: String = "client",
      batch: String = "Transient Lazy Val",
      engineId: String = "",
      engineVersion: String = "",
      metricsClass: Option[String] = None,
      dataSourceParamsJsonPath: Option[String] = None,
      preparatorParamsJsonPath: Option[String] = None,
      algorithmsParamsJsonPath: Option[String] = None,
      servingParamsJsonPath: Option[String] = None,
      metricsParamsJsonPath: Option[String] = None,
      jsonBasePath: String = ".")

    val parser = new scopt.OptionParser[RunWorkflowConfig]("RunWorkflow") {
      opt[String]("core") required() action { (x, c) =>
        c.copy(core = x)
      } text("PredictionIO core assembly.")
      opt[String]("engineId") required() action { (x, c) =>
        c.copy(engineId = x)
      } text("Engine ID.")
      opt[String]("engineVersion") required() action { (x, c) =>
        c.copy(engineVersion = x)
      } text("Engine version.")
      opt[String]("metricsClass") action { (x, c) =>
        c.copy(metricsClass = Some(x))
      } text("Metrics class name to run.")
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
      opt[String]("batch") action { (x, c) =>
        c.copy(batch = x)
      } text("Batch label of the run.")
      opt[String]("jsonBasePath") action { (x, c) =>
        c.copy(jsonBasePath = x)
      } text("Directory to lookup parameters JSON files. Default: .")
      opt[String]("dsp") action { (x, c) =>
        c.copy(dataSourceParamsJsonPath = Some(x))
      } text("Data source parameters JSON file. Will try to use " +
        "dataSourceParams.json in the base path.")
      opt[String]("pp") action { (x, c) =>
        c.copy(preparatorParamsJsonPath = Some(x))
      } text("Preparator parameters JSON file. Will try to use " +
        "preparatorParams.json in the base path.")
      opt[String]("ap") action { (x, c) =>
        c.copy(algorithmsParamsJsonPath = Some(x))
      } text("Algorithms parameters JSON file. Will try to use " +
        "algorithmsParams.json in the base path.")
      opt[String]("sp") action { (x, c) =>
        c.copy(servingParamsJsonPath = Some(x))
      } text("Serving parameters JSON file. Will try to use " +
        "servingParams.json in the base path.")
      opt[String]("mp") action { (x, c) =>
        c.copy(metricsParamsJsonPath = Some(x))
      } text("Metrics parameters JSON file. Will try to use " +
        "metricsParams.json in the base path.")
    }

    parser.parse(args, RunWorkflowConfig()) map { wfc =>
      // Collect and serialize PIO_* environmental variables
      val pioEnvVars = sys.env.filter(kv => kv._1.startsWith("PIO_")).map(kv =>
        s"${kv._1}=${kv._2}"
      ).mkString(",")

      val defaults = Map(
        "dsp" -> (wfc.dataSourceParamsJsonPath, "dataSourceParams.json"),
        "pp" -> (wfc.preparatorParamsJsonPath, "preparatorParams.json"),
        "ap" -> (wfc.algorithmsParamsJsonPath, "algorithmsParams.json"),
        "sp" -> (wfc.servingParamsJsonPath, "servingParams.json"),
        "mp" -> (wfc.metricsParamsJsonPath, "metricsParams.json"))

      val engineManifests = Storage.getMetaDataEngineManifests
      engineManifests.get(wfc.engineId, wfc.engineVersion) map { em =>
        val sparkHome =
          if (wfc.sparkHome != "") wfc.sparkHome
          else sys.env.get("SPARK_HOME").getOrElse(".")
        val sparkSubmit = Seq(
          s"${sparkHome}/bin/spark-submit",
          "--verbose",
          "--deploy-mode",
          wfc.sparkDeployMode,
          "--master",
          wfc.sparkMaster,
          "--class",
          "io.prediction.workflow.CreateWorkflow",
          "--jars",
          em.files.mkString(","),
          wfc.core,
          "--env",
          pioEnvVars,
          "--engineId",
          wfc.engineId,
          "--engineVersion",
          wfc.engineVersion,
          "--engineFactory",
          em.engineFactory) ++
          wfc.metricsClass.map(x => Seq("--metricsClass", x)).
            getOrElse(Seq()) ++
          (if (wfc.batch != "") Seq("--batch", wfc.batch) else Seq()) ++ Seq(
          "--jsonBasePath", wfc.jsonBasePath) ++ defaults.flatMap { _ match {
            case (key, (path, default)) =>
              path.map(p => Seq(s"--$key", p)).getOrElse {
              if ((new File(withPath(default, wfc.jsonBasePath))).exists)
                Seq(s"--$key", default)
              else
                Seq()
            }
          }}
        if (wfc.sparkDeployMode == "cluster")
          Process(sparkSubmit, None, "SPARK_YARN_USER_ENV" -> pioEnvVars).!
        else
          Process(sparkSubmit).!
      } getOrElse {
        error(s"Engine ${wfc.engineId} ${wfc.engineVersion} is not registered.")
      }
    }
  }

  private def withPath(file: String, path: String) =
    path + File.separator + file

  def runWorkflow(
      ca: ConsoleArgs,
      core: File,
      em: EngineManifest,
      files: Seq[File]): Unit = {
    // Collect and serialize PIO_* environmental variables
    val pioEnvVars = sys.env.filter(kv => kv._1.startsWith("PIO_")).map(kv =>
      s"${kv._1}=${kv._2}"
    ).mkString(",")

    val defaults = Map(
      "dsp" -> (ca.dataSourceParamsJsonPath, "datasource.json"),
      "pp" -> (ca.preparatorParamsJsonPath, "preparator.json"),
      "ap" -> (ca.algorithmsParamsJsonPath, "algorithms.json"),
      "sp" -> (ca.servingParamsJsonPath, "serving.json"),
      "mp" -> (ca.metricsParamsJsonPath, "metrics.json"))

    val sparkHome = ca.sparkHome.getOrElse(
      sys.env.get("SPARK_HOME").getOrElse("."))

    val sparkSubmit = Seq(
      s"${sparkHome}/bin/spark-submit") ++ ca.passThrough ++ Seq(
      "--class",
      "io.prediction.workflow.CreateWorkflow",
      "--jars",
      files.map(_.getCanonicalPath).mkString(","),
      core.getCanonicalPath,
      "--env",
      pioEnvVars,
      "--engineId",
      em.id,
      "--engineVersion",
      em.version,
      "--engineFactory",
      em.engineFactory) ++
      ca.metricsClass.map(x => Seq("--metricsClass", x)).
        getOrElse(Seq()) ++
      (if (ca.batch != "") Seq("--batch", ca.batch) else Seq()) ++ Seq(
      "--jsonBasePath", ca.paramsPath) ++ defaults.flatMap { _ match {
        case (key, (path, default)) =>
          path.map(p => Seq(s"--$key", p)).getOrElse {
          if (new File(withPath(default, ca.paramsPath)).exists)
            Seq(s"--$key", default)
          else
            Seq()
        }
      }}
    info(s"Submission command: ${sparkSubmit.mkString(" ")}")
    Process(sparkSubmit, None, "SPARK_YARN_USER_ENV" -> pioEnvVars).!
  }
}

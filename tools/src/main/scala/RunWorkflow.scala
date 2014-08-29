package io.prediction.tools

import io.prediction.storage.EngineManifest

import grizzled.slf4j.Logging
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path

import scala.sys.process._

import java.io.File

object RunWorkflow extends Logging {
  def runWorkflow(
      ca: ConsoleArgs,
      core: File,
      em: EngineManifest): Unit = {
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

    val hadoopConf = new Configuration
    val hdfs = FileSystem.get(hadoopConf)

    val sparkSubmit =
      Seq(Seq(sparkHome, "bin", "spark-submit").mkString(File.separator)) ++
      ca.passThrough ++
      Seq(
        "--class",
        "io.prediction.workflow.CreateWorkflow",
        "--name",
        s"PredictionIO: ${em.id} ${em.version} (${ca.batch})",
        "--jars",
        em.files.mkString(","),
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
          if (hdfs.exists(new Path(ca.paramsPath + Path.SEPARATOR + default)))
            Seq(s"--$key", default)
          else
            Seq()
        }
      }}
    info(s"Submission command: ${sparkSubmit.mkString(" ")}")
    Process(sparkSubmit, None, "SPARK_YARN_USER_ENV" -> pioEnvVars).!
  }
}

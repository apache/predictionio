package io.prediction.tools

import io.prediction.data.storage.EngineManifest

import grizzled.slf4j.Logging

import scala.sys.process._

import java.io.File

object RunServer extends Logging {
  def runServer(
      ca: ConsoleArgs,
      core: File,
      em: EngineManifest,
      engineInstanceId: String): Unit = {
    val pioEnvVars = sys.env.filter(kv => kv._1.startsWith("PIO_")).map(kv =>
      s"${kv._1}=${kv._2}"
    ).mkString(",")

    val sparkHome = ca.common.sparkHome.getOrElse(
      sys.env.get("SPARK_HOME").getOrElse("."))

    val sparkSubmit =
      Seq(Seq(sparkHome, "bin", "spark-submit").mkString(File.separator)) ++
      ca.common.passThrough ++
      Seq(
        "--class",
        "io.prediction.workflow.CreateServer",
        "--name",
        s"PredictionIO Engine Instance: ${engineInstanceId}",
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

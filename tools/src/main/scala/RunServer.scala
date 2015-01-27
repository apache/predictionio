/** Copyright 2014 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.tools

import io.prediction.data.storage.EngineManifest
import io.prediction.workflow.WorkflowUtils

import grizzled.slf4j.Logging

import scala.sys.process._

import java.io.File

object RunServer extends Logging {
  def runServer(
      ca: ConsoleArgs,
      core: File,
      em: EngineManifest,
      engineInstanceId: String): Int = {
    val pioEnvVars = sys.env.filter(kv => kv._1.startsWith("PIO_")).map(kv =>
      s"${kv._1}=${kv._2}"
    ).mkString(",")

    val sparkHome = ca.common.sparkHome.getOrElse(
      sys.env.get("SPARK_HOME").getOrElse("."))

    val extraFiles = WorkflowUtils.thirdPartyConfFiles

    val driverClassPathIndex =
      ca.common.sparkPassThrough.indexOf("--driver-class-path")
    val driverClassPathPrefix =
      if (driverClassPathIndex != -1)
        Seq(ca.common.sparkPassThrough(driverClassPathIndex + 1))
      else
        Seq()
    val extraClasspaths =
      driverClassPathPrefix ++ WorkflowUtils.thirdPartyClasspaths

    val deployModeIndex =
      ca.common.sparkPassThrough.indexOf("--deploy-mode")
    val deployMode = if (deployModeIndex != -1)
      ca.common.sparkPassThrough(deployModeIndex + 1)
    else
      "client"

    val mainJar =
      if (ca.build.uberJar) {
        if (deployMode == "cluster")
          em.files.filter(_.startsWith("hdfs")).head
        else
          em.files.filterNot(_.startsWith("hdfs")).head
      } else {
        if (deployMode == "cluster") {
          em.files.filter(_.contains("pio-assembly")).head
        } else {
          core.getCanonicalPath
        }
      }

    val sparkSubmit =
      Seq(Seq(sparkHome, "bin", "spark-submit").mkString(File.separator)) ++
      ca.common.sparkPassThrough ++
      Seq(
        "--class",
        "io.prediction.workflow.CreateServer",
        "--name",
        s"PredictionIO Engine Instance: ${engineInstanceId}") ++
      (if (!ca.build.uberJar) {
        Seq(
          "--jars",
          (em.files ++ Console.builtinEngines(
            ca.common.pioHome.get).map(_.getCanonicalPath)).mkString(","))
      } else Seq()) ++
      (if (extraFiles.size > 0)
        Seq("--files", extraFiles.mkString(","))
      else
        Seq()) ++
      (if (extraClasspaths.size > 0)
        Seq("--driver-class-path", extraClasspaths.mkString(":"))
      else
        Seq()) ++
      (if (ca.common.sparkKryo)
        Seq(
          "--conf",
          "spark.serializer=org.apache.spark.serializer.KryoSerializer")
      else
        Seq()) ++
      Seq(
        mainJar,
        "--engineInstanceId",
        engineInstanceId,
        "--ip",
        ca.deploy.ip,
        "--port",
        ca.deploy.port.toString,
        "--event-server-ip",
        ca.eventServer.ip,
        "--event-server-port",
        ca.eventServer.port.toString,
        "--log-file",
        ca.common.logFile) ++
      (if (ca.accessKey.accessKey != "")
        Seq("--accesskey", ca.accessKey.accessKey) else Seq()) ++
      (if (ca.eventServer.enabled) Seq("--feedback") else Seq()) ++
      (if (ca.batch != "") Seq("--batch", ca.batch) else Seq()) ++
      (if (ca.common.verbose) Seq("--verbose") else Seq()) ++
      (if (ca.common.debug) Seq("--debug") else Seq()) ++
      ca.deploy.logUrl.map(x => Seq("--log-url", x)).getOrElse(Seq()) ++
      ca.deploy.logPrefix.map(x => Seq("--log-prefix", x)).getOrElse(Seq())

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

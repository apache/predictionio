/** Copyright 2015 TappingStone, Inc.
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

import io.prediction.tools.console.ConsoleArgs
import io.prediction.workflow.WorkflowUtils

import grizzled.slf4j.Logging

import scala.sys.process._

import java.io.File

object Runner extends Logging {
  def envStringToMap(env: String): Map[String, String] =
    env.split(',').flatMap(p =>
      p.split('=') match {
        case Array(k, v) => List(k -> v)
        case _ => Nil
      }
    ).toMap

  def runOnSpark(
      className: String,
      classArgs: Seq[String],
      ca: ConsoleArgs,
      core: File): Int = {
    // Collect and serialize PIO_* environmental variables
    val pioEnvVars = sys.env.filter(kv => kv._1.startsWith("PIO_")).map(kv =>
      s"${kv._1}=${kv._2}"
    ).mkString(",")

    val sparkHome = ca.common.sparkHome.getOrElse(
      sys.env.get("SPARK_HOME").getOrElse("."))

    val mainJar = core.getCanonicalPath

    val driverClassPathIndex =
      ca.common.sparkPassThrough.indexOf("--driver-class-path")
    val driverClassPathPrefix =
      if (driverClassPathIndex != -1) {
        Seq(ca.common.sparkPassThrough(driverClassPathIndex + 1))
      } else {
        Seq()
      }
    val extraClasspaths =
      driverClassPathPrefix ++ WorkflowUtils.thirdPartyClasspaths

    val extraFiles = WorkflowUtils.thirdPartyConfFiles

    val sparkSubmitCommand =
      Seq(Seq(sparkHome, "bin", "spark-submit").mkString(File.separator))

    val sparkSubmitFiles = if (extraFiles.size > 0) {
      Seq("--files", extraFiles.mkString(","))
    } else {
      Seq("")
    }

    val sparkSubmitExtraClasspaths = if (extraClasspaths.size > 0) {
      Seq("--driver-class-path", extraClasspaths.mkString(":"))
    } else {
      Seq("")
    }

    val sparkSubmitKryo = if (ca.common.sparkKryo) {
      Seq(
        "--conf",
        "spark.serializer=org.apache.spark.serializer.KryoSerializer")
    } else {
      Seq("")
    }

    val verbose = if (ca.common.verbose) { Seq("--verbose") } else { Seq() }

    val sparkSubmit = Seq(
      sparkSubmitCommand,
      ca.common.sparkPassThrough,
      Seq("--class", className),
      sparkSubmitFiles,
      sparkSubmitExtraClasspaths,
      sparkSubmitKryo,
      Seq(mainJar),
      classArgs,
      Seq("--env", pioEnvVars),
      verbose).flatten.filter(_ != "")
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

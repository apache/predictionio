/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.predictionio.tools.commands

import org.apache.predictionio.core.BuildInfo
import org.apache.predictionio.controller.Utils
import org.apache.predictionio.data.storage
import org.apache.predictionio.data.storage.EngineManifest
import org.apache.predictionio.data.storage.EngineManifestSerializer
import org.apache.predictionio.tools.RegisterEngine
import org.apache.predictionio.tools.EitherLogging
import org.apache.predictionio.tools.{RunWorkflow, RunServer}
import org.apache.predictionio.tools.{DeployArgs, WorkflowArgs, SparkArgs, ServerArgs}
import org.apache.predictionio.tools.ReturnTypes._
import org.apache.predictionio.tools.Common._
import org.apache.predictionio.workflow.WorkflowUtils

import org.apache.commons.io.FileUtils
import org.json4s.native.Serialization.read
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.read
import org.json4s.native.Serialization.write
import scala.io.Source
import scala.util.Random
import scala.collection.JavaConversions._
import scala.sys.process._
import scalaj.http.Http
import java.io.File

case class BuildArgs(
  sbt: Option[File] = None,
  sbtExtra: Option[String] = None,
  sbtAssemblyPackageDependency: Boolean = true,
  sbtClean: Boolean = false,
  uberJar: Boolean = false,
  forceGeneratePIOSbt: Boolean = false)

case class EngineArgs(
  manifestJson: File = new File("manifest.json"),
  engineId: Option[String] = None,
  engineVersion: Option[String] = None)

object Engine extends EitherLogging {

  private val manifestAutogenTag = "pio-autogen-manifest"

  private def readManifestJson(json: File): Expected[EngineManifest] = {
    implicit val formats = Utils.json4sDefaultFormats +
      new EngineManifestSerializer
    try {
      Right(read[EngineManifest](Source.fromFile(json).mkString))
    } catch {
      case e: java.io.FileNotFoundException =>
        logAndFail(s"${json.getCanonicalPath} does not exist. Aborting.")
      case e: MappingException =>
        logAndFail(s"${json.getCanonicalPath} has invalid content: " +
          e.getMessage)
    }
  }

  private def withRegisteredManifest[T](ea: EngineArgs)(
      op: EngineManifest => Expected[T]): Expected[T] = {
    val res: Expected[Expected[T]] = for {
      ej <- readManifestJson(ea.manifestJson).right
      id <- Right(ea.engineId getOrElse ej.id).right
      version <- Right(ea.engineVersion getOrElse ej.version).right
      manifest <- storage.Storage.getMetaDataEngineManifests.get(id, version)
        .toRight {
          val errStr =
            s"""Engine ${id} ${version} cannot be found in the system.")
                |Possible reasons:
                |- the engine is not yet built by the 'build' command;
                |- the meta data store is offline."""
          error(errStr)
          errStr
        }.right
    } yield {
      op(manifest)
    }
    res.joinRight
  }

  private def generateManifestJson(json: File): MaybeError = {
    val cwd = sys.props("user.dir")
    implicit val formats = Utils.json4sDefaultFormats +
      new EngineManifestSerializer
    val rand = Random.alphanumeric.take(32).mkString
    val ha = java.security.MessageDigest.getInstance("SHA-1").
      digest(cwd.getBytes).map("%02x".format(_)).mkString
    val em = EngineManifest(
      id = rand,
      version = ha,
      name = new File(cwd).getName,
      description = Some(manifestAutogenTag),
      files = Seq(),
      engineFactory = "")
    try {
      FileUtils.writeStringToFile(json, write(em), "ISO-8859-1")
      Success
    } catch {
      case e: java.io.IOException =>
        logAndFail(s"Cannot generate ${json} automatically (${e.getMessage}). " +
          "Aborting.")
    }
  }

  private def regenerateManifestJson(json: File): MaybeError = {
    val cwd = sys.props("user.dir")
    val ha = java.security.MessageDigest.getInstance("SHA-1").
      digest(cwd.getBytes).map("%02x".format(_)).mkString
    if (json.exists) {
      readManifestJson(json).right.flatMap { em =>
        if (em.description == Some(manifestAutogenTag) && ha != em.version) {
          warn("This engine project directory contains an auto-generated " +
            "manifest that has been copied/moved from another location. ")
          warn("Regenerating the manifest to reflect the updated location. " +
            "This will dissociate with all previous engine instances.")
          generateManifestJson(json)
        } else {
          logAndSucceed(s"Using existing engine manifest JSON at " +
            "${json.getCanonicalPath}")
        }
      }
    } else {
      generateManifestJson(json)
    }
  }

  private def detectSbt(sbt: Option[File], pioHome: String): String = {
    sbt map {
      _.getCanonicalPath
    } getOrElse {
      val f = new File(Seq(pioHome, "sbt", "sbt").mkString(File.separator))
      if (f.exists) f.getCanonicalPath else "sbt"
    }
  }

  private def outputSbtError(line: String): Unit = {
    """\[.*error.*\]""".r findFirstIn line foreach { _ => error(line) }
  }

  private def compile(
    buildArgs: BuildArgs, pioHome: String, verbose: Boolean): MaybeError = {
    // only add pioVersion to sbt if project/pio.sbt exists
    if (new File("project", "pio-build.sbt").exists || buildArgs.forceGeneratePIOSbt) {
      FileUtils.writeLines(
        new File("pio.sbt"),
        Seq(
          "// Generated automatically by pio build.",
          "// Changes in this file will be overridden.",
          "",
          "pioVersion := \"" + BuildInfo.version + "\""))
    }
    implicit val formats = Utils.json4sDefaultFormats

    val sbt = detectSbt(buildArgs.sbt, pioHome)
    info(s"Using command '${sbt}' at the current working directory to build.")
    info("If the path above is incorrect, this process will fail.")
    val asm =
      if (buildArgs.sbtAssemblyPackageDependency) {
        " assemblyPackageDependency"
      } else {
        ""
      }
    val clean = if (buildArgs.sbtClean) " clean" else ""
    val buildCmd = s"${sbt} ${buildArgs.sbtExtra.getOrElse("")}${clean} " +
      (if (buildArgs.uberJar) "assembly" else s"package${asm}")
    val core = new File(s"pio-assembly-${BuildInfo.version}.jar")
    if (buildArgs.uberJar) {
      info(s"Uber JAR enabled. Putting ${core.getName} in lib.")
      val dst = new File("lib")
      dst.mkdir()
      coreAssembly(pioHome) match {
        case Right(coreFile) =>
          FileUtils.copyFileToDirectory(
            coreFile,
            dst,
            true)
        case Left(errStr) => return Left(errStr)
      }
    } else {
      if (new File("engine.json").exists()) {
        info(s"Uber JAR disabled. Making sure lib/${core.getName} is absent.")
        new File("lib", core.getName).delete()
      } else {
        info("Uber JAR disabled, but current working directory does not look " +
          s"like an engine project directory. Please delete lib/${core.getName} manually.")
      }
    }
    info(s"Going to run: ${buildCmd}")
    try {
      val r =
        if (verbose) {
          buildCmd.!(ProcessLogger(line => info(line), line => error(line)))
        } else {
          buildCmd.!(ProcessLogger(
            line => outputSbtError(line),
            line => outputSbtError(line)))
        }
      if (r != 0) {
        logAndFail(s"Return code of build command: ${buildCmd} is ${r}. Aborting.")
      } else {
        logAndSucceed("Compilation finished successfully.")
      }
    } catch {
      case e: java.io.IOException =>
        logAndFail(s"Exception during compilation: ${e.getMessage}")
    }
  }

  def build(
    buildArgs: BuildArgs,
    pioHome: String,
    manifestJson: File,
    verbose: Boolean): MaybeError = {

    regenerateManifestJson(manifestJson) match {
      case Left(err) => return Left(err)
      case _ => Unit
    }

    Template.verifyTemplateMinVersion(new File("template.json")) match {
      case Left(err) => return Left(err)
      case Right(_) =>
        compile(buildArgs, pioHome, verbose)
        info("Looking for an engine...")
        val jarFiles = jarFilesForScala
        if (jarFiles.isEmpty) {
          return logAndFail("No engine found. Your build might have failed. Aborting.")
        }
        jarFiles foreach { f => info(s"Found ${f.getName}")}
        RegisterEngine.registerEngine(
          manifestJson,
          jarFiles,
          false)
    }
  }

  /** Training an engine.
    *  The function starts a training process to bu run concurrenlty.
    *
    * @param ea An instance of [[EngineArgs]]
    * @param wa An instance of [[WorkflowArgs]] for running a single training.
    * @param sa An instance of [[SparkArgs]]
    * @param pioHome [[String]] with a path to PIO installation
    * @param verbose A [[Boolean]]
    * @return An instance of [[Expected]] contaning either [[Left]]
    *         with an error message or [[Right]] with a handle to a process
    *         responsible for training and a function () => Unit,
    *         that must be called when the process is complete
    */
  def train(
    ea: EngineArgs,
    wa: WorkflowArgs,
    sa: SparkArgs,
    pioHome: String,
    verbose: Boolean = false): Expected[(Process, () => Unit)] = {

    regenerateManifestJson(ea.manifestJson) match {
      case Left(err) => return Left(err)
      case _ => Unit
    }

    Template.verifyTemplateMinVersion(new File("template.json")).right.flatMap {
      _ =>
        withRegisteredManifest(ea) { em =>
            RunWorkflow.runWorkflow(wa, sa, em, pioHome, verbose)
          }
    }
  }

  /** Deploying an engine.
    *  The function starts a new process to be run concerrently.
    *
    * @param ea An instance of [[EngineArgs]]
    * @param engineInstanceId An instance of [[engineInstanceId]]
    * @param serverArgs An instance of [[ServerArgs]]
    * @param sparkArgs An instance of [[SparkArgs]]
    * @param pioHome [[String]] with a path to PIO installation
    * @param verbose A [[Boolean]]
    * @return An instance of [[Expected]] contaning either [[Left]]
    *         with an error message or [[Right]] with a handle to process
    *         of a running angine  and a function () => Unit,
    *         that must be called when the process is complete
    */
  def deploy(
    ea: EngineArgs,
    engineInstanceId: Option[String],
    serverArgs: ServerArgs,
    sparkArgs: SparkArgs,
    pioHome: String,
    verbose: Boolean = false): Expected[(Process, () => Unit)] = {

    val verifyResult = Template.verifyTemplateMinVersion(new File("template.json"))
    if (verifyResult.isLeft) {
      return Left(verifyResult.left.get)
    }
    withRegisteredManifest(ea) { em =>
      val variantJson = parse(Source.fromFile(serverArgs.variantJson).mkString)
      val variantId = variantJson \ "id" match {
        case JString(s) => s
        case _ =>
          return logAndFail("Unable to read engine variant ID from " +
            s"${serverArgs.variantJson.getCanonicalPath}. Aborting.")
      }
      val engineInstances = storage.Storage.getMetaDataEngineInstances
      val engineInstance = engineInstanceId map { eid =>
        engineInstances.get(eid)
      } getOrElse {
        engineInstances.getLatestCompleted(em.id, em.version, variantId)
      }
      engineInstance map { r =>
        RunServer.runServer(r.id, serverArgs, sparkArgs, em, pioHome, verbose)
      } getOrElse {
        engineInstanceId map { eid =>
          logAndFail(s"Invalid engine instance ID ${eid}. Aborting.")
        } getOrElse {
          logAndFail(s"No valid engine instance found for engine ${em.id} " +
            s"${em.version}.\nTry running 'train' before 'deploy'. Aborting.")
        }
      }
    }
  }

  def undeploy(da: DeployArgs): MaybeError = {

    val serverUrl = s"http://${da.ip}:${da.port}"
    info(
      s"Undeploying any existing engine instance at ${serverUrl}")
    try {
      val code = Http(s"${serverUrl}/stop").asString.code
      code match {
        case 200 => Success
        case 404 =>
          logAndFail(s"Another process is using ${serverUrl}. Unable to undeploy.")
        case _ =>
          logAndFail(s"Another process is using ${serverUrl}, or an existing " +
            s"engine server is not responding properly (HTTP ${code}). " +
            "Unable to undeploy.")
      }
    } catch {
      case e: java.net.ConnectException =>
        logAndFail(s"Nothing at ${serverUrl}")
      case _: Throwable =>
        logAndFail("Another process might be occupying " +
          s"${da.ip}:${da.port}. Unable to undeploy.")
    }
  }

  /** Running a driver on spark.
    *  The function starts a process and returns immediately
    *
    * @param mainClass A [[String]] with the class containing a main functionto run
    * @param driverArguments Arguments to be passed to the main function
    * @param manifestJson An instance of [[File]] for running a single training.
    * @param buildArgs An instance of [[BuildArgs]]
    * @param sparkArgs an instance of [[SparkArgs]]
    * @param pioHome [[String]] with a path to PIO installation
    * @param verbose A [[Boolean]]
    * @return An instance of [[Expected]] contaning either [[Left]]
    *         with an error message or [[Right]] with a handle to a process
    *         of a running driver
    */
  def run(
    mainClass: String,
    driverArguments: Seq[String],
    manifestJson: File,
    buildArgs: BuildArgs,
    sparkArgs: SparkArgs,
    pioHome: String,
    verbose: Boolean): Expected[Process] = {

    generateManifestJson(manifestJson) match {
      case Left(err) => return Left(err)
      case _ => Unit
    }

    compile(buildArgs, pioHome, verbose)

    val extraFiles = WorkflowUtils.thirdPartyConfFiles

    val jarFiles = jarFilesForScala
    jarFiles foreach { f => info(s"Found JAR: ${f.getName}") }
    val allJarFiles = jarFiles.map(_.getCanonicalPath)
    val cmd = s"${getSparkHome(sparkArgs.sparkHome)}/bin/spark-submit --jars " +
      s"${allJarFiles.mkString(",")} " +
      (if (extraFiles.size > 0) {
        s"--files ${extraFiles.mkString(",")} "
      } else {
        ""
      }) +
      "--class " +
      s"${mainClass} ${sparkArgs.sparkPassThrough.mkString(" ")} " +
      coreAssembly(pioHome) + " " +
      driverArguments.mkString(" ")
    info(s"Submission command: ${cmd}")
    Right(Process(
      cmd,
      None,
      "SPARK_YARN_USER_ENV" -> sys.env.filter(kv => kv._1.startsWith("PIO_")).
        map(kv => s"${kv._1}=${kv._2}").mkString(",")).run())
  }

  def unregister(jsonManifest: File): MaybeError = {
    RegisterEngine.unregisterEngine(jsonManifest)
  }

}

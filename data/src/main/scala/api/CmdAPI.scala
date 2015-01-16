package io.prediction.data.api

import java.io.File


import io.prediction.controller.Utils
import io.prediction.core.BuildInfo
import io.prediction.data.storage.App
import io.prediction.data.storage.AccessKey
import io.prediction.data.storage.EngineManifest
import io.prediction.data.storage.EngineManifestSerializer
import io.prediction.data.storage.Storage
import io.prediction.data.storage.hbase.upgrade.Upgrade_0_8_3
import io.prediction.data.storage.hbase.upgrade.CheckDistribution
import io.prediction.tools.dashboard.Dashboard
import io.prediction.tools.dashboard.DashboardConfig
import io.prediction.data.api.EventServer
import io.prediction.data.api.EventServerConfig
import io.prediction.data.api.RestServer
import io.prediction.data.api.RestServerConfig
import io.prediction.workflow.WorkflowUtils

import grizzled.slf4j.Logging
import org.apache.commons.io.FileUtils
import org.apache.hadoop.fs.Path
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.{read, write}
import scalaj.http.Http
import semverfi._

import scala.io.Source
import scala.sys.process._
import scala.util.Random

import java.io.File
import java.nio.file.Files

/**
 * Created by asylum on 1/16/15.
 */
object CmdClient extends Logging{

  val manifestAutogenTag = "pio-autogen-manifest"

  def regenerateManifestJson(json: File): Unit = {
    val cwd = sys.props("user.dir")
    val ha = java.security.MessageDigest.getInstance("SHA-1").
      digest(cwd.getBytes).map("%02x".format(_)).mkString
    if (json.exists) {
      val em = readManifestJson(json)
      if (em.description == Some(manifestAutogenTag) && ha != em.version) {
        warn("This engine project directory contains an auto-generated " +
          "manifest that has been copied/moved from another location. ")
        warn("Regenerating the manifest to reflect the updated location. " +
          "This will dissociate with all previous engine instances.")
        generateManifestJson(json)
      } else {
        info(s"Using existing engine manifest JSON at ${json.getCanonicalPath}")
      }
    } else {
      generateManifestJson(json)
    }
  }

  def generateManifestJson(json: File): Unit = {
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
    } catch {
      case e: java.io.IOException =>
        error(s"Cannot generate ${json} automatically (${e.getMessage}). " +
          "Aborting.")
        sys.exit(1)
    }
  }

  def readManifestJson(json: File): EngineManifest = {
    implicit val formats = Utils.json4sDefaultFormats +
      new EngineManifestSerializer
    try {
      read[EngineManifest](Source.fromFile(json).mkString)
    } catch {
      case e: java.io.FileNotFoundException =>
        error(s"${json.getCanonicalPath} does not exist. Aborting.")
        sys.exit(1)
      case e: MappingException =>
        error(s"${json.getCanonicalPath} has invalid content: " +
          e.getMessage)
        sys.exit(1)
    }
  }

  def build(): String = {
    ""
  }
}

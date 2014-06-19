package io.prediction.tools

import scala.io.Source

import grizzled.slf4j.Logging
import org.json4s._
import org.json4s.native.JsonMethods._

import io.prediction.storage.Config
import io.prediction.storage.EngineManifest

object RegisterEngine extends Logging {
  case class Args(jsonManifest: String = "")

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Args]("RegisterEngine") {
      arg[String]("<engine manifest JSON file>") action { (x, c) =>
        c.copy(jsonManifest = x)
      } text("the JSON file that contains the engine's manifest")
    }

    parser.parse(args, Args()) map { config =>
      implicit val formats = DefaultFormats
      val settingsConfig = new Config()
      val jsonString = Source.fromFile(config.jsonManifest).mkString
      val engineManifest = parse(jsonString).extract[EngineManifest]
      info(s"Registering engine ${engineManifest.id} ${engineManifest.version}")
      val engineManifests = settingsConfig.getSettingsEngineManifests
      engineManifests.update(engineManifest, true)
    }
  }
}

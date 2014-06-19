package io.prediction.tools

import io.prediction.storage.Config

import grizzled.slf4j.Logging

import scala.sys.process._

object RunEvaluationWorkflow extends Logging {
  def main(args: Array[String]): Unit = {
    case class Args(
      sparkSubmit: String = "",
      id: String = "",
      version: String = "",
      batch: String = "",
      dataPrepJsonPath: String = "",
      validatorJsonPath: String = "",
      cleanserJsonPath: String = "",
      algoJsonPath: String = "",
      serverJsonPath: String = "",
      jsonDir: String = "")

    val parser = new scopt.OptionParser[Args]("RunEvaluationWorkflow") {
      arg[String]("<path of spark-submit binary>") action { (x, c) =>
        c.copy(sparkSubmit = x)
      } text("path to the spark-submit binary")
      arg[String]("<engine id>") action { (x, c) =>
        c.copy(id = x)
      } text("the engine's ID")
      arg[String]("<engine version>") action { (x, c) =>
        c.copy(version = x)
      } text("the engine's version")
      opt[String]("batch") action { (x, c) =>
        c.copy(batch = x)
      } text("batch label of the run")
      opt[String]("dp").optional()
        .valueName("<dataprep param json>").action { (x,c) =>
          c.copy(dataPrepJsonPath = x)}
      opt[String]("vp").optional()
        .valueName("<validator param json>").action { (x,c) =>
          c.copy(validatorJsonPath = x)}
      opt[String]("cp").optional()
        .valueName("<cleanser param json>").action { (x,c) =>
          c.copy(cleanserJsonPath = x)}
      opt[String]("ap").optional()
        .valueName("<algo param json>").action { (x,c) =>
          c.copy(algoJsonPath = x) }
      opt[String]("sp").optional()
        .valueName("<server param json>").action { (x,c) =>
          c.copy(serverJsonPath = x) }
      opt[String]("jsonDir").optional()
        .valueName("<json directory>").action { (x,c) =>
          c.copy(jsonDir = x) }

    }

    parser.parse(args, Args()) map { parsedArgs =>
      val config = new Config
      val engineManifests = config.getSettingsEngineManifests
      engineManifests.get(parsedArgs.id, parsedArgs.version) map { engineManifest =>
        Seq(
          parsedArgs.sparkSubmit,
          "--verbose",
          "--master spark://localhost:7077",
          "--class io.prediction.tools.CreateEvaluationWorkflow",
          engineManifest.jar,
          "--engineManifestId",
          engineManifest.id,
          "--engineManifestVersion",
          engineManifest.version,
          "--evaluatorFactory",
          engineManifest.evaluatorFactory,
          "--engineFactory",
          engineManifest.engineFactory,
          if (parsedArgs.batch != "") "--batch " + parsedArgs.batch else "",
          if (parsedArgs.dataPrepJsonPath != "") "--dp " + parsedArgs.dataPrepJsonPath else "",
          if (parsedArgs.validatorJsonPath != "") "--vp " + parsedArgs.validatorJsonPath else "",
          if (parsedArgs.cleanserJsonPath != "") "--cp " + parsedArgs.cleanserJsonPath else "",
          if (parsedArgs.algoJsonPath != "") "--ap " + parsedArgs.algoJsonPath else "",
          if (parsedArgs.serverJsonPath != "") "--sp " + parsedArgs.serverJsonPath else "",
          if (parsedArgs.jsonDir != "") "--jsonDir " + parsedArgs.jsonDir else ""
        ).mkString(" ").!
      } getOrElse {
        error(s"Engine ${parsedArgs.id} ${parsedArgs.version} is not registered.")
      }
    }
  }
}

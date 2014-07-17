package io.prediction.tools

import grizzled.slf4j.Logging
import org.json4s._
import org.json4s.native.Serialization.{ read, write }

import scala.sys.process._

import java.io.File

import io.prediction.storage.Storage
import io.prediction.storage.Run

object RunEvaluationWorkflow extends Logging {
  def main(args: Array[String]): Unit = {
    case class Args(
      sparkHome: String = "",
      sparkMaster: String = "local",
      sparkDeployMode: String = "client",
      id: String = "",
      version: String = "",
      batch: String = "",
      dataPrepJsonPath: String = "dataPrepParams.json",
      validatorJsonPath: String = "validatorParams.json",
      cleanserJsonPath: String = "cleanserParams.json",
      algoJsonPath: String = "algoParams.json",
      serverJsonPath: String = "serverParams.json",
      jsonDir: String = ".")

    val parser = new scopt.OptionParser[Args]("RunEvaluationWorkflow") {
      arg[String]("<engine id>") action { (x, c) =>
        c.copy(id = x)
      } text("Engine ID.")
      arg[String]("<engine version>") action { (x, c) =>
        c.copy(version = x)
      } text("Engine version.")
      opt[String]("sparkHome") action { (x, c) =>
        c.copy(sparkHome = x)
      } text("Path to a Apache Spark installation. If not specified, will try to use the SPARK_HOME environmental variable. If this fails as well, default to current directory.")
      opt[String]("sparkMaster") action { (x, c) =>
        c.copy(sparkMaster = x)
      } text("Spark master URL. If not specified, default to local.")
      opt[String]("sparkDeployMode") action { (x, c) =>
        c.copy(sparkDeployMode = x)
      } text("Spark deploy mode. If not specified, default to client.")
      opt[String]("batch") action { (x, c) =>
        c.copy(batch = x)
      } text("Batch label of the run.")
      opt[String]("jsonDir") action { (x, c) =>
        c.copy(jsonDir = x)
      } text("Base directory of JSON files. Default: .")
      opt[String]("dp") action { (x, c) =>
        c.copy(dataPrepJsonPath = x)
      } text("Data preparator parameters file. Default: dataPrepParams.json")
      opt[String]("vp") action { (x, c) =>
        c.copy(validatorJsonPath = x)
      } text("Validator parameters file. Default: validatorParams.json")
      opt[String]("cp") action { (x, c) =>
        c.copy(cleanserJsonPath = x)
      } text("Cleanser parameters file. Default: cleanserParams.json")
      opt[String]("ap") action { (x, c) =>
        c.copy(algoJsonPath = x)
      } text("Algorithm parameters file. Default: algoParams.json")
      opt[String]("sp") action { (x, c) =>
        c.copy(serverJsonPath = x)
      } text("Server parameters file. Default: serverParams.json")
    }

    parser.parse(args, Args()) map { parsedArgs =>
      // Collect and serialize PIO_* environmental variables
      implicit val formats = DefaultFormats
      val pioEnvVars = sys.env.filter(kv => kv._1.startsWith("PIO_")).map(kv =>
        s"${kv._1}=${kv._2}"
      ).mkString(",")

      val engineManifests = Storage.getSettingsEngineManifests
      val defaults = Args()
      engineManifests.get(parsedArgs.id, parsedArgs.version) map { engineManifest =>
        val sparkHome = if (parsedArgs.sparkHome != "") parsedArgs.sparkHome else sys.env.get("SPARK_HOME").getOrElse(".")
        val params = Map(
          "dp" -> parsedArgs.dataPrepJsonPath,
          "vp" -> parsedArgs.validatorJsonPath,
          "cp" -> parsedArgs.cleanserJsonPath,
          "ap" -> parsedArgs.algoJsonPath,
          "sp" -> parsedArgs.serverJsonPath)
        val sparkSubmit = Seq(
          s"${sparkHome}/bin/spark-submit",
          "--verbose",
          "--deploy-mode",
          parsedArgs.sparkDeployMode,
          "--master",
          parsedArgs.sparkMaster,
          "--class",
          "io.prediction.tools.CreateEvaluationWorkflow") ++
          (if (engineManifest.files.size > 1) Seq("--jars", engineManifest.files.slice(1, engineManifest.files.size).mkString(",")) else Nil) ++ Seq(
          engineManifest.files.head,
          "--env",
          pioEnvVars,
          "--engineManifestId",
          engineManifest.id,
          "--engineManifestVersion",
          engineManifest.version,
          "--evaluatorFactory",
          engineManifest.evaluatorFactory,
          "--engineFactory",
          engineManifest.engineFactory) ++
          (if (parsedArgs.batch != "") Seq("--batch", parsedArgs.batch) else Nil) ++
          (if (params("dp") == defaults.dataPrepJsonPath && !(new File(withPath(params("dp"), parsedArgs.jsonDir))).exists) Nil else Seq("--dp", withPath(params("dp"), parsedArgs.jsonDir))) ++
          (if (params("vp") == defaults.validatorJsonPath && !(new File(withPath(params("vp"), parsedArgs.jsonDir))).exists) Nil else Seq("--vp", withPath(params("vp"), parsedArgs.jsonDir))) ++
          (if (params("cp") == defaults.cleanserJsonPath && !(new File(withPath(params("cp"), parsedArgs.jsonDir))).exists) Nil else Seq("--cp", withPath(params("cp"), parsedArgs.jsonDir))) ++
          (if (params("ap") == defaults.algoJsonPath && !(new File(withPath(params("ap"), parsedArgs.jsonDir))).exists) Nil else Seq("--ap", withPath(params("ap"), parsedArgs.jsonDir))) ++
          (if (params("sp") == defaults.serverJsonPath && !(new File(withPath(params("sp"), parsedArgs.jsonDir))).exists) Nil else Seq("--sp", withPath(params("sp"), parsedArgs.jsonDir)))
        if (parsedArgs.sparkDeployMode == "cluster")
          Process(sparkSubmit, None, "SPARK_YARN_USER_ENV" -> pioEnvVars).!
        else
          Process(sparkSubmit).!
      } getOrElse {
        error(s"Engine ${parsedArgs.id} ${parsedArgs.version} is not registered.")
      }
    }
  }

  private def withPath(file: String, path: String) = s"$path/$file"
}

package io.prediction.tools

import io.prediction.controller.Utils
import io.prediction.storage.EngineManifest
import io.prediction.storage.EngineManifestSerializer
import io.prediction.storage.Storage

import org.json4s._
import org.json4s.native.Serialization.{read, write}

import scala.io.Source
import scala.sys.process._

import java.io.File

case class ConsoleArgs(
  passThrough: Seq[String] = Seq(),
  pioHome: Option[String] = None,
  sparkHome: Option[String] = None,
  engineJson: File = new File("engine.json"),
  commands: Seq[String] = Seq(),
  engineId: Option[String] = None,
  engineVersion: Option[String] = None,
  batch: String = "Transient Lazy Val",
  metricsClass: Option[String] = None,
  dataSourceParamsJsonPath: Option[String] = None,
  preparatorParamsJsonPath: Option[String] = None,
  algorithmsParamsJsonPath: Option[String] = None,
  servingParamsJsonPath: Option[String] = None,
  metricsParamsJsonPath: Option[String] = None,
  paramsPath: String = ".")

object Console {
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[ConsoleArgs]("pio") {
      head("PredictionIO Command Line Interface Console\n")
      note("Note that it is possible to supply pass-through arguments at\n" +
        "the end of the command by using a '--' separator, e.g.\n\n" +
        "pio train --params-path params -- --master spark://mycluster:7077\n" +
        "\nIn the example above, the '--master' argument will be passed to\n" +
        "underlying spark-submit command. Please refer to the usage section\n" +
        "for each command for more information.\n\n" +
        "The following options are common to all commands:\n")
      opt[String]("pio-home") action { (x, c) =>
        c.copy(pioHome = Some(x))
      } text("Root directory of a PredictionIO installation.\n" +
        "        Specify this if automatic discovery fail.")
      opt[String]("spark-home") action { (x, c) =>
        c.copy(sparkHome = Some(x))
      } text("Root directory of an Apache Spark installation.\n" +
        "        If not specified, will try to use the SPARK_HOME\n" +
        "        environmental variable. If this fails as well, default to\n" +
        "        current directory.")
      opt[File]("engine-json") action { (x, c) =>
        c.copy(engineJson = x)
      } validate { x =>
        if (x.exists)
          success
        else
          failure(s"${x.getCanonicalPath} does not exist.")
      } text("Path to an engine JSON file. Default: engine.json")
      note("\n")
      cmd("register").
        text("Build and register an engine at the current directory.\n").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "register")
        }
      cmd("train").
        text("Kick off a training using an engine. This will produce an\n" +
          "engine instance. This command will pass all pass-through\n" +
          "arguments to its underlying spark-submit command.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "train")
        } children(
          opt[String]("batch") action { (x, c) =>
            c.copy(batch = x)
          } text("Batch label of the run."),
          opt[String]("params-path") action { (x, c) =>
            c.copy(paramsPath = x)
          } text("Directory to lookup parameters JSON files. Default: ."),
          opt[String]("datasource-params") abbr("dsp") action { (x, c) =>
            c.copy(dataSourceParamsJsonPath = Some(x))
          } text("Data source parameters JSON file. Will try to use\n" +
            "        datasource.json in the base path."),
          opt[String]("preparator-params") abbr("pp") action { (x, c) =>
            c.copy(preparatorParamsJsonPath = Some(x))
          } text("Preparator parameters JSON file. Will try to use\n" +
            "        preparator.json in the base path."),
          opt[String]("algorithms-params") abbr("ap") action { (x, c) =>
            c.copy(algorithmsParamsJsonPath = Some(x))
          } text("Algorithms parameters JSON file. Will try to use\n" +
            "        algorithms.json in the base path."),
          opt[String]("serving-params") abbr("sp") action { (x, c) =>
            c.copy(servingParamsJsonPath = Some(x))
          } text("Serving parameters JSON file. Will try to use\n" +
            "        serving.json in the base path."),
          opt[String]("metrics-params") abbr("mp") action { (x, c) =>
            c.copy(metricsParamsJsonPath = Some(x))
          } text("Metrics parameters JSON file. Will try to use\n" +
            "        metrics.json in the base path.")
        )
      /*
      cmd("metadata") text("commands to retrieve metadata\n") action { (_, c) =>
        c.copy(commands = c.commands :+ "metadata") } children(
        cmd("latest-completed-run") text("get the latest run ID given engine information") action { (_, c) =>
          c.copy(commands = c.commands :+ "latest-completed-run") } children(
          opt[String]("engine-id") required() action { (x, c) => c.copy(engineId = Some(x)) },
          opt[String]("engine-version") required() action { (x, c) => c.copy(engineVersion = Some(x)) }
        ),
        cmd("engine-files") text("get files required to run an engine") action { (_, c) =>
          c.copy(commands = c.commands :+ "engine-files") } children(
          opt[String]("engine-id") required() action { (x, c) => c.copy(engineId = Some(x)) },
          opt[String]("engine-version") required() action { (x, c) => c.copy(engineVersion = Some(x)) }
        )
      )
      */
    }

    val separatorIndex = args.indexWhere(_ == "--")
    val (consoleArgs, theRest) = args.splitAt(separatorIndex)
    val passThroughArgs = theRest.drop(1)

    parser.parse(consoleArgs, ConsoleArgs()) map { pca =>
      val ca = pca.copy(passThrough = passThroughArgs)
      ca.commands match {
        case Seq("register") =>
          register(ca)
        case Seq("train") =>
          train(ca)
        case Seq("metadata", "latest-completed-run") =>
          Storage.getMetaDataRuns.getLatestCompleted(ca.engineId.get, ca.engineVersion.get) map { run =>
            println(run.id)
          } getOrElse {
            System.err.println(s"No valid run found for ${ca.engineId.get} ${ca.engineVersion.get}!")
            sys.exit(1)
          }
        case Seq("metadata", "engine-files") =>
          Storage.getMetaDataEngineManifests.get(ca.engineId.get, ca.engineVersion.get).map { em =>
            println(em.files.mkString(" "))
          } getOrElse {
            System.err.println(s"Engine ${ca.engineId.get} ${ca.engineVersion.get} is not registered.")
            sys.exit(1)
          }
        case _ =>
          System.err.println(parser.usage)
          sys.exit(1)
      }
    }
    sys.exit(0)
  }

  def register(ca: ConsoleArgs): Unit = {
    // Detect sbt. Try to use PIO's sbt, then fall back to search path.
    val sbtAtHome = ca.pioHome map { pioHome =>
      val pioSbt = Seq(pioHome, "sbt", "sbt").mkString(File.separator)
      val pioSbtFile = new File(pioSbt)
      if (new File(pioSbt).canExecute)
        pioSbt
    }

    val sbt = sbtAtHome.getOrElse {
      if (new File("sbt").canExecute) {
        "sbt"
      } else {
        println("Failed to find executable sbt. Aborting.")
        sys.exit(1)
      }
    }

    println(s"Detected sbt at ${sbt}. Using it to build project.")
    s"${sbt} assemblyPackageDependency".!
    s"${sbt} package".!

    println("Build finished.")

    println("Locating files to be registered.")

    val jarFiles = jarFilesForScala
    jarFiles foreach { f => println(s"Found ${f.getName}")}

    RegisterEngine.registerEngine(ca.engineJson, jarFiles)
  }

  def train(ca: ConsoleArgs): Unit =
    RunWorkflow.runWorkflow(
      ca,
      coreAssembly(ca.pioHome.get),
      readEngineJson(ca.engineJson),
      jarFilesForScala)

  def coreAssembly(pioHome: String) = {
    val detectedCore =
      if (new File(pioHome + File.separator + "RELEASE").exists)
        jarFilesAt(new File(pioHome + File.separator + "lib"))
      else
        jarFilesAt(new File(pioHome + File.separator + "assembly"))
    if (detectedCore.size == 1) {
      detectedCore.head
    } else {
      println(s"More than one JAR found: ${detectedCore.mkString(", ")}")
      println("Please remove all JARs except the PredictionIO Core Assembly.")
      println("Aborting.")
      sys.exit(1)
    }
  }

  def readEngineJson(json: File): EngineManifest = {
    implicit val formats = Utils.json4sDefaultFormats +
      new EngineManifestSerializer
    try {
      read[EngineManifest](Source.fromFile(json).mkString)
    } catch {
      case e: java.io.FileNotFoundException =>
        println(s"${json.getCanonicalPath} does not exist. Aborting.")
        sys.exit(1)
      case e: MappingException =>
        println(s"${json.getCanonicalPath} has invalid content: " +
          e.getMessage)
        sys.exit(1)
    }
  }

  def jarFilesAt(path: File): Array[File] = recursiveListFiles(path) filter {
    _.getName.toLowerCase.endsWith(".jar")
  }

  def jarFilesForScala: Array[File] = jarFilesAt(new File("target"))

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }
}

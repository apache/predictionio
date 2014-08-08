package io.prediction.tools

import io.prediction.controller.Utils
import io.prediction.storage.EngineManifest
import io.prediction.storage.EngineManifestSerializer
import io.prediction.storage.Storage

import org.json4s._
import org.json4s.native.Serialization.{read, write}
import scalaj.http.Http

import scala.io.Source
import scala.sys.process._

import java.io.File

case class ConsoleArgs(
  passThrough: Seq[String] = Seq(),
  pioHome: Option[String] = None,
  sparkHome: Option[String] = None,
  engineJson: File = new File("engine.json"),
  sbt: Option[File] = None,
  commands: Seq[String] = Seq(),
  batch: String = "Transient Lazy Val",
  metricsClass: Option[String] = None,
  dataSourceParamsJsonPath: Option[String] = None,
  preparatorParamsJsonPath: Option[String] = None,
  algorithmsParamsJsonPath: Option[String] = None,
  servingParamsJsonPath: Option[String] = None,
  metricsParamsJsonPath: Option[String] = None,
  paramsPath: String = "params",
  engineInstanceId: Option[String] = None,
  ip: String = "localhost",
  port: Int = 8000)

object Console {
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[ConsoleArgs]("pio") {
      override def showUsageOnError = false
      head("PredictionIO Command Line Interface Console\n")
      help("help")
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
      opt[File]("sbt") action { (x, c) =>
        c.copy(sbt = Some(x))
      } validate { x =>
        if (x.exists)
          success
        else
          failure(s"${x.getCanonicalPath} does not exist.")
      } text("Path to sbt. Default: sbt")
      note("")
      cmd("register").
        text("Build and register an engine at the current directory.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "register")
        }
      note("")
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
          } text("Directory to lookup parameters JSON files. Default: params"),
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
      note("")
      cmd("eval").
        text("Kick off an evaluation using an engine. This will produce an\n" +
          "engine instance. This command will pass all pass-through\n" +
          "arguments to its underlying spark-submit command.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "eval")
        } children(
          opt[String]("batch") action { (x, c) =>
            c.copy(batch = x)
          } text("Batch label of the run."),
          opt[String]("params-path") action { (x, c) =>
            c.copy(paramsPath = x)
          } text("Directory to lookup parameters JSON files. Default: params"),
          opt[String]("metrics-class") required() action { (x, c) =>
            c.copy(metricsClass = Some(x))
          } text("Name of metrics class to run."),
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
      note("")
      cmd("deploy").
        text("Deploy an engine instance as a prediction server. This\n" +
          "command will pass all pass-through arguments to its underlying\n" +
          "spark-submit command.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "deploy")
        } children(
          opt[String]("engine-instance-id") action { (x, c) =>
            c.copy(engineInstanceId = Some(x))
          } text("Engine instance ID."),
          opt[String]("ip") action { (x, c) =>
            c.copy(ip = x)
          } text("IP to bind to. Default: localhost"),
          opt[Int]("port") action { (x, c) =>
            c.copy(port = x)
          } text("Port to bind to. Default: 8000")
        )
      note("")
      cmd("undeploy").
        text("Undeploy an engine instance as a prediction server.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "undeploy")
        } children(
          opt[String]("ip") action { (x, c) =>
            c.copy(ip = x)
          } text("IP to unbind from. Default: localhost"),
          opt[Int]("port") action { (x, c) =>
            c.copy(port = x)
          } text("Port to unbind from. Default: 8000")
        )
    }

    val separatorIndex = args.indexWhere(_ == "--")
    val (consoleArgs, theRest) =
      if (separatorIndex == -1)
        (args, Array[String]())
      else
        args.splitAt(separatorIndex)
    val passThroughArgs = theRest.drop(1)

    parser.parse(consoleArgs, ConsoleArgs()) map { pca =>
      val ca = pca.copy(passThrough = passThroughArgs)
      ca.commands match {
        case Seq("register") =>
          register(ca)
        case Seq("train") =>
          train(ca)
        case Seq("eval") =>
          train(ca)
        case Seq("deploy") =>
          deploy(ca)
        case Seq("undeploy") =>
          undeploy(ca)
        case _ =>
          System.err.println(
            s"Unrecognized command sequence: ${ca.commands.mkString(" ")}\n")
          System.err.println(parser.usage)
          sys.exit(1)
      }
    }
    sys.exit(0)
  }

  def register(ca: ConsoleArgs): Unit = {
    val sbt = ca.sbt map { _.getCanonicalPath } getOrElse { "sbt" }

    println(s"Using ${sbt} to build.")
    println("If the path above is incorrect, this process will fail.")
    val r1 = s"${sbt} assemblyPackageDependency".!
    if (r1 != 0) {
      println(s"Return code of previous step is ${r1}. Aborting.")
      sys.exit(1)
    }
    val r2 = s"${sbt} package".!
    if (r2 != 0) {
      println(s"Return code of previous step is ${r2}. Aborting.")
      sys.exit(1)
    }

    println("Build finished.")

    println("Locating files to be registered.")

    val jarFiles = jarFilesForScala
    if (jarFiles.size == 0) {
      println("No files can be found for registration. Aborting.")
      sys.exit(1)
    }
    jarFiles foreach { f => println(s"Found ${f.getName}")}

    RegisterEngine.registerEngine(ca.engineJson, jarFiles)
  }

  def train(ca: ConsoleArgs): Unit = {
    withRegisteredManifest(ca.engineJson) { em =>
      RunWorkflow.runWorkflow(
        ca,
        coreAssembly(ca.pioHome.get),
        em)
    }
  }

  def deploy(ca: ConsoleArgs): Unit = {
    withRegisteredManifest(ca.engineJson) { em =>
      val runs = Storage.getMetaDataRuns
      val run = ca.engineInstanceId map { eid =>
        runs.get(eid)
      } getOrElse {
        runs.getLatestCompleted(em.id, em.version)
      }
      run map { r =>
        undeploy(ca)
        RunServer.runServer(
          ca,
          coreAssembly(ca.pioHome.get),
          em,
          r.id)
      } getOrElse {
        ca.engineInstanceId map { eid =>
          println(
            s"Invalid engine instance ID ${ca.engineInstanceId}. Aborting.")
        } getOrElse {
          println(
            s"No valid engine instance found for engine ${em.id} " +
              s"${em.version}.\nTry running 'train' before 'deploy'. Aborting.")
        }
        sys.exit(1)
      }
    }
  }

  def undeploy(ca: ConsoleArgs): Unit = {
    val serverUrl = s"http://${ca.ip}:${ca.port}"
    println(
      s"Undeploying any existing engine instance at ${serverUrl}")
    try {
      Http(s"${serverUrl}/stop").asString
    } catch {
      case e: java.net.ConnectException =>
        println(s"Nothing at ${serverUrl}")
    }
  }

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

  def withRegisteredManifest(json: File)(op: EngineManifest => Unit): Unit = {
    val ej = readEngineJson(json)
    Storage.getMetaDataEngineManifests.get(ej.id, ej.version) map {
      op
    } getOrElse {
      println(s"Engine ${ej.id} ${ej.version} is not registered.")
      println("Have you run the 'register' command yet?")
      sys.exit(1)
    }
  }

  def jarFilesAt(path: File): Array[File] = recursiveListFiles(path) filter {
    _.getName.toLowerCase.endsWith(".jar")
  }

  def jarFilesForScala: Array[File] = jarFilesAt(new File("target"))

  def recursiveListFiles(f: File): Array[File] = {
    Option(f.listFiles) map { these =>
      these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
    } getOrElse Array[File]()
  }
}

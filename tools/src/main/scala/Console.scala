package io.prediction.tools

import io.prediction.BuildInfo
import io.prediction.controller.Utils
import io.prediction.storage.EngineManifest
import io.prediction.storage.EngineManifestSerializer
import io.prediction.storage.Storage
import io.prediction.tools.dashboard.Dashboard
import io.prediction.tools.dashboard.DashboardConfig

import grizzled.slf4j.Logging
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
  sbtExtra: Option[String] = None,
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
  port: Int = 8000,
  mainClass: Option[String] = None)

object Console extends Logging {
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[ConsoleArgs]("pio") {
      override def showUsageOnError = false
      head("PredictionIO Command Line Interface Console", BuildInfo.version)
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
      note("")
      cmd("dashboard").
        text("Launch a dashboard at the specific IP and port.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "dashboard")
        } children(
          opt[String]("ip") action { (x, c) =>
            c.copy(ip = x)
          } text("IP to bind to. Default: localhost"),
          opt[Int]("port") action { (x, c) =>
            c.copy(port = x)
          } text("Port to bind to. Default: 8000")
        )
      note("")
      cmd("run").
        text("Launch a driver program. This command will pass all\n" +
          "pass-through arguments to its underlying spark-submit command.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "run")
        } children(
          arg[String]("<main class>") action { (x, c) =>
            c.copy(mainClass = Some(x))
          } text("Main class name of the driver program."),
          opt[String]("sbtExtra") action { (x, c) =>
            c.copy(sbtExtra = Some(x))
          } text("Extra command to pass to SBT.")
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
        case Seq("dashboard") =>
          dashboard(ca)
        case Seq("run") =>
          run(ca)
        case _ =>
          error(
            s"Unrecognized command sequence: ${ca.commands.mkString(" ")}\n")
          System.err.println(parser.usage)
          sys.exit(1)
      }
    }
    sys.exit(0)
  }

  def register(ca: ConsoleArgs): Unit = {
    val sbt = detectSbt(ca)
    info(s"Using command '${sbt}' at the current working directory to build.")
    info("If the path above is incorrect, this process will fail.")

    val cmd = s"${sbt} package assemblyPackageDependency"
    info(s"Going to run: ${cmd}")
    val r = cmd.!(ProcessLogger(
      line => info(line), line => error(line)))
    if (r != 0) {
      error(s"Return code of previous step is ${r}. Aborting.")
      sys.exit(1)
    }
    info("Build finished successfully. Locating files to be registered.")

    val jarFiles = jarFilesForScala
    if (jarFiles.size == 0) {
      error("No files can be found for registration. Aborting.")
      sys.exit(1)
    }
    jarFiles foreach { f => info(s"Found ${f.getName}")}

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
      val engineInstances = Storage.getMetaDataEngineInstances
      val engineInstance = ca.engineInstanceId map { eid =>
        engineInstances.get(eid)
      } getOrElse {
        engineInstances.getLatestCompleted(em.id, em.version)
      }
      engineInstance map { r =>
        undeploy(ca)
        RunServer.runServer(
          ca,
          coreAssembly(ca.pioHome.get),
          em,
          r.id)
      } getOrElse {
        ca.engineInstanceId map { eid =>
          error(
            s"Invalid engine instance ID ${ca.engineInstanceId}. Aborting.")
        } getOrElse {
          error(
            s"No valid engine instance found for engine ${em.id} " +
              s"${em.version}.\nTry running 'train' before 'deploy'. Aborting.")
        }
        sys.exit(1)
      }
    }
  }

  def dashboard(ca: ConsoleArgs): Unit = {
    info(s"Creating dashboard at ${ca.ip}:${ca.port}")
    Dashboard.createDashboard(DashboardConfig(
      ip = ca.ip,
      port = ca.port))
  }

  def undeploy(ca: ConsoleArgs): Unit = {
    val serverUrl = s"http://${ca.ip}:${ca.port}"
    info(
      s"Undeploying any existing engine instance at ${serverUrl}")
    try {
      Http(s"${serverUrl}/stop").asString
    } catch {
      case e: java.net.ConnectException =>
        warn(s"Nothing at ${serverUrl}")
    }
  }

  def run(ca: ConsoleArgs): Unit = {
    if (!new File(ca.pioHome.get + File.separator + "RELEASE").exists) {
      info("Development tree detected. Building built-in engines.")

      val sbt = detectSbt(ca)
      info(s"Using command '${sbt}' at the ${ca.pioHome.get} to build.")
      info("If the path above is incorrect, this process will fail.")

      val cmd = Process(
        s"${sbt} ${ca.sbtExtra.getOrElse("")} engines/package",
        new File(ca.pioHome.get))
      info(s"Going to run: ${cmd}")
      try {
        val r = cmd.!(ProcessLogger(
          line => info(line), line => error(line)))
        if (r != 0) {
          error(s"Return code of previous step is ${r}. Aborting.")
          sys.exit(1)
        }
        info("Build finished successfully.")
      } catch {
        case e: java.io.IOException =>
          error(s"${e.getMessage}")
          sys.exit(1)
      }
    }

    val jarFiles = jarFilesForScala
    jarFiles foreach { f => info(s"Found JAR: ${f.getName}") }
    val allJarFiles = jarFiles ++ builtinEngines(ca.pioHome.get)
    val cmd = s"${getSparkHome(ca.sparkHome)}/bin/spark-submit --jars " +
      s"${allJarFiles.map(_.getCanonicalPath).mkString(",")} --class " +
      s"${ca.mainClass.get} ${coreAssembly(ca.pioHome.get)} " +
      ca.passThrough.mkString(" ")
    val r = cmd.!(ProcessLogger(
      line => info(line), line => error(line)))
    if (r != 0) {
      error(s"Return code of previous step is ${r}. Aborting.")
      sys.exit(1)
    }
  }

  def coreAssembly(pioHome: String): File = {
    val fn = s"tools-assembly-${BuildInfo.version}.jar"
    val core =
      if (new File(pioHome + File.separator + "RELEASE").exists)
        new File(Seq(pioHome, "lib", fn).mkString(File.separator))
      else
        new File(Seq(pioHome, "assembly", fn).mkString(File.separator))
    if (core.exists) {
      core
    } else {
      error(s"PredictionIO Core Assembly (${core.getCanonicalPath}) does not " +
        "exist. Aborting.")
      sys.exit(1)
    }
  }

  def builtinEngines(pioHome: String): Seq[File] = {
    val engine = s"engines_${scalaVersionNoPatch}-${BuildInfo.version}.jar"
    val engineDeps = s"engines-assembly-${BuildInfo.version}-deps.jar"
    val engineDir =
      if (new File(pioHome + File.separator + "RELEASE").exists)
        new File(pioHome + File.separator + "lib")
      else
        new File(Seq(
          pioHome,
          "engines",
          "target",
          s"scala-${scalaVersionNoPatch}").mkString(File.separator))
    val engineFiles = Seq(
      new File(engineDir, engine),
      new File(engineDir, engineDeps))
    val allPresent = !engineFiles.exists(!_.exists)
    if (allPresent) {
      engineFiles
    } else {
      engineFiles foreach { f =>
        if (!f.exists) error(s"${f.getCanonicalPath} does not exist.")
      }
      error(s"Built-in PredictionIO engine JAR file(s) listed above is " +
        "missing. Aborting.")
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
        error(s"${json.getCanonicalPath} does not exist. Aborting.")
        sys.exit(1)
      case e: MappingException =>
        error(s"${json.getCanonicalPath} has invalid content: " +
          e.getMessage)
        sys.exit(1)
    }
  }

  def withRegisteredManifest(json: File)(op: EngineManifest => Unit): Unit = {
    val ej = readEngineJson(json)
    Storage.getMetaDataEngineManifests.get(ej.id, ej.version) map {
      op
    } getOrElse {
      error(s"Engine ${ej.id} ${ej.version} is not registered.")
      error("Have you run the 'register' command yet?")
      sys.exit(1)
    }
  }

  def jarFilesAt(path: File): Array[File] = recursiveListFiles(path) filter {
    _.getName.toLowerCase.endsWith(".jar")
  }

  def jarFilesForScala: Array[File] = jarFilesAt(new File("target")).
    filterNot { f =>
      f.getName.toLowerCase.endsWith("-javadoc.jar") ||
      f.getName.toLowerCase.endsWith("-sources.jar")
    }

  def recursiveListFiles(f: File): Array[File] = {
    Option(f.listFiles) map { these =>
      these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
    } getOrElse Array[File]()
  }

  def getSparkHome(sparkHome: Option[String]): String = {
    sparkHome getOrElse {
      sys.env.get("SPARK_HOME").getOrElse(".")
    }
  }

  def versionNoPatch(fullVersion: String): String = {
    val v = """^(\d+\.\d+)""".r
    val versionNoPatch = for {
      v(np) <- v findFirstIn fullVersion
    } yield np
    versionNoPatch.getOrElse(fullVersion)
  }

  def scalaVersionNoPatch: String = versionNoPatch(BuildInfo.scalaVersion)

  def detectSbt(ca: ConsoleArgs): String = {
    ca.sbt map {
      _.getCanonicalPath
    } getOrElse {
      val f = new File(Seq(ca.pioHome.get, "sbt", "sbt").mkString(
        File.separator))
      if (f.exists) f.getCanonicalPath else "sbt"
    }
  }
}

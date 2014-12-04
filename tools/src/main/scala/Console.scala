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

import io.prediction.controller.Utils
import io.prediction.core.BuildInfo
import io.prediction.data.storage.App
import io.prediction.data.storage.AccessKey
import io.prediction.data.storage.EngineManifest
import io.prediction.data.storage.EngineManifestSerializer
import io.prediction.data.storage.Storage
import io.prediction.tools.dashboard.Dashboard
import io.prediction.tools.dashboard.DashboardConfig
import io.prediction.data.api.EventServer
import io.prediction.data.api.EventServerConfig
import io.prediction.workflow.WorkflowUtils

import grizzled.slf4j.Logging
import org.apache.commons.io.FileUtils
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

case class ConsoleArgs(
  common: CommonArgs = CommonArgs(),
  build: BuildArgs = BuildArgs(),
  app: AppArgs = AppArgs(),
  accessKey: AccessKeyArgs = AccessKeyArgs(),
  eventServer: EventServerArgs = EventServerArgs(),
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
  mainClass: Option[String] = None,
  projectName: Option[String] = None,
  directoryName: Option[String] = None)

case class CommonArgs(
  sparkPassThrough: Seq[String] = Seq(),
  driverPassThrough: Seq[String] = Seq(),
  pioHome: Option[String] = None,
  sparkHome: Option[String] = None,
  engineId: Option[String] = None,
  engineVersion: Option[String] = None,
  variantJson: File = new File("engine.json"),
  manifestJson: File = new File("manifest.json"),
  stopAfterRead: Boolean = false,
  stopAfterPrepare: Boolean = false,
  verbose: Boolean = false,
  debug: Boolean = false)

case class BuildArgs(
  sbt: Option[File] = None,
  sbtExtra: Option[String] = None,
  sbtAssemblyPackageDependency: Boolean = true,
  sbtClean: Boolean = false)

case class AppArgs(
  id: Option[Int] = None,
  name: String = "",
  description: Option[String] = None)

case class AccessKeyArgs(
  accessKey: String = "",
  events: Seq[String] = Seq())

case class EventServerArgs(
  enabled: Boolean = false,
  ip: String = "localhost",
  port: Int = 7070)

object Console extends Logging {
  val distFilename = "DIST"
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[ConsoleArgs]("pio") {
      override def showUsageOnError = false
      head("PredictionIO Command Line Interface Console", BuildInfo.version)
      help("")
      note("Note that it is possible to supply pass-through arguments at\n" +
        "the end of the command by using a '--' separator, e.g.\n\n" +
        "pio train --params-path params -- --master spark://mycluster:7077\n" +
        "\nIn the example above, the '--master' argument will be passed to\n" +
        "underlying spark-submit command. Please refer to the usage section\n" +
        "for each command for more information.\n\n" +
        "The following options are common to all commands:\n")
      opt[String]("pio-home") action { (x, c) =>
        c.copy(common = c.common.copy(pioHome = Some(x)))
      } text("Root directory of a PredictionIO installation.\n" +
        "        Specify this if automatic discovery fail.")
      opt[String]("spark-home") action { (x, c) =>
        c.copy(common = c.common.copy(sparkHome = Some(x)))
      } text("Root directory of an Apache Spark installation.\n" +
        "        If not specified, will try to use the SPARK_HOME\n" +
        "        environmental variable. If this fails as well, default to\n" +
        "        current directory.")
      opt[String]("engine-id") abbr("ei") action { (x, c) =>
        c.copy(common = c.common.copy(engineId = Some(x)))
      } text("Specify an engine ID. Usually used by distributed deployment.")
      opt[String]("engine-version") abbr("ev") action { (x, c) =>
        c.copy(common = c.common.copy(engineVersion = Some(x)))
      } text("Specify an engine version. Usually used by distributed " +
        "deployment.")
      opt[File]("variant") abbr("v") action { (x, c) =>
        c.copy(common = c.common.copy(variantJson = x))
      } validate { x =>
        if (x.exists)
          success
        else
          failure(s"${x.getCanonicalPath} does not exist.")
      } text("Path to an engine variant JSON file. Default: engine.json")
      opt[File]("manifest") abbr("m") action { (x, c) =>
        c.copy(common = c.common.copy(manifestJson = x))
      } validate { x =>
        if (x.exists)
          success
        else
          failure(s"${x.getCanonicalPath} does not exist.")
      } text("Path to an engine manifest JSON file. Default: manifest.json")
      opt[File]("sbt") action { (x, c) =>
        c.copy(build = c.build.copy(sbt = Some(x)))
      } validate { x =>
        if (x.exists)
          success
        else
          failure(s"${x.getCanonicalPath} does not exist.")
      } text("Path to sbt. Default: sbt")
      opt[Unit]("verbose") action { (x, c) =>
        c.copy(common = c.common.copy(verbose = true))
      }
      opt[Unit]("debug") action { (x, c) =>
        c.copy(common = c.common.copy(debug = true))
      }
      note("")
      cmd("version").
        text("Displays the version of this command line console.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "version")
        }
      note("")
      cmd("help").action { (_, c) =>
        c.copy(commands = c.commands :+ "help")
      } children(
        arg[String]("<command>") optional()
          action { (x, c) =>
            c.copy(commands = c.commands :+ x)
          }
        )
      note("")
      cmd("new").
        text("Creates a new engine project in a subdirectory with the same " +
          "name as the project. The project name will also be used as the " +
          "default engine ID.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "new")
        } children(
          arg[String]("<project name>") action { (x, c) =>
            c.copy(projectName = Some(x))
          } text("Engine project name.")
        )
      //note("")
      //cmd("instance").
      //  text("Creates a new engine instance in a subdirectory with the same " +
      //    "name as the engine's ID by default.").
      //  action { (_, c) =>
      //    c.copy(commands = c.commands :+ "instance")
      //  } children(
      //    arg[String]("<engine ID>") action { (x, c) =>
      //      c.copy(projectName = Some(x))
      //    } text("Engine ID."),
      //    opt[String]("directory-name") action { (x, c) =>
      //      c.copy(directoryName = Some(x))
      //    } text("Engine instance directory name.")
      //  )
      note("")
      cmd("build").
        text("Build an engine at the current directory.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "build")
        } children(
          opt[String]("sbt-extra") action { (x, c) =>
            c.copy(build = c.build.copy(sbtExtra = Some(x)))
          } text("Extra command to pass to SBT when it builds your engine."),
          opt[Unit]("clean") action { (x, c) =>
            c.copy(build = c.build.copy(sbtClean = true))
          } text("Clean build."),
          opt[Unit]("no-asm") action { (x, c) =>
            c.copy(build = c.build.copy(sbtAssemblyPackageDependency = false))
          } text("Skip building external dependencies assembly.")
        )
      //note("")
      //cmd("register").
      //  text("Build and register an engine at the current directory.\n" +
      //    "If the engine at the current directory is a PredictionIO\n" +
      //    "built-in engine that is not part of PredictionIO's source tree,\n" +
      //    "the build step will be skipped.").
      //  action { (_, c) =>
      //    c.copy(commands = c.commands :+ "register")
      //  } children(
      //    opt[String]("sbt-extra") action { (x, c) =>
      //      c.copy(build = c.build.copy(sbtExtra = Some(x)))
      //    } text("Extra command to pass to SBT when it builds your engine."),
      //    opt[Unit]("clean") action { (x, c) =>
      //      c.copy(build = c.build.copy(sbtClean = true))
      //    } text("Clean build."),
      //    opt[Unit]("asm") action { (x, c) =>
      //      c.copy(build = c.build.copy(sbtAssemblyPackageDependency = true))
      //    } text("Build dependencies assembly.")
      //  )
      note("")
      cmd("unregister").
        text("Unregister an engine at the current directory.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "unregister")
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
            "        metrics.json in the base path."),
          opt[Unit]("stop-after-read") abbr("sar") action { (x, c) =>
            c.copy(common = c.common.copy(stopAfterRead = true))
          },
          opt[Unit]("stop-after-prepare") abbr("sap") action { (x, c) =>
            c.copy(common = c.common.copy(stopAfterPrepare = true))
          }
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
          } text("Port to bind to. Default: 8000"),
          opt[Unit]("feedback") action { (_, c) =>
            c.copy(eventServer = c.eventServer.copy(enabled = true))
          } text("Enable feedback loop to event server."),
          opt[String]("event-server-ip") action { (x, c) =>
            c.copy(eventServer = c.eventServer.copy(ip = x))
          } text("Event server IP. Default: localhost"),
          opt[Int]("event-server-port") action { (x, c) =>
            c.copy(eventServer = c.eventServer.copy(port = x))
          } text("Event server port. Default: 7070"),
          opt[String]("accesskey") action { (x, c) =>
            c.copy(accessKey = c.accessKey.copy(accessKey = x))
          } text("Access key of the App where feedback data will be stored.")
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
          c.copy(
            commands = c.commands :+ "dashboard",
            port = 9000)
        } children(
          opt[String]("ip") action { (x, c) =>
            c.copy(ip = x)
          } text("IP to bind to. Default: localhost"),
          opt[Int]("port") action { (x, c) =>
            c.copy(port = x)
          } text("Port to bind to. Default: 9000")
        )
      note("")
      cmd("eventserver").
        text("Launch an Event Server at the specific IP and port.").
        action { (_, c) =>
          c.copy(
            commands = c.commands :+ "eventserver",
            port = 7070)
        } children(
          opt[String]("ip") action { (x, c) =>
            c.copy(ip = x)
          } text("IP to bind to. Default: localhost"),
          opt[Int]("port") action { (x, c) =>
            c.copy(port = x)
          } text("Port to bind to. Default: 7070")
        )
      //note("")
      //cmd("compile").
      //  text("Compile a driver program.").
      //  action { (_, c) =>
      //    c.copy(commands = c.commands :+ "compile")
      //  } children(
      //    opt[String]("sbt-extra") action { (x, c) =>
      //      c.copy(build = c.build.copy(sbtExtra = Some(x)))
      //    } text("Extra command to pass to SBT when it builds your engine."),
      //    opt[Unit]("clean") action { (x, c) =>
      //      c.copy(build = c.build.copy(sbtClean = true))
      //    } text("Clean build."),
      //    opt[Unit]("asm") action { (x, c) =>
      //      c.copy(build = c.build.copy(sbtAssemblyPackageDependency = true))
      //    } text("Build dependencies assembly.")
      //  )
      note("")
      cmd("run").
        text("Launch a driver program. This command will pass all\n" +
          "pass-through arguments to its underlying spark-submit command.\n" +
          "In addition, it also supports a second level of pass-through\n" +
          "arguments to the driver program, e.g.\n" +
          "pio run -- --master spark://localhost:7077 -- --driver-arg foo").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "run")
        } children(
          arg[String]("<main class>") action { (x, c) =>
            c.copy(mainClass = Some(x))
          } text("Main class name of the driver program."),
          opt[String]("sbt-extra") action { (x, c) =>
            c.copy(build = c.build.copy(sbtExtra = Some(x)))
          } text("Extra command to pass to SBT when it builds your engine."),
          opt[Unit]("clean") action { (x, c) =>
            c.copy(build = c.build.copy(sbtClean = true))
          } text("Clean build."),
          opt[Unit]("no-asm") action { (x, c) =>
            c.copy(build = c.build.copy(sbtAssemblyPackageDependency = false))
          } text("Skip building external dependencies assembly.")
        )
      note("")
      cmd("status").
        text("Displays status information about the PredictionIO system.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "status")
        }
      //note("")
      //cmd("dist").
      //  text("Build an engine at the current directory and create a \n" +
      //    "distributable package.\n" +
      //    "If the engine at the current directory is a PredictionIO\n" +
      //    "built-in engine that is not part of PredictionIO's source tree,\n" +
      //    "the build step will be skipped.").
      //  action { (_, c) =>
      //    c.copy(commands = c.commands :+ "dist")
      //  } children(
      //    opt[String]("sbt-extra") action { (x, c) =>
      //      c.copy(build = c.build.copy(sbtExtra = Some(x)))
      //    } text("Extra command to pass to SBT when it builds your engine."),
      //    opt[Unit]("clean") action { (x, c) =>
      //      c.copy(build = c.build.copy(sbtClean = true))
      //    } text("Clean build."),
      //    opt[Unit]("asm") action { (x, c) =>
      //      c.copy(build = c.build.copy(sbtAssemblyPackageDependency = true))
      //    } text("Build dependencies assembly.")
      //  )
      note("")
      cmd("app").
        text("Manage apps.\n").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "app")
        } children(
          cmd("new").
            text("Create a new app key to app ID mapping.").
            action { (_, c) =>
              c.copy(commands = c.commands :+ "new")
            } children(
              opt[Int]("id") action { (x, c) =>
                c.copy(app = c.app.copy(id = Some(x)))
              } text("Specify this if you already have data under an app ID."),
              opt[String]("description") action { (x, c) =>
                c.copy(app = c.app.copy(description = Some(x)))
              } text("Specify this if you already have data under an app ID."),
              arg[String]("<name>") action { (x, c) =>
                c.copy(app = c.app.copy(name = x))
              } text("App name.")
            ),
          note(""),
          cmd("list").
            text("List all apps.").
            action { (_, c) =>
              c.copy(commands = c.commands :+ "list")
            },
          note(""),
          cmd("delete").
            text("Delete an app.").
            action { (_, c) =>
              c.copy(commands = c.commands :+ "delete")
            } children(
              arg[String]("<name>") action { (x, c) =>
                c.copy(app = c.app.copy(name = x))
              } text("Name of the app to be deleted.")
            ),
          note(""),
          cmd("data-delete").
            text("Delete data of an app").
            action { (_, c) =>
              c.copy(commands = c.commands :+ "data-delete")
            } children(
              arg[String]("<name>") action { (x, c) =>
                c.copy(app = c.app.copy(name = x))
              } text("Name of the app whose data to be deleted.")
            )
        )
      note("")
      cmd("accesskey").
        text("Manage app access keys.\n").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "accesskey")
        } children(
          cmd("new").
            text("Add allowed event(s) to an access key.").
            action { (_, c) =>
              c.copy(commands = c.commands :+ "new")
            } children(
              arg[String]("<app name>") action { (x, c) =>
                c.copy(app = c.app.copy(name = x))
              } text("App to be associated with the new access key."),
              arg[String]("[<event1> <event2> ...]") unbounded() optional()
                action { (x, c) =>
                  c.copy(accessKey = c.accessKey.copy(
                    events = c.accessKey.events :+ x))
                } text("Allowed event name(s) to be added to the access key.")
            ),
          cmd("list").
            text("List all access keys of an app.").
            action { (_, c) =>
              c.copy(commands = c.commands :+ "list")
            } children(
              arg[String]("<app name>") action { (x, c) =>
                c.copy(app = c.app.copy(name = x))
              } text("App name.")
            ),
          note(""),
          cmd("delete").
            text("Delete an access key.").
            action { (_, c) =>
              c.copy(commands = c.commands :+ "delete")
            } children(
              arg[String]("<access key>") action { (x, c) =>
                c.copy(accessKey = c.accessKey.copy(accessKey = x))
              } text("The access key to be deleted.")
            )
        )
    }

    val separatorIndex = args.indexWhere(_ == "--")
    val (consoleArgs, theRest) =
      if (separatorIndex == -1)
        (args, Array[String]())
      else
        args.splitAt(separatorIndex)
    val allPassThroughArgs = theRest.drop(1)
    val secondSepIdx = allPassThroughArgs.indexWhere(_ == "--")
    val (sparkPassThroughArgs, driverPassThroughArgs) =
      if (secondSepIdx == -1)
        (allPassThroughArgs, Array[String]())
      else {
        val t = allPassThroughArgs.splitAt(secondSepIdx)
        (t._1, t._2.drop(1))
      }

    parser.parse(consoleArgs, ConsoleArgs()) map { pca =>
      val ca = pca.copy(common = pca.common.copy(
        sparkPassThrough = sparkPassThroughArgs,
        driverPassThrough = driverPassThroughArgs))
      ca.commands match {
        case Seq("") =>
          System.err.println(help())
          sys.exit(1)
        case Seq("version") =>
          version(ca)
        case Seq("new") =>
          createProject(ca)
        //case Seq("instance") =>
        //  createInstance(ca)
        case Seq("build") =>
          regenerateManifestJson(ca.common.manifestJson)
          build(ca)
        case Seq("register") =>
          register(ca)
        case Seq("unregister") =>
          unregister(ca)
        case Seq("train") =>
          regenerateManifestJson(ca.common.manifestJson)
          train(ca)
        case Seq("eval") =>
          regenerateManifestJson(ca.common.manifestJson)
          train(ca)
        case Seq("deploy") =>
          deploy(ca)
        case Seq("undeploy") =>
          undeploy(ca)
        case Seq("dashboard") =>
          dashboard(ca)
        case Seq("eventserver") =>
          eventserver(ca)
        case Seq("compile") =>
          generateManifestJson(ca.common.manifestJson)
          compile(ca)
        case Seq("run") =>
          generateManifestJson(ca.common.manifestJson)
          run(ca)
        case Seq("dist") =>
          dist(ca)
        case Seq("status") =>
          status(ca)
        case Seq("app", "new") =>
          appNew(ca)
        case Seq("app", "list") =>
          appList(ca)
        case Seq("app", "delete") =>
          appDelete(ca)
        case Seq("app", "data-delete") =>
          appDataDelete(ca)
        case Seq("accesskey", "new") =>
          accessKeyNew(ca)
        case Seq("accesskey", "list") =>
          accessKeyList(ca)
        case Seq("accesskey", "delete") =>
          accessKeyDelete(ca)
        case _ =>
          System.err.println(help(ca.commands))
          sys.exit(1)
      }
      sys.exit(0)
    } getOrElse {
      val command = args.toSeq.filterNot(_.startsWith("--")).head
      System.err.println(help(Seq(command)))
      sys.exit(1)
    }
  }

  def help(commands: Seq[String] = Seq()) = {
    if (commands.isEmpty) {
      mainHelp
    } else {
      val stripped =
        (if (commands.head == "help") commands.drop(1) else commands).
          mkString("-")
      helpText.getOrElse(stripped, s"Help is unavailable for ${stripped}.")
    }
  }

  val mainHelp = console.txt.main().toString

  val helpText = Map(
    "" -> mainHelp,
    "status" -> console.txt.status().toString,
    "version" -> console.txt.version().toString,
    "new" -> console.txt.newCommand().toString,
    "build" -> console.txt.build().toString,
    "train" -> console.txt.train().toString,
    "deploy" -> console.txt.deploy().toString,
    "eventserver" -> console.txt.eventserver().toString,
    "app" -> console.txt.app().toString,
    "accesskey" -> console.txt.accesskey().toString,
    "run" -> console.txt.run().toString,
    "eval" -> console.txt.eval().toString,
    "dashboard" -> console.txt.dashboard().toString)

  def createProject(ca: ConsoleArgs): Unit = {
    val scalaEngineTemplate = Map(
      "build.sbt" -> templates.scala.txt.buildSbt(
        ca.projectName.get,
        BuildInfo.version,
        BuildInfo.sparkVersion),
      "engine.json" -> templates.scala.txt.engineJson(
        ca.projectName.get,
        "myorg.MyEngineFactory"),
      "manifest.json" -> templates.scala.txt.manifestJson(
        ca.projectName.get,
        "0.0.1-SNAPSHOT",
        ca.projectName.get),
      joinFile(Seq("project", "assembly.sbt")) ->
        templates.scala.project.txt.assemblySbt(),
      joinFile(Seq("src", "main", "scala", "Engine.scala")) ->
        templates.scala.src.main.scala.txt.engine())

    val template = ca.projectName.get match {
      case _ =>
        info(s"Creating Scala engine project ${ca.projectName.get}")
        scalaEngineTemplate
    }

    writeTemplate(template, ca.projectName.get)

    info(s"Engine project created in subdirectory ${ca.projectName.get}.")
  }

  /*
  def createInstance(ca: ConsoleArgs): Unit = {
    val targetDir = ca.directoryName.getOrElse(ca.projectName.get)
    val engineId = ca.projectName.getOrElse("")

    val templateOpt = BuiltInEngine.idInstanceMap.get(engineId)

    if (templateOpt.isEmpty) {
      val engineIdList = BuiltInEngine.instances
        .zipWithIndex
        .map { case(eit, idx) => s"  ${eit.engineId}" }
        .mkString("\n")
      error(s"${engineId} is not a built-in engine. \n" +
        s"Below are built-in engines: \n$engineIdList\n" +
        s"Aborting.")
      sys.exit(1)
    }

    writeTemplate(templateOpt.get.template, targetDir)

    info(s"Engine instance created in subdirectory ${targetDir}.")
  }
  */

  private def writeTemplate(template: Map[String, Any], targetDir: String) = {
    try {
      template map { ft =>
        FileUtils.writeStringToFile(
          new File(targetDir, ft._1),
          ft._2.toString,
          "ISO-8859-1")
      }
    } catch {
      case e: java.io.IOException =>
        error(s"Error occurred while generating template: ${e.getMessage}")
        error("Aborting.")
        sys.exit(1)
    }
  }

  def version(ca: ConsoleArgs): Unit = println(BuildInfo.version)

  def build(ca: ConsoleArgs): Unit = {
    compile(ca)
    info("Looking for an engine...")
    val jarFiles = jarFilesForScala
    if (jarFiles.size == 0) {
      error("No engine found. Your build might have failed. Aborting.")
      sys.exit(1)
    }
    jarFiles foreach { f => info(s"Found ${f.getName}")}
    val copyLocal = if (sys.env.contains("HADOOP_CONF_DIR")) {
      info("HADOOP_CONF_DIR is set. Assuming HDFS is available.")
      true
    } else {
      info("HADOOP_CONF_DIR is not set. Assuming HDFS is unavailable.")
      false
    }
    RegisterEngine.registerEngine(ca.common.manifestJson, jarFiles, copyLocal)
    info("Your engine is ready for training.")
  }

  def register(ca: ConsoleArgs): Unit = {
    if (builtinEngineDir ||
      !RegisterEngine.builtinEngine(ca.common.manifestJson)) {
      if (!distEngineDir) compile(ca)
      info("Locating files to be registered.")
      val jarFiles = jarFilesForScala
      if (jarFiles.size == 0) {
        error("No files can be found for registration. Aborting.")
        sys.exit(1)
      }
      jarFiles foreach { f => info(s"Found ${f.getName}")}
      RegisterEngine.registerEngine(ca.common.manifestJson, jarFiles)
    } else {
      info("Registering a built-in engine.")
      RegisterEngine.registerEngine(
        ca.common.manifestJson,
        builtinEngines(ca.common.pioHome.get))
    }
  }

  def unregister(ca: ConsoleArgs): Unit = {
    RegisterEngine.unregisterEngine(ca.common.manifestJson)
  }

  def train(ca: ConsoleArgs): Unit = {
    withRegisteredManifest(
      ca.common.manifestJson,
      ca.common.engineId,
      ca.common.engineVersion) { em =>
      RunWorkflow.runWorkflow(
        ca,
        coreAssembly(ca.common.pioHome.get),
        em,
        ca.common.variantJson)
    }
  }

  def deploy(ca: ConsoleArgs): Unit = {
    withRegisteredManifest(
      ca.common.manifestJson,
      ca.common.engineId,
      ca.common.engineVersion) { em =>
      val variantJson = parse(Source.fromFile(ca.common.variantJson).mkString)
      val variantId = variantJson \ "id" match {
        case JString(s) => s
        case _ =>
          error("Unable to read engine variant ID from " +
            s"${ca.common.variantJson.getCanonicalPath}. Aborting.")
          sys.exit(1)
      }
      val engineInstances = Storage.getMetaDataEngineInstances
      val engineInstance = ca.engineInstanceId map { eid =>
        engineInstances.get(eid)
      } getOrElse {
        engineInstances.getLatestCompleted(em.id, em.version, variantId)
      }
      engineInstance map { r =>
        undeploy(ca)
        RunServer.runServer(
          ca,
          coreAssembly(ca.common.pioHome.get),
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

  def eventserver(ca: ConsoleArgs): Unit = {
    info(s"Creating Event Server at ${ca.ip}:${ca.port}")
    EventServer.createEventServer(EventServerConfig(
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
      case e: scalaj.http.HttpException => e.code match {
        case 404 =>
          error(s"Another process is using ${serverUrl}. Aborting.")
          sys.exit(1)
      }
      case e: java.net.ConnectException =>
        warn(s"Nothing at ${serverUrl}")
    }
  }

  def compile(ca: ConsoleArgs): Unit = {
    if (!new File(ca.common.pioHome.get + File.separator + "RELEASE").exists) {
      info("Development tree detected. Building built-in engines.")

      val sbt = detectSbt(ca)
      info(s"Using command '${sbt}' at ${ca.common.pioHome.get} to build.")
      info("If the path above is incorrect, this process will fail.")

      val asm =
        if (ca.build.sbtAssemblyPackageDependency)
          " engines/assemblyPackageDependency"
        else
          ""
      val clean =
        if (ca.build.sbtClean)
          " engines/clean"
        else
          ""
      val cmd = Process(
        s"${sbt}${clean} engines/publishLocal${asm}",
        new File(ca.common.pioHome.get))
      info(s"Going to run: ${cmd}")
      try {
        val r =
          if (ca.common.verbose || ca.common.debug)
            cmd.!(ProcessLogger(line => info(line), line => error(line)))
          else
            cmd.!(ProcessLogger(
              line => outputSbtError(line),
              line => outputSbtError(line)))
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

    if (builtinEngineDir) {
      info("Current working directory is a PredictionIO built-in engines " +
        "project. Skipping redundant packaging step.")
    } else {
      val sbt = detectSbt(ca)
      info(s"Using command '${sbt}' at the current working directory to build.")
      info("If the path above is incorrect, this process will fail.")
      val asm =
        if (ca.build.sbtAssemblyPackageDependency)
          " assemblyPackageDependency"
        else
          ""
      val clean = if (ca.build.sbtClean) " clean" else ""
      val buildCmd = s"${sbt} ${ca.build.sbtExtra.getOrElse("")}${clean} " +
        s"package${asm}"
      info(s"Going to run: ${buildCmd}")
      try {
        val r =
          if (ca.common.verbose || ca.common.debug)
          buildCmd.!(ProcessLogger(line => info(line), line => error(line)))
          else
          buildCmd.!(ProcessLogger(
            line => outputSbtError(line),
            line => outputSbtError(line)))
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
  }

  private def outputSbtError(line: String): Unit = {
    """\[.*error.*\]""".r findFirstIn line foreach { _ => error(line) }
  }

  def run(ca: ConsoleArgs): Unit = {
    compile(ca)

    val extraFiles = WorkflowUtils.hadoopEcoConfFiles

    val jarFiles = jarFilesForScala
    jarFiles foreach { f => info(s"Found JAR: ${f.getName}") }
    val allJarFiles = (jarFiles ++
      builtinEngines(ca.common.pioHome.get)).map(_.getCanonicalPath)
    val cmd = s"${getSparkHome(ca.common.sparkHome)}/bin/spark-submit --jars " +
      s"${allJarFiles.mkString(",")} " + (if (extraFiles.size > 0)
        s"--files ${extraFiles.mkString(",")} " else "") +
      "--class " +
      s"${ca.mainClass.get} ${ca.common.sparkPassThrough.mkString(" ")} " +
      coreAssembly(ca.common.pioHome.get) + " " +
      ca.common.driverPassThrough.mkString(" ")
    val proc = Process(
      cmd,
      None,
      "SPARK_YARN_USER_ENV" -> sys.env.filter(kv => kv._1.startsWith("PIO_")).
        map(kv => s"${kv._1}=${kv._2}").mkString(","))
    info(s"Submission command: ${cmd}")
    val r = proc.!
    if (r != 0) {
      error(s"Return code of previous step is ${r}. Aborting.")
      sys.exit(1)
    }
  }

  def dist(ca: ConsoleArgs): Unit = {
    if (builtinEngineDir ||
      !RegisterEngine.builtinEngine(ca.common.manifestJson)) {
      compile(ca)
    }
    info("Locating files to be distributed.")
    val jarFiles = jarFilesForScala
    if (jarFiles.size == 0) {
      error("No files can be found for distribution. Aborting.")
      sys.exit(1)
    }
    val distDir = new File("dist")
    val libDir = new File(distDir, "lib")
    libDir.mkdirs
    jarFiles foreach { f =>
      info(s"Found ${f.getName}")
      FileUtils.copyFile(f, new File(libDir, f.getName))
    }
    val variantJson = "engine.json"
    FileUtils.copyFile(new File(variantJson), new File(distDir, variantJson))
    val paramsDir = new File("params")
    if (paramsDir.exists)
      FileUtils.copyDirectory(paramsDir, new File(distDir, paramsDir.getName))
    Files.createFile(distDir.toPath.resolve(distFilename))
    info(s"Successfully created distributable at: ${distDir.getCanonicalPath}")
  }

  def appNew(ca: ConsoleArgs): Unit = {
    val apps = Storage.getMetaDataApps
    apps.getByName(ca.app.name) map { app =>
      error(s"App ${ca.app.name} already exists. Aborting.")
    } getOrElse {
      ca.app.id.map { id =>
        apps.get(id) map { app =>
          error(
            s"App ID ${id} already exists and maps to the app '${app.name}'. " +
            "Aborting.")
          sys.exit(1)
        }
      }
      val appid = apps.insert(App(
        id = ca.app.id.getOrElse(0),
        name = ca.app.name,
        description = ca.app.description))
      appid map { id =>
        val events = Storage.getLEvents()
        val dbInit = events.init(id)
        if (dbInit) {
          info(s"Initialized Event Store for this app ID: ${id}.")
          val accessKeys = Storage.getMetaDataAccessKeys
          val accessKey = accessKeys.insert(AccessKey(
            key = "",
            appid = id,
            events = Seq()))
          accessKey map { k =>
            info("Created new app:")
            info(s"      Name: ${ca.app.name}")
            info(s"        ID: ${id}")
            info(s"Access Key: ${k}")
          } getOrElse {
            error(s"Unable to create new access key.")
          }
        } else {
          error(s"Unable to initialize Event Store for this app ID: ${id}.")
        }
        events.close()
      } getOrElse {
        error(s"Unable to create new app.")
      }
    }
  }

  def appList(ca: ConsoleArgs): Unit = {
    val apps = Storage.getMetaDataApps.getAll().sortBy(_.name)
    val title = "Name"
    info(f"$title%20s |   ID")
    apps foreach { app =>
      info(f"${app.name}%20s | ${app.id}%4d")
    }
    info(s"Finished listing ${apps.size} app(s).")
  }

  def appDelete(ca: ConsoleArgs): Unit = {
    val apps = Storage.getMetaDataApps
    apps.getByName(ca.app.name) map { app =>
      info(s"The following app will be deleted. Are you sure?")
      info(s"    App Name: ${app.name}")
      info(s"      App ID: ${app.id}")
      info(s" Description: ${app.description}")
      val choice = readLine("Enter 'YES' to proceed: ")
      choice match {
        case "YES" => {
          val events = Storage.getLEvents()
          if (events.remove(app.id)) {
            info(s"Removed Event Store for this app ID: ${app.id}")
            if (Storage.getMetaDataApps.delete(app.id))
              info(s"Deleted app ${app.name}.")
            else
              error(s"Error deleting app ${app.name}.")
          } else {
            error(s"Error removing Event Store for this app.")
          }
          events.close()
          info("Done.")
        }
        case _ => info("Aborted.")
      }
    } getOrElse {
      error(s"App ${ca.app.name} does not exist. Aborting.")
    }
  }

  def appDataDelete(ca: ConsoleArgs): Unit = {
    val apps = Storage.getMetaDataApps
    apps.getByName(ca.app.name) map { app =>
      info(s"The data of the following app will be deleted. Are you sure?")
      info(s"    App Name: ${app.name}")
      info(s"      App ID: ${app.id}")
      info(s" Description: ${app.description}")
      val choice = readLine("Enter 'YES' to proceed: ")
      choice match {
        case "YES" => {
          val events = Storage.getLEvents()
          // remove table
          if (events.remove(app.id)) {
            info(s"Removed Event Store for this app ID: ${app.id}")
          } else {
            error(s"Error removing Event Store for this app.")
          }
          // re-create table
          val dbInit = events.init(app.id)
          if (dbInit) {
            info(s"Initialized Event Store for this app ID: ${app.id}.")
          } else {
            error(s"Unable to initialize Event Store for this appId:" +
              s" ${app.id}.")
          }
          events.close()
          info("Done.")
        }
        case _ => info("Aborted.")
      }
    } getOrElse {
      error(s"App ${ca.app.name} does not exist. Aborting.")
    }
  }

  def accessKeyNew(ca: ConsoleArgs): Unit = {
    val apps = Storage.getMetaDataApps
    apps.getByName(ca.app.name) map { app =>
      val accessKeys = Storage.getMetaDataAccessKeys
      val accessKey = accessKeys.insert(AccessKey(
        key = "",
        appid = app.id,
        events = ca.accessKey.events))
      accessKey map { k =>
        info(s"Created new access key: ${k}")
      } getOrElse {
        error(s"Unable to create new access key.")
      }
    } getOrElse {
      error(s"App ${ca.app.name} does not exist. Aborting.")
    }
  }

  def accessKeyList(ca: ConsoleArgs): Unit = {
    val keys =
      if (ca.app.name == "")
        Storage.getMetaDataAccessKeys.getAll
      else {
        val apps = Storage.getMetaDataApps
        apps.getByName(ca.app.name) map { app =>
          Storage.getMetaDataAccessKeys.getByAppid(app.id)
        } getOrElse {
          error(s"App ${ca.app.name} does not exist. Aborting.")
          sys.exit(1)
        }
      }
    val title = "Access Key(s)"
    info(f"$title%64s | App ID | Allowed Event(s)")
    keys foreach { k =>
      val events = if (k.events.size > 0) k.events.mkString(",") else "(all)"
      info(f"${k.key}%s | ${k.appid}%6d | ${events}%s")
    }
    info(s"Finished listing ${keys.size} access key(s).")
  }

  def accessKeyDelete(ca: ConsoleArgs): Unit = {
    if (Storage.getMetaDataAccessKeys.delete(ca.accessKey.accessKey))
      info(s"Deleted access key ${ca.accessKey.accessKey}.")
    else
      error(s"Error deleting access key ${ca.accessKey.accessKey}.")
  }

  def status(ca: ConsoleArgs): Unit = {
    println("PredictionIO")
    ca.common.pioHome map { pioHome =>
      println(s"  Installed at: ${pioHome}")
      println(s"  Version: ${BuildInfo.version}")
    } getOrElse {
      println("Unable to locate PredictionIO installation. Aborting.")
      sys.exit(1)
    }
    println("")
    val sparkHome = getSparkHome(ca.common.sparkHome)
    if (new File(s"${sparkHome}/bin/spark-submit").exists) {
      println(s"Apache Spark")
      println(s"  Installed at: ${sparkHome}")
      val sparkMinVersion = "1.1.0"
      val sparkReleaseFile = new File(s"${sparkHome}/RELEASE")
      if (sparkReleaseFile.exists) {
        val sparkReleaseStrings =
          Source.fromFile(sparkReleaseFile).mkString.split(' ')
        val sparkReleaseVersion = sparkReleaseStrings(1)
        val parsedMinVersion = Version.apply(sparkMinVersion)
        val parsedCurrentVersion = Version.apply(sparkReleaseVersion)
        if (parsedCurrentVersion >= parsedMinVersion) {
          println(s"  Version: ${sparkReleaseVersion} (meets minimum " +
            s"requirement of ${sparkMinVersion})")
        } else {
          println("  Version: ${sparkReleaseVersion}")
          println("Apache Spark version does not meet minimum requirement. " +
            "Aborting.")
        }
      } else {
        println("  Version information cannot be found.")
        println("  If you are using a developmental tree, please make sure")
        println("  you are using a version of at least ${sparkMinVersion}.")
      }
    } else {
      println("Unable to locate a proper Apache Spark installation. Aborting.")
      sys.exit(1)
    }
    println("")
    println("Storage Backend Connections")
    try {
      Storage.verifyAllDataObjects()
    } catch {
      case e: Throwable =>
        e.printStackTrace
        println("")
        println("Unable to connect to all storage backend(s) successfully. " +
          "Please refer to error message(s) above. Aborting.")
        sys.exit(1)
    }
    println("")
    println("(sleeping 5 seconds for all messages to show up...)")
    Thread.sleep(5000)
    println("Your system is all ready to go.")
  }

  def coreAssembly(pioHome: String): File = {
    val core = s"pio-assembly-${BuildInfo.version}.jar"
    val coreDir =
      if (new File(pioHome + File.separator + "RELEASE").exists)
        new File(pioHome + File.separator + "lib")
      else
        new File(pioHome + File.separator + "assembly")
    val coreFile = new File(coreDir, core)
    if (coreFile.exists) {
      coreFile
    } else {
      error(s"PredictionIO Core Assembly (${coreFile.getCanonicalPath}) does " +
        "not exist. Aborting.")
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

  def withRegisteredManifest(
      json: File,
      engineId: Option[String],
      engineVersion: Option[String])(
      op: EngineManifest => Unit): Unit = {
    val ej = readManifestJson(json)
    val id = engineId getOrElse ej.id
    val version = engineVersion getOrElse ej.version
    Storage.getMetaDataEngineManifests.get(id, version) map {
      op
    } getOrElse {
      error(s"Engine ${id} ${version} cannot be found in the system.")
      error("Have you run the 'build' command to build your engine yet?")
      sys.exit(1)
    }
  }

  def jarFilesAt(path: File): Array[File] = recursiveListFiles(path) filter {
    _.getName.toLowerCase.endsWith(".jar")
  }

  def jarFilesForScala: Array[File] = {
    val libFiles = jarFilesForScalaFilter(jarFilesAt(new File("lib")))
    val targetFiles = jarFilesForScalaFilter(jarFilesAt(new File("target" +
      File.separator + s"scala-${scalaVersionNoPatch}")))
    // Use libFiles is target is empty.
    if (targetFiles.size > 0) targetFiles else libFiles
  }

  def jarFilesForScalaFilter(jars: Array[File]) =
    jars.filterNot { f =>
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
    ca.build.sbt map {
      _.getCanonicalPath
    } getOrElse {
      val f = new File(Seq(ca.common.pioHome.get, "sbt", "sbt").mkString(
        File.separator))
      if (f.exists) f.getCanonicalPath else "sbt"
    }
  }

  def joinFile(path: Seq[String]): String =
    path.mkString(File.separator)

  def builtinEngineDir(): Boolean = {
    val engineDir = new File(".." + File.separator + "engines").getCanonicalPath
    val cwd = new File(".").getCanonicalPath
    new File(".." + File.separator + "make-distribution.sh").exists &&
      engineDir == cwd
  }

  def distEngineDir(): Boolean = new File(distFilename).exists
}

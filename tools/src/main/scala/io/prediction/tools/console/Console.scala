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

package io.prediction.tools.console

import java.io.File

import grizzled.slf4j.Logging

import io.prediction.controller.Utils
import io.prediction.core.BuildInfo
import io.prediction.data.api.EventServer
import io.prediction.data.api.EventServerConfig
import io.prediction.data.storage.EngineManifest
import io.prediction.data.storage.EngineManifestSerializer
import io.prediction.data.storage.Storage
import io.prediction.data.storage.hbase.upgrade.Upgrade_0_8_3
import io.prediction.tools.admin.{AdminServer, AdminServerConfig}
import io.prediction.tools.RegisterEngine
import io.prediction.tools.RunServer
import io.prediction.tools.RunWorkflow
import io.prediction.tools.dashboard.Dashboard
import io.prediction.tools.dashboard.DashboardConfig
import io.prediction.workflow.WorkflowUtils
import org.apache.commons.io.FileUtils
import org.apache.hadoop.fs.Path
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.read
import org.json4s.native.Serialization.write
import semverfi._

import scala.collection.JavaConversions._
import scala.io.Source
import scala.sys.process._
import scala.util.Random
import scalaj.http.Http

case class ConsoleArgs(
  common: CommonArgs = CommonArgs(),
  build: BuildArgs = BuildArgs(),
  app: AppArgs = AppArgs(),
  accessKey: AccessKeyArgs = AccessKeyArgs(),
  deploy: DeployArgs = DeployArgs(),
  eventServer: EventServerArgs = EventServerArgs(),
  adminServer: AdminServerArgs = AdminServerArgs(),
  dashboard: DashboardArgs = DashboardArgs(),
  upgrade: UpgradeArgs = UpgradeArgs(),
  template: TemplateArgs = TemplateArgs(),
  export: ExportArgs = ExportArgs(),
  imprt: ImportArgs = ImportArgs(),
  commands: Seq[String] = Seq(),
  metricsClass: Option[String] = None,
  metricsParamsJsonPath: Option[String] = None,
  paramsPath: String = "params",
  engineInstanceId: Option[String] = None,
  mainClass: Option[String] = None)

case class CommonArgs(
  batch: String = "",
  sparkPassThrough: Seq[String] = Seq(),
  driverPassThrough: Seq[String] = Seq(),
  pioHome: Option[String] = None,
  sparkHome: Option[String] = None,
  engineId: Option[String] = None,
  engineVersion: Option[String] = None,
  engineFactory: Option[String] = None,
  engineParamsKey: Option[String] = None,
  evaluation: Option[String] = None,
  engineParamsGenerator: Option[String] = None,
  variantJson: File = new File("engine.json"),
  manifestJson: File = new File("manifest.json"),
  stopAfterRead: Boolean = false,
  stopAfterPrepare: Boolean = false,
  skipSanityCheck: Boolean = false,
  verbose: Boolean = false,
  verbosity: Int = 0,
  sparkKryo: Boolean = false)

case class BuildArgs(
  sbt: Option[File] = None,
  sbtExtra: Option[String] = None,
  sbtAssemblyPackageDependency: Boolean = true,
  sbtClean: Boolean = false,
  uberJar: Boolean = false,
  forceGeneratePIOSbt: Boolean = false)

case class DeployArgs(
  ip: String = "0.0.0.0",
  port: Int = 8000,
  logUrl: Option[String] = None,
  logPrefix: Option[String] = None)

case class EventServerArgs(
  enabled: Boolean = false,
  ip: String = "0.0.0.0",
  port: Int = 7070,
  stats: Boolean = false)

case class AdminServerArgs(
ip: String = "localhost",
port: Int = 7071)

case class DashboardArgs(
  ip: String = "0.0.0.0",
  port: Int = 9000)

case class UpgradeArgs(
  from: String = "0.0.0",
  to: String = "0.0.0",
  oldAppId: Int = 0,
  newAppId: Int = 0
)

object Console extends Logging {
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[ConsoleArgs]("pio") {
      override def showUsageOnError: Boolean = false
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
        if (x.exists) {
          success
        } else {
          failure(s"${x.getCanonicalPath} does not exist.")
        }
      } text("Path to an engine variant JSON file. Default: engine.json")
      opt[File]("manifest") abbr("m") action { (x, c) =>
        c.copy(common = c.common.copy(manifestJson = x))
      } validate { x =>
        if (x.exists) {
          success
        } else {
          failure(s"${x.getCanonicalPath} does not exist.")
        }
      } text("Path to an engine manifest JSON file. Default: manifest.json")
      opt[File]("sbt") action { (x, c) =>
        c.copy(build = c.build.copy(sbt = Some(x)))
      } validate { x =>
        if (x.exists) {
          success
        } else {
          failure(s"${x.getCanonicalPath} does not exist.")
        }
      } text("Path to sbt. Default: sbt")
      opt[Unit]("verbose") action { (x, c) =>
        c.copy(common = c.common.copy(verbose = true))
      }
      opt[Unit]("spark-kryo") abbr("sk") action { (x, c) =>
        c.copy(common = c.common.copy(sparkKryo = true))
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
          } text("Skip building external dependencies assembly."),
          opt[Unit]("uber-jar") action { (x, c) =>
            c.copy(build = c.build.copy(uberJar = true))
          },
          opt[Unit]("generate-pio-sbt") action { (x, c) =>
            c.copy(build = c.build.copy(forceGeneratePIOSbt = true))
          }
        )
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
            c.copy(common = c.common.copy(batch = x))
          } text("Batch label of the run."),
          opt[String]("params-path") action { (x, c) =>
            c.copy(paramsPath = x)
          } text("Directory to lookup parameters JSON files. Default: params"),
          opt[String]("metrics-params") abbr("mp") action { (x, c) =>
            c.copy(metricsParamsJsonPath = Some(x))
          } text("Metrics parameters JSON file. Will try to use\n" +
            "        metrics.json in the base path."),
          opt[Unit]("skip-sanity-check") abbr("ssc") action { (x, c) =>
            c.copy(common = c.common.copy(skipSanityCheck = true))
          },
          opt[Unit]("stop-after-read") abbr("sar") action { (x, c) =>
            c.copy(common = c.common.copy(stopAfterRead = true))
          },
          opt[Unit]("stop-after-prepare") abbr("sap") action { (x, c) =>
            c.copy(common = c.common.copy(stopAfterPrepare = true))
          },
          opt[Unit]("uber-jar") action { (x, c) =>
            c.copy(build = c.build.copy(uberJar = true))
          },
          opt[Int]("verbosity") action { (x, c) =>
            c.copy(common = c.common.copy(verbosity = x))
          },
          opt[String]("engine-factory") action { (x, c) =>
            c.copy(common = c.common.copy(engineFactory = Some(x)))
          },
          opt[String]("engine-params-key") action { (x, c) =>
            c.copy(common = c.common.copy(engineParamsKey = Some(x)))
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
          arg[String]("<evaluation-class>") action { (x, c) =>
            c.copy(common = c.common.copy(evaluation = Some(x)))
          },
          arg[String]("[<engine-parameters-generator-class>]") optional() action { (x, c) =>
            c.copy(common = c.common.copy(engineParamsGenerator = Some(x)))
          } text("Optional engine parameters generator class, overriding the first argument"),
          opt[String]("batch") action { (x, c) =>
            c.copy(common = c.common.copy(batch = x))
          } text("Batch label of the run.")
        )
      note("")
      cmd("deploy").
        text("Deploy an engine instance as a prediction server. This\n" +
          "command will pass all pass-through arguments to its underlying\n" +
          "spark-submit command.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "deploy")
        } children(
          opt[String]("batch") action { (x, c) =>
            c.copy(common = c.common.copy(batch = x))
          } text("Batch label of the deployment."),
          opt[String]("engine-instance-id") action { (x, c) =>
            c.copy(engineInstanceId = Some(x))
          } text("Engine instance ID."),
          opt[String]("ip") action { (x, c) =>
            c.copy(deploy = c.deploy.copy(ip = x))
          },
          opt[Int]("port") action { (x, c) =>
            c.copy(deploy = c.deploy.copy(port = x))
          } text("Port to bind to. Default: 8000"),
          opt[Unit]("feedback") action { (_, c) =>
            c.copy(eventServer = c.eventServer.copy(enabled = true))
          } text("Enable feedback loop to event server."),
          opt[String]("event-server-ip") action { (x, c) =>
            c.copy(eventServer = c.eventServer.copy(ip = x))
          },
          opt[Int]("event-server-port") action { (x, c) =>
            c.copy(eventServer = c.eventServer.copy(port = x))
          } text("Event server port. Default: 7070"),
          opt[Int]("admin-server-port") action { (x, c) =>
            c.copy(adminServer = c.adminServer.copy(port = x))
          } text("Admin server port. Default: 7071"),
          opt[String]("admin-server-port") action { (x, c) =>
          c.copy(adminServer = c.adminServer.copy(ip = x))
          } text("Admin server IP. Default: localhost"),
          opt[String]("accesskey") action { (x, c) =>
            c.copy(accessKey = c.accessKey.copy(accessKey = x))
          } text("Access key of the App where feedback data will be stored."),
          opt[Unit]("uber-jar") action { (x, c) =>
            c.copy(build = c.build.copy(uberJar = true))
          },
          opt[String]("log-url") action { (x, c) =>
            c.copy(deploy = c.deploy.copy(logUrl = Some(x)))
          },
          opt[String]("log-prefix") action { (x, c) =>
            c.copy(deploy = c.deploy.copy(logPrefix = Some(x)))
          }
        )
      note("")
      cmd("undeploy").
        text("Undeploy an engine instance as a prediction server.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "undeploy")
        } children(
          opt[String]("ip") action { (x, c) =>
            c.copy(deploy = c.deploy.copy(ip = x))
          },
          opt[Int]("port") action { (x, c) =>
            c.copy(deploy = c.deploy.copy(port = x))
          } text("Port to unbind from. Default: 8000")
        )
      note("")
      cmd("dashboard").
        text("Launch a dashboard at the specific IP and port.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "dashboard")
        } children(
          opt[String]("ip") action { (x, c) =>
            c.copy(dashboard = c.dashboard.copy(ip = x))
          },
          opt[Int]("port") action { (x, c) =>
            c.copy(dashboard = c.dashboard.copy(port = x))
          } text("Port to bind to. Default: 9000")
        )
      note("")
      cmd("eventserver").
        text("Launch an Event Server at the specific IP and port.").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "eventserver")
        } children(
          opt[String]("ip") action { (x, c) =>
            c.copy(eventServer = c.eventServer.copy(ip = x))
          },
          opt[Int]("port") action { (x, c) =>
            c.copy(eventServer = c.eventServer.copy(port = x))
          } text("Port to bind to. Default: 7070"),
          opt[Unit]("stats") action { (x, c) =>
            c.copy(eventServer = c.eventServer.copy(stats = true))
          }
        )
      cmd("adminserver").
        text("Launch an Admin Server at the specific IP and port.").
        action { (_, c) =>
        c.copy(commands = c.commands :+ "adminserver")
      } children(
        opt[String]("ip") action { (x, c) =>
          c.copy(adminServer = c.adminServer.copy(ip = x))
        } text("IP to bind to. Default: localhost"),
        opt[Int]("port") action { (x, c) =>
          c.copy(adminServer = c.adminServer.copy(port = x))
        } text("Port to bind to. Default: 7071")
        )
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
      note("")
      cmd("upgrade").
        text("Upgrade tool").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "upgrade")
        } children(
          arg[String]("<from version>") action { (x, c) =>
            c.copy(upgrade = c.upgrade.copy(from = x))
          } text("The version upgraded from."),
          arg[String]("<to version>") action { (x, c) =>
            c.copy(upgrade = c.upgrade.copy(to = x))
          } text("The version upgraded to."),
          arg[Int]("<old App ID>") action { (x, c) =>
            c.copy(upgrade = c.upgrade.copy(oldAppId = x))
          } text("Old App ID."),
          arg[Int]("<new App ID>") action { (x, c) =>
            c.copy(upgrade = c.upgrade.copy(newAppId = x))
          } text("New App ID.")
        )
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
          cmd("show").
            text("Show details of an app.").
            action { (_, c) =>
              c.copy(commands = c.commands :+ "show")
            } children (
              arg[String]("<name>") action { (x, c) =>
                c.copy(app = c.app.copy(name = x))
              } text("Name of the app to be shown.")
            ),
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
              } text("Name of the app whose data to be deleted."),
              opt[String]("channel") action { (x, c) =>
                c.copy(app = c.app.copy(dataDeleteChannel = Some(x)))
              } text("Name of channel whose data to be deleted."),
              opt[Unit]("all") action { (x, c) =>
                c.copy(app = c.app.copy(all = true))
              } text("Delete data of all channels including default")
            ),
          note(""),
          cmd("channel-new").
            text("Create a new channel for the app.").
            action { (_, c) =>
              c.copy(commands = c.commands :+ "channel-new")
            } children (
              arg[String]("<name>") action { (x, c) =>
                c.copy(app = c.app.copy(name = x))
              } text("App name."),
              arg[String]("<channel>") action { (x, c) =>
                c.copy(app = c.app.copy(channel = x))
              } text ("Channel name to be created.")
            ),
          note(""),
          cmd("channel-delete").
            text("Delete a channel of the app.").
            action { (_, c) =>
              c.copy(commands = c.commands :+ "channel-delete")
            } children (
              arg[String]("<name>") action { (x, c) =>
                c.copy(app = c.app.copy(name = x))
              } text("App name."),
              arg[String]("<channel>") action { (x, c) =>
                c.copy(app = c.app.copy(channel = x))
              } text ("Channel name to be deleted.")
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
              arg[String]("<app name>") optional() action { (x, c) =>
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
      cmd("template").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "template")
        } children(
          cmd("get").
            action { (_, c) =>
              c.copy(commands = c.commands :+ "get")
            } children(
              arg[String]("<template ID>") required() action { (x, c) =>
                c.copy(template = c.template.copy(repository = x))
              },
              arg[String]("<new engine directory>") action { (x, c) =>
                c.copy(template = c.template.copy(directory = x))
              },
              opt[String]("version") action { (x, c) =>
                c.copy(template = c.template.copy(version = Some(x)))
              },
              opt[String]("name") action { (x, c) =>
                c.copy(template = c.template.copy(name = Some(x)))
              },
              opt[String]("package") action { (x, c) =>
                c.copy(template = c.template.copy(packageName = Some(x)))
              },
              opt[String]("email") action { (x, c) =>
                c.copy(template = c.template.copy(email = Some(x)))
              }
            ),
          cmd("list").
            action { (_, c) =>
              c.copy(commands = c.commands :+ "list")
            }
        )
      cmd("export").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "export")
        } children(
          opt[Int]("appid") required() action { (x, c) =>
            c.copy(export = c.export.copy(appId = x))
          },
          opt[String]("output") required() action { (x, c) =>
            c.copy(export = c.export.copy(outputPath = x))
          },
          opt[String]("format") action { (x, c) =>
            c.copy(export = c.export.copy(format = x))
          },
          opt[String]("channel") action { (x, c) =>
            c.copy(export = c.export.copy(channel = Some(x)))
          }
        )
      cmd("import").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "import")
        } children(
          opt[Int]("appid") required() action { (x, c) =>
            c.copy(imprt = c.imprt.copy(appId = x))
          },
          opt[String]("input") required() action { (x, c) =>
            c.copy(imprt = c.imprt.copy(inputPath = x))
          },
          opt[String]("channel") action { (x, c) =>
            c.copy(imprt = c.imprt.copy(channel = Some(x)))
          }
        )
    }

    val separatorIndex = args.indexWhere(_ == "--")
    val (consoleArgs, theRest) =
      if (separatorIndex == -1) {
        (args, Array[String]())
      } else {
        args.splitAt(separatorIndex)
      }
    val allPassThroughArgs = theRest.drop(1)
    val secondSepIdx = allPassThroughArgs.indexWhere(_ == "--")
    val (sparkPassThroughArgs, driverPassThroughArgs) =
      if (secondSepIdx == -1) {
        (allPassThroughArgs, Array[String]())
      } else {
        val t = allPassThroughArgs.splitAt(secondSepIdx)
        (t._1, t._2.drop(1))
      }

    parser.parse(consoleArgs, ConsoleArgs()) map { pca =>
      val ca = pca.copy(common = pca.common.copy(
        sparkPassThrough = sparkPassThroughArgs,
        driverPassThrough = driverPassThroughArgs))
      WorkflowUtils.modifyLogging(ca.common.verbose)
      val rv: Int = ca.commands match {
        case Seq("") =>
          System.err.println(help())
          1
        case Seq("version") =>
          version(ca)
          0
        case Seq("build") =>
          regenerateManifestJson(ca.common.manifestJson)
          build(ca)
        case Seq("unregister") =>
          unregister(ca)
          0
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
          0
        case Seq("eventserver") =>
          eventserver(ca)
          0
        case Seq("adminserver") =>
          adminserver(ca)
          0
        case Seq("run") =>
          generateManifestJson(ca.common.manifestJson)
          run(ca)
        case Seq("status") =>
          status(ca)
        case Seq("upgrade") =>
          upgrade(ca)
          0
        case Seq("app", "new") =>
          App.create(ca)
        case Seq("app", "list") =>
          App.list(ca)
        case Seq("app", "show") =>
          App.show(ca)
        case Seq("app", "delete") =>
          App.delete(ca)
        case Seq("app", "data-delete") =>
          App.dataDelete(ca)
        case Seq("app", "channel-new") =>
          App.channelNew(ca)
        case Seq("app", "channel-delete") =>
          App.channelDelete(ca)
        case Seq("accesskey", "new") =>
          AccessKey.create(ca)
        case Seq("accesskey", "list") =>
          AccessKey.list(ca)
        case Seq("accesskey", "delete") =>
          AccessKey.delete(ca)
        case Seq("template", "get") =>
          Template.get(ca)
        case Seq("template", "list") =>
          Template.list(ca)
        case Seq("export") =>
          Export.eventsToFile(ca, coreAssembly(ca.common.pioHome.get))
        case Seq("import") =>
          Import.fileToEvents(ca, coreAssembly(ca.common.pioHome.get))
        case _ =>
          System.err.println(help(ca.commands))
          1
      }
      sys.exit(rv)
    } getOrElse {
      val command = args.toSeq.filterNot(_.startsWith("--")).head
      System.err.println(help(Seq(command)))
      sys.exit(1)
    }
  }

  def help(commands: Seq[String] = Seq()): String = {
    if (commands.isEmpty) {
      mainHelp
    } else {
      val stripped =
        (if (commands.head == "help") commands.drop(1) else commands).
          mkString("-")
      helpText.getOrElse(stripped, s"Help is unavailable for ${stripped}.")
    }
  }

  val mainHelp = txt.main().toString

  val helpText = Map(
    "" -> mainHelp,
    "status" -> txt.status().toString,
    "upgrade" -> txt.upgrade().toString,
    "version" -> txt.version().toString,
    "template" -> txt.template().toString,
    "build" -> txt.build().toString,
    "train" -> txt.train().toString,
    "deploy" -> txt.deploy().toString,
    "eventserver" -> txt.eventserver().toString,
    "adminserver" -> txt.adminserver().toString,
    "app" -> txt.app().toString,
    "accesskey" -> txt.accesskey().toString,
    "import" -> txt.imprt().toString,
    "export" -> txt.export().toString,
    "run" -> txt.run().toString,
    "eval" -> txt.eval().toString,
    "dashboard" -> txt.dashboard().toString)

  def version(ca: ConsoleArgs): Unit = println(BuildInfo.version)

  def build(ca: ConsoleArgs): Int = {
    Template.verifyTemplateMinVersion(new File("template.json"))
    compile(ca)
    info("Looking for an engine...")
    val jarFiles = jarFilesForScala
    if (jarFiles.size == 0) {
      error("No engine found. Your build might have failed. Aborting.")
      return 1
    }
    jarFiles foreach { f => info(s"Found ${f.getName}")}
    val copyLocal = if (sys.env.contains("HADOOP_CONF_DIR")) {
      info("HADOOP_CONF_DIR is set. Assuming HDFS is available.")
      true
    } else {
      info("HADOOP_CONF_DIR is not set. Assuming HDFS is unavailable.")
      false
    }
    val finalJarFiles = if (sys.env.contains("HADOOP_CONF_DIR") && !ca.build.uberJar) {
      info("Also copying PredictionIO core assembly.")
      jarFiles :+ coreAssembly(ca.common.pioHome.get)
    } else {
      jarFiles
    }
    RegisterEngine.registerEngine(
      ca.common.manifestJson,
      finalJarFiles,
      copyLocal)
    info("Your engine is ready for training.")
    0
  }

  def unregister(ca: ConsoleArgs): Unit = {
    RegisterEngine.unregisterEngine(ca.common.manifestJson)
  }

  def train(ca: ConsoleArgs): Int = {
    Template.verifyTemplateMinVersion(new File("template.json"))
    withRegisteredManifest(
      ca.common.manifestJson,
      ca.common.engineId,
      ca.common.engineVersion) { em =>
      if (ca.build.uberJar) {
        val uniqueJars =
          em.files.map(_.split(Path.SEPARATOR_CHAR).last).groupBy(identity).keys
        if (uniqueJars.size > 1) {
          error("Uber JAR mode cannot be turned on when current build produced " +
            "more than 1 engine JAR files. Aborting.")
          return 1
        }
      }
      RunWorkflow.runWorkflow(
        ca,
        coreAssembly(ca.common.pioHome.get),
        em,
        ca.common.variantJson)
    }
  }

  def deploy(ca: ConsoleArgs): Int = {
    Template.verifyTemplateMinVersion(new File("template.json"))
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
          return 1
      }
      val engineInstances = Storage.getMetaDataEngineInstances
      val engineInstance = ca.engineInstanceId map { eid =>
        engineInstances.get(eid)
      } getOrElse {
        engineInstances.getLatestCompleted(em.id, em.version, variantId)
      }
      engineInstance map { r =>
        // undeploy(ca)
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
        1
      }
    }
  }

  def dashboard(ca: ConsoleArgs): Unit = {
    info(s"Creating dashboard at ${ca.dashboard.ip}:${ca.dashboard.port}")
    Dashboard.createDashboard(DashboardConfig(
      ip = ca.dashboard.ip,
      port = ca.dashboard.port))
  }

  def eventserver(ca: ConsoleArgs): Unit = {
    info(
      s"Creating Event Server at ${ca.eventServer.ip}:${ca.eventServer.port}")
    EventServer.createEventServer(EventServerConfig(
      ip = ca.eventServer.ip,
      port = ca.eventServer.port,
      stats = ca.eventServer.stats))
  }

  def adminserver(ca: ConsoleArgs): Unit = {
    info(
      s"Creating Admin Server at ${ca.adminServer.ip}:${ca.adminServer.port}")
    AdminServer.createAdminServer(AdminServerConfig(
      ip = ca.adminServer.ip,
      port = ca.adminServer.port
    ))
  }

  def undeploy(ca: ConsoleArgs): Int = {
    val serverUrl = s"http://${ca.deploy.ip}:${ca.deploy.port}"
    info(
      s"Undeploying any existing engine instance at ${serverUrl}")
    try {
      val code = Http(s"${serverUrl}/stop").asString.code
      code match {
        case 200 => 0
        case 404 =>
          error(s"Another process is using ${serverUrl}. Unable to undeploy.")
          1
        case _ =>
          error(s"Another process is using ${serverUrl}, or an existing " +
            s"engine server is not responding properly (HTTP ${code}). " +
            "Unable to undeploy.")
            1
      }
    } catch {
      case e: java.net.ConnectException =>
        warn(s"Nothing at ${serverUrl}")
        0
      case _: Throwable =>
        error("Another process might be occupying " +
          s"${ca.deploy.ip}:${ca.deploy.port}. Unable to undeploy.")
        1
    }
  }

  def compile(ca: ConsoleArgs): Unit = {
    // only add pioVersion to sbt if project/pio.sbt exists
    if (new File("project", "pio-build.sbt").exists || ca.build.forceGeneratePIOSbt) {
      FileUtils.writeLines(
        new File("pio.sbt"),
        Seq(
          "// Generated automatically by pio build.",
          "// Changes in this file will be overridden.",
          "",
          "pioVersion := \"" + BuildInfo.version + "\""))
    }
    implicit val formats = Utils.json4sDefaultFormats
    try {
      val engineFactory =
        (parse(Source.fromFile("engine.json").mkString) \ "engineFactory").
          extract[String]
      WorkflowUtils.checkUpgrade("build", engineFactory)
    } catch {
      case e: Throwable => WorkflowUtils.checkUpgrade("build")
    }
    val sbt = detectSbt(ca)
    info(s"Using command '${sbt}' at the current working directory to build.")
    info("If the path above is incorrect, this process will fail.")
    val asm =
      if (ca.build.sbtAssemblyPackageDependency) {
        " assemblyPackageDependency"
      } else {
        ""
      }
    val clean = if (ca.build.sbtClean) " clean" else ""
    val buildCmd = s"${sbt} ${ca.build.sbtExtra.getOrElse("")}${clean} " +
      (if (ca.build.uberJar) "assembly" else s"package${asm}")
    val core = new File(s"pio-assembly-${BuildInfo.version}.jar")
    if (ca.build.uberJar) {
      info(s"Uber JAR enabled. Putting ${core.getName} in lib.")
      val dst = new File("lib")
      dst.mkdir()
      FileUtils.copyFileToDirectory(
        coreAssembly(ca.common.pioHome.get),
        dst,
        true)
    } else {
      info(s"Uber JAR disabled. Making sure lib/${core.getName} is absent.")
      new File("lib", core.getName).delete()
    }
    info(s"Going to run: ${buildCmd}")
    try {
      val r =
        if (ca.common.verbose) {
          buildCmd.!(ProcessLogger(line => info(line), line => error(line)))
        } else {
          buildCmd.!(ProcessLogger(
            line => outputSbtError(line),
            line => outputSbtError(line)))
        }
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

  private def outputSbtError(line: String): Unit = {
    """\[.*error.*\]""".r findFirstIn line foreach { _ => error(line) }
  }

  def run(ca: ConsoleArgs): Int = {
    compile(ca)

    val extraFiles = WorkflowUtils.thirdPartyConfFiles

    val jarFiles = jarFilesForScala
    jarFiles foreach { f => info(s"Found JAR: ${f.getName}") }
    val allJarFiles = jarFiles.map(_.getCanonicalPath)
    val cmd = s"${getSparkHome(ca.common.sparkHome)}/bin/spark-submit --jars " +
      s"${allJarFiles.mkString(",")} " +
      (if (extraFiles.size > 0) {
        s"--files ${extraFiles.mkString(",")} "
      } else {
        ""
      }) +
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
      return 1
    }
    r
  }

  def status(ca: ConsoleArgs): Int = {
    println("PredictionIO")
    ca.common.pioHome map { pioHome =>
      println(s"  Installed at: ${pioHome}")
      println(s"  Version: ${BuildInfo.version}")
    } getOrElse {
      println("Unable to locate PredictionIO installation. Aborting.")
      return 1
    }
    println("")
    val sparkHome = getSparkHome(ca.common.sparkHome)
    if (new File(s"${sparkHome}/bin/spark-submit").exists) {
      println(s"Apache Spark")
      println(s"  Installed at: ${sparkHome}")
      val sparkMinVersion = "1.3.0"
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
        println(s"  you are using a version of at least ${sparkMinVersion}.")
      }
    } else {
      println("Unable to locate a proper Apache Spark installation. Aborting.")
      return 1
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
        return 1
    }
    println("")
    println("(sleeping 5 seconds for all messages to show up...)")
    Thread.sleep(5000)
    println("Your system is all ready to go.")
    0
  }

  def upgrade(ca: ConsoleArgs): Unit = {
    (ca.upgrade.from, ca.upgrade.to) match {
      case ("0.8.2", "0.8.3") => {
        Upgrade_0_8_3.runMain(ca.upgrade.oldAppId, ca.upgrade.newAppId)
      }
      case _ =>
        println(s"Upgrade from version ${ca.upgrade.from} to ${ca.upgrade.to}"
          + s" is not supported.")
    }
  }

  def coreAssembly(pioHome: String): File = {
    val core = s"pio-assembly-${BuildInfo.version}.jar"
    val coreDir =
      if (new File(pioHome + File.separator + "RELEASE").exists) {
        new File(pioHome + File.separator + "lib")
      } else {
        new File(pioHome + File.separator + "assembly")
      }
    val coreFile = new File(coreDir, core)
    if (coreFile.exists) {
      coreFile
    } else {
      error(s"PredictionIO Core Assembly (${coreFile.getCanonicalPath}) does " +
        "not exist. Aborting.")
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
      op: EngineManifest => Int): Int = {
    val ej = readManifestJson(json)
    val id = engineId getOrElse ej.id
    val version = engineVersion getOrElse ej.version
    Storage.getMetaDataEngineManifests.get(id, version) map {
      op
    } getOrElse {
      error(s"Engine ${id} ${version} cannot be found in the system.")
      error("Possible reasons:")
      error("- the engine is not yet built by the 'build' command;")
      error("- the meta data store is offline.")
      1
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

  def jarFilesForScalaFilter(jars: Array[File]): Array[File] =
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
}

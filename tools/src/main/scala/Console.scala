package io.prediction.tools

import io.prediction.storage.Storage

import scala.sys.process._

import java.io.File

object Console {
  case class ConsoleArgs(
    pioHome: Option[String] = None,
    engineJson: String = "engine.json",
    commands: Seq[String] = Seq(),
    engineId: Option[String] = None,
    engineVersion: Option[String] = None)

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[ConsoleArgs]("pio") {
      head("PredictionIO Command Line Interface Console\n")
      note("The following options are common to all commands:\n")
      opt[String]("pio-home") action { (x, c) =>
        c.copy(pioHome = Some(x))
      } text("Root directory of a PredictionIO installation.\n" +
        "        Specify this if automatic discovery fail.\n")
      opt[String]("engine-json") action { (x, c) =>
        c.copy(engineJson = x)
      } text("Path to an engine JSON file. Default: engine.json")
      note("\n")
      cmd("register").
        text("Build and register an engine at the current directory.\n").
        action { (_, c) =>
          c.copy(commands = c.commands :+ "register")
        }
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

    parser.parse(args, ConsoleArgs()) map { ca =>
      ca.commands match {
        case Seq("register") =>
          register(ca)
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
      val pioSbt = s"${pioHome}/sbt/sbt"
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

    val jarFiles = recursiveListFiles(new File("target")) filter {
      _.getName.toLowerCase.endsWith(".jar")
    }
    jarFiles foreach { f => println(s"Found ${f.getName}")}

    RegisterEngine.registerEngine(ca.engineJson, jarFiles)
  }

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }
}

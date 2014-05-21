package io.prediction.core.tools

import java.io.{ File, IOException }

import grizzled.slf4j.Logging
import org.apache.commons.io.FileUtils

/**
 * This CLI tool creates an engine template that engine developers can use to
 * build their custom engines.
 */
object CreateEngineTemplate extends Logging {
  def main(args: Array[String]): Unit = {
    val projectDir = new File(args(0))
    val engineTemplateBaseDir = "/engine/templates"
    val engineTemplates = Array(
      "build.sbt",
      "src/main/scala/Data.scala",
      "src/main/scala/DataPreparator.scala",
      "src/main/scala/Algorithm.scala",
      "src/main/scala/Server.scala",
      "src/main/scala/Evaluator.scala",
      "src/main/scala/Runner.scala")

    try {
      engineTemplates foreach { tplFile =>
        FileUtils.copyInputStreamToFile(getClass.getResourceAsStream(
          s"/engine/templates/${tplFile}"),
          new File(s"${projectDir}/${tplFile}"))
      }
      info(s"Created new engine project at ${projectDir}.")
    } catch {
      case e: IOException => error(s"Giving up due to error: ${e.getMessage()}")
    }
  }
}

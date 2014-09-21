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
    val language = args(1)
    val engineTemplateBaseDir = "/engine/templates"
    val engineTemplates = language match {
      case "scala" =>
        Array(
          "build.sbt",
          "src/main/scala/Data.scala",
          "src/main/scala/DataPreparator.scala",
          "src/main/scala/Algorithm.scala",
          "src/main/scala/Server.scala",
          "src/main/scala/Evaluator.scala",
          "src/main/scala/Runner.scala")
      case "java" =>
        Array(
          "build.sbt",
          "src/main/java/MyData.java",
          "src/main/java/MyParams.java",
          "src/main/java/DataSource.java",
          "src/main/java/Preparator.java",
          "src/main/java/Algorithm.java",
          "src/main/java/Serving.java",
          "src/main/java/Metrics.java")
      case _ => {
        error(s"Unsupported language: ${language}.")
        System.exit(1)
        Array()
      }
    }

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

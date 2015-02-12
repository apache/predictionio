package io.prediction.tools.console

import io.prediction.tools.Runner

import java.io.File

case class ExportArgs(
  appId: Int = 0,
  outputPath: String = "",
  format: String = "json")

object Export {
  def eventsToFile(ca: ConsoleArgs, core: File): Int = {
    Runner.runOnSpark(
      "io.prediction.tools.export.EventsToFile",
      Seq(
        "--appid",
        ca.export.appId.toString,
        "--output",
        ca.export.outputPath,
        "--format",
        ca.export.format),
      ca,
      core)
  }
}

package io.prediction.tools.console

import io.prediction.tools.Runner

import java.io.File

case class ImportArgs(
  appId: Int = 0,
  inputPath: String = "")

object Import {
  def fileToEvents(ca: ConsoleArgs, core: File): Int = {
    Runner.runOnSpark(
      "io.prediction.tools.imprt.FileToEvents",
      Seq(
        "--appid",
        ca.imprt.appId.toString,
        "--input",
        ca.imprt.inputPath),
      ca,
      core)
  }
}

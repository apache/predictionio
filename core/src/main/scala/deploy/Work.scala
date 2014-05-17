package io.prediction.core.deploy

import java.io.File

trait Work {
  def workId: String
  def job: Any
}

case class SimpleWork(workId: String, job: Any) extends Work

case class WorkResult(workId: String, result: Any)

case class Process(
  workId: String,
  command: Seq[String],
  cwd: Option[File],
  extraEnv: Array[(String, String)],
  job: Any = Nil) extends Work

case class ProcessResult(workId: String, returnCode: Int)

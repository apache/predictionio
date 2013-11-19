package io.prediction.scheduler

import io.prediction.commons.filepath.BaseDir
import io.prediction.commons.Config

import scala.sys.process._

import play.api._
import play.api.libs.json._
import play.api.mvc._

object Operations extends Controller {
  val config = new Config

  def deleteApp(appid: Int) = Action {
    val path = BaseDir.appDir(config.settingsHdfsRoot, appid)
    // mkdir again to make sure that rmr failure is not due to non existing dir.
    // mkdir error can be ignored.
    val mkdir = s"${Scheduler.hadoopCommand} fs -mkdir $path".!
    val code = s"${Scheduler.hadoopCommand} fs -rmr $path".!
    if (code == 0)
      Ok(Json.obj("message" -> s"Deleted HDFS storage for App ID: $appid ($path)"))
    else
      InternalServerError(Json.obj("message" -> s"Unable to delete HDFS storage for App ID: $appid ($path). Please check scheduler.log and hadoop log files."))
  }

  def deleteEngine(appid: Int, engineid: Int) = Action {
    val path = BaseDir.engineDir(config.settingsHdfsRoot, appid, engineid)
    // mkdir again to make sure that rmr failure is not due to non existing dir.
    // mkdir error can be ignored.
    val mkdir = s"${Scheduler.hadoopCommand} fs -mkdir $path".!
    val code = s"${Scheduler.hadoopCommand} fs -rmr $path".!
    if (code == 0)
      Ok(Json.obj("message" -> s"Deleted HDFS storage for App ID: $appid, Engine ID: $engineid ($path)"))
    else
      InternalServerError(Json.obj("message" -> s"Unable to delete HDFS storage for App ID: $appid, Engine ID: $engineid ($path). Please check scheduler.log and hadoop log files."))
  }

  def deleteAlgo(appid: Int, engineid: Int, algoid: Int) = Action {
    val path = BaseDir.algoDir(config.settingsHdfsRoot, appid, engineid, algoid, None)
    deleteAlgoBase(path, appid, engineid, algoid, None)
  }

  def deleteOfflineEval(appid: Int, engineid: Int, offlineevalid: Int) = Action {
    val path = BaseDir.offlineEvalDir(config.settingsHdfsRoot, appid, engineid, offlineevalid)
    // mkdir again to make sure that rmr failure is not due to non existing dir.
    // mkdir error can be ignored.
    val mkdir = s"${Scheduler.hadoopCommand} fs -mkdir $path".!
    val code = s"${Scheduler.hadoopCommand} fs -rmr $path".!
    if (code == 0)
      Ok(Json.obj("message" -> s"Deleted HDFS storage for App ID: $appid, Engine ID: $engineid, OfflineEval ID: $offlineevalid ($path)"))
    else
      InternalServerError(Json.obj("message" -> s"Unable to delete HDFS storage for App ID: $appid, Engine ID: $engineid, OfflineEval ID: $offlineevalid ($path). Please check scheduler.log and hadoop log files."))
  }

  def deleteOfflineEvalAlgo(appid: Int, engineid: Int, offlineevalid: Int, algoid: Int) = Action {
    val path = BaseDir.algoDir(config.settingsHdfsRoot, appid, engineid, algoid, Some(offlineevalid))
    deleteAlgoBase(path, appid, engineid, algoid, Some(offlineevalid))
  }

  def deleteAlgoBase(path: String, appid: Int, engineid: Int, algoid: Int, offlineevalid: Option[Int]) = {
    // mkdir again to make sure that rmr failure is not due to non existing dir.
    // mkdir error can be ignored.
    val mkdir = s"${Scheduler.hadoopCommand} fs -mkdir $path".!
    val code = s"${Scheduler.hadoopCommand} fs -rmr $path".!
    if (code == 0)
      Ok(Json.obj("message" -> s"Deleted HDFS storage for App ID: $appid, Engine ID: $engineid, Algo ID: $algoid, OfflineEval ID: ${offlineevalid.getOrElse("N/A")} ($path)"))
    else
      InternalServerError(Json.obj("message" -> s"Unable to delete HDFS storage for App ID: $appid, Engine ID: $engineid, Algo ID: $algoid, OfflineEval ID: ${offlineevalid.getOrElse("N/A")} ($path). Please check scheduler and hadoop log files."))
  }
}

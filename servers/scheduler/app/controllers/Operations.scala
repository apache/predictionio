package io.prediction.scheduler

import io.prediction.commons.filepath.BaseDir
import io.prediction.commons.Config

//import org.apache.hadoop.conf.Configuration
//import org.apache.hadoop.fs.{FileSystem, Path}

import play.api._
import play.api.libs.json._
import play.api.mvc._

object Operations extends Controller {
  val config = new Config

  /** Hadoop stuff. */
  //val conf = new Configuration
  //val fileSystem = FileSystem.get(conf)

  def deleteApp(appid: Int) = Action {
    //val path = new Path(BaseDir.appDir(config.settingsHdfsRoot, appid))
    val path = BaseDir.appDir(config.settingsHdfsRoot, appid)
    try {
      //fileSystem.delete(path, true)
      Ok(Json.obj("message" -> "Deleted HDFS storage for App ID: %d (%s)".format(appid, path)))
    } catch {
      case e: java.io.IOException => InternalServerError(Json.obj("message" -> "Unable to delete HDFS storage for App ID: %d (%s) due to an exception: %s".format(appid, path, e.getMessage)))
    }
  }

  def deleteEngine(appid: Int, engineid: Int) = Action {
    //val path = new Path(BaseDir.engineDir(config.settingsHdfsRoot, appid, engineid))
    val path = BaseDir.engineDir(config.settingsHdfsRoot, appid, engineid)
    try {
      //fileSystem.delete(path, true)
      Ok(Json.obj("message" -> "Deleted HDFS storage for App ID: %d, Engine ID: %d (%s)".format(appid, engineid, path)))
    } catch {
      case e: java.io.IOException => InternalServerError(Json.obj("message" -> "Unable to delete HDFS storage for App ID: %d, Engine ID: %d (%s) due to an exception: %s".format(appid, engineid, path, e.getMessage)))
    }
  }

  def deleteAlgo(appid: Int, engineid: Int, algoid: Int) = Action {
    //val path = new Path(BaseDir.algoDir(config.settingsHdfsRoot, appid, engineid, algoid, None))
    val path = BaseDir.algoDir(config.settingsHdfsRoot, appid, engineid, algoid, None)
    deleteAlgoBase(path, appid, engineid, algoid, None)
  }

  def deleteOfflineEval(appid: Int, engineid: Int, offlineevalid: Int) = Action {
    //val path = new Path(BaseDir.offlineEvalDir(config.settingsHdfsRoot, appid, engineid, offlineevalid))
    val path = BaseDir.offlineEvalDir(config.settingsHdfsRoot, appid, engineid, offlineevalid)
    try {
      //fileSystem.delete(path, true)
      Ok(Json.obj("message" -> "Deleted HDFS storage for App ID: %d, Engine ID: %d, OfflineEval ID: %d (%s)".format(appid, engineid, offlineevalid, path)))
    } catch {
      case e: java.io.IOException => InternalServerError(Json.obj("message" -> "Unable to delete HDFS storage for App ID: %d, Engine ID: %d, OfflineEval ID: %d (%s) due to an exception: %s".format(appid, engineid, offlineevalid, path, e.getMessage)))
    }
  }

  def deleteOfflineEvalAlgo(appid: Int, engineid: Int, offlineevalid: Int, algoid: Int) = Action {
    //val path = new Path(BaseDir.algoDir(config.settingsHdfsRoot, appid, engineid, algoid, Some(offlineevalid)))
    val path = BaseDir.algoDir(config.settingsHdfsRoot, appid, engineid, algoid, Some(offlineevalid))
    deleteAlgoBase(path, appid, engineid, algoid, Some(offlineevalid))
  }

  def deleteAlgoBase(path: String, appid: Int, engineid: Int, algoid: Int, offlineevalid: Option[Int]) = {
    try {
      //fileSystem.delete(path, true)
      Ok(Json.obj("message" -> "Deleted HDFS storage for App ID: %d, Engine ID: %d, Algo ID: %d, OfflineEval ID: %s (%s)".format(appid, engineid, algoid, offlineevalid.getOrElse("N/A"), path)))
    } catch {
      case e: java.io.IOException => InternalServerError(Json.obj("message" -> "Unable to delete HDFS storage for App ID: %d, Engine ID: %d, Algo ID: %d, OfflineEval ID: %s (%s) due to an exception: %s".format(appid, engineid, algoid, offlineevalid.getOrElse("N/A"), path, e.getMessage)))
    }
  }
}

package io.prediction.commons.scalding.appdata

import com.twitter.scalding._

import cascading.flow.FlowDef
import cascading.tuple.Tuple

import io.prediction.commons.scalding.appdata.mongodb.{ MongoUsersSource, MongoItemsSource, MongoU2iActionsSource }
import io.prediction.commons.scalding.appdata.file.{ FileUsersSource, FileItemsSource, FileU2iActionsSource }

object Users {

  /**
   * dbName: used as file path in dbType=="file"
   */
  def apply(appId: Int, dbType: String, dbName: String, dbHost: Option[String], dbPort: Option[Int]): UsersSource = {
    dbType match {
      case "file" => {
        new FileUsersSource(dbName, appId)
      }
      case "mongodb" => {
        require(((dbHost != None) && (dbPort != None)), "Please specify host and port number for mongodb.")
        new MongoUsersSource(dbName, dbHost.get, dbPort.get, appId)
      }
      case _ => {
        throw new RuntimeException("Invalid Users database type: " + dbType)
      }
    }
  }

}

object Items {

  /**
   * dbName: used as file path in dbType=="file"
   */
  def apply(appId: Int, itypes: Option[List[String]], dbType: String, dbName: String, dbHost: Option[String], dbPort: Option[Int]): ItemsSource = {
    dbType match {
      case "file" => {
        new FileItemsSource(dbName, appId, itypes)
      }
      case "mongodb" => {
        require(((dbHost != None) && (dbPort != None)), "Please specify host and port number for mongodb.")
        new MongoItemsSource(dbName, dbHost.get, dbPort.get, appId, itypes)
      }
      case _ => {
        throw new RuntimeException("Invalid Items database type: " + dbType)
      }
    }
  }

}

object U2iActions {

  /**
   * dbName: used as file path in dbType=="file"
   */
  def apply(appId: Int, dbType: String, dbName: String, dbHost: Option[String], dbPort: Option[Int]): U2iActionsSource = {
    dbType match {
      case "file" => {
        new FileU2iActionsSource(dbName, appId)
      }
      case "mongodb" => {
        require(((dbHost != None) && (dbPort != None)), "Please specify host and port number for mongodb.")
        /*
        val opt = evalId.map(x => Map("evalid" -> x)).getOrElse(Map())
        val queryData = Map("appid" -> appId) ++ opt
        */
        new MongoU2iActionsSource(dbName, dbHost.get, dbPort.get, appId)
      }
      case _ => {
        throw new RuntimeException("Invalid U2iActions database type: " + dbType)
      }
    }
  }

}

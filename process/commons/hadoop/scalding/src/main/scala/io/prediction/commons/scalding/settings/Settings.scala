package io.prediction.commons.scalding.settings

import io.prediction.commons.scalding.settings.file.FileOfflineEvalResultsSource
import io.prediction.commons.scalding.settings.mongodb.MongoOfflineEvalResultsSource

object OfflineEvalResults {
  
  def apply(dbType: String, dbName: String, dbHost: Option[String], dbPort: Option[Int]): OfflineEvalResultsSource = {
    dbType match {
      case "file" => {
        new FileOfflineEvalResultsSource(dbName)
      }
      case "mongodb" => {
        require(((dbHost != None) && (dbPort != None)), "Please specify host and port number for mongodb.")
        new MongoOfflineEvalResultsSource(dbName, dbHost.get, dbPort.get)
      }
      case _ => {
        throw new RuntimeException("Invalid OfflineEvalResults database type: " + dbType)
      }
    }
  }
}
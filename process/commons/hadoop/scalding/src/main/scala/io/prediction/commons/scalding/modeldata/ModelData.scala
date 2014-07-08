package io.prediction.commons.scalding.modeldata

import io.prediction.commons.scalding.modeldata.mongodb.{ MongoItemRecScoresSource, MongoItemSimScoresSource }
import io.prediction.commons.scalding.modeldata.file.{ FileItemRecScoresSource, FileItemSimScoresSource }

object ItemSimScores {

  /**
   * dbName: used as file path in dbType=="file"
   */
  def apply(dbType: String, dbName: String, dbHost: Seq[String], dbPort: Seq[Int], algoid: Int, modelset: Boolean): ItemSimScoresSource = {
    dbType match {
      case "file" => {
        new FileItemSimScoresSource(dbName)
      }
      case "mongodb" => {
        require(((!dbHost.isEmpty) && (!dbPort.isEmpty)), "Please specify host and port number for mongodb.")
        new MongoItemSimScoresSource(dbName, dbHost, dbPort, algoid, modelset)
      }
      case _ => {
        throw new RuntimeException("Invalid ItemSimScores database type: " + dbType)
      }
    }
  }
}

object ItemRecScores {

  /**
   * dbName: used as file path in dbType=="file"
   */
  def apply(dbType: String, dbName: String, dbHost: Seq[String], dbPort: Seq[Int], algoid: Int, modelset: Boolean): ItemRecScoresSource = {
    dbType match {
      case "file" => {
        new FileItemRecScoresSource(dbName)
      }
      case "mongodb" => {
        require(((!dbHost.isEmpty) && (!dbPort.isEmpty)), "Please specify host and port number for mongodb.")
        new MongoItemRecScoresSource(dbName, dbHost, dbPort, algoid, modelset)
      }
      case _ => {
        throw new RuntimeException("Invalid ItemRecScores database type: " + dbType)
      }
    }
  }

}

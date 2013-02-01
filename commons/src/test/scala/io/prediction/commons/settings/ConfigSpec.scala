package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class ConfigSpec extends Specification { def is =
  "PredictionIO Config Specification"                                         ^
                                                                              p^
  "load an existing config file"                                              ! load()^
  "get raw config"                                                            ! dbtype()^
  "get a MongoUsers implementation"                                           ! getMongoUsers()^
  "get a MongoApps implementation"                                            ! getMongoApps()^
  "get a MongoEngines implementation"                                         ! getMongoEngines()^
  "get a MongoAlgos implementation"                                           ! getMongoAlgos()^
                                                                              Step(MongoConnection()(mongoConfig.settingsDbName).dropDatabase())
                                                                              end

  lazy val mongoConfig = new Config

  def load() = {
    mongoConfig must beAnInstanceOf[Config]
  }

  def dbtype() = {
    mongoConfig.settingsDbType must beEqualTo("mongodb")
  }

  def getMongoUsers() = {
    mongoConfig.getUsers() must beAnInstanceOf[mongodb.MongoUsers]
  }

  def getMongoApps() = {
    mongoConfig.getApps() must beAnInstanceOf[mongodb.MongoApps]
  }

  def getMongoEngines() = {
    mongoConfig.getEngines() must beAnInstanceOf[mongodb.MongoEngines]
  }

  def getMongoAlgos() = {
    mongoConfig.getAlgos() must beAnInstanceOf[mongodb.MongoAlgos]
  }
}

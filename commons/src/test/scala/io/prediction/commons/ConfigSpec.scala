package io.prediction.commons

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class ConfigSpec extends Specification {
  def is =
    "PredictionIO Config Specification" ^
      p ^
      "load an existing config file" ! load() ^
      "get raw config" ! dbtype() ^
      "get a MongoUsers implementation" ! getMongoUsers() ^
      "get a MongoApps implementation" ! getMongoApps() ^
      "get a MongoEngines implementation" ! getMongoEngines() ^
      "get a MongoAlgos implementation" ! getMongoAlgos() ^
      "get a list of job JARs" ! jars() ^
      "get sharding configuration" ! sharding() ^
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
    mongoConfig.getSettingsUsers() must beAnInstanceOf[settings.mongodb.MongoUsers]
  }

  def getMongoApps() = {
    mongoConfig.getSettingsApps() must beAnInstanceOf[settings.mongodb.MongoApps]
  }

  def getMongoEngines() = {
    mongoConfig.getSettingsEngines() must beAnInstanceOf[settings.mongodb.MongoEngines]
  }

  def getMongoAlgos() = {
    mongoConfig.getSettingsAlgos() must beAnInstanceOf[settings.mongodb.MongoAlgos]
  }

  def jars() = {
    mongoConfig.jars must havePairs(
      "algorithms.mahout.itemrec" -> "../lib/predictionio-process-itemrec-algorithms-scala-mahout-assembly-0.7.0-SNAPSHOT.jar",
      "algorithms.mahout.corejob" -> "../vendors/mahout-distribution-0.8/mahout-core-0.8-job.jar",
      "algorithms.scalding.itemrec.generic" -> "../lib/predictionio-process-itemrec-algorithms-hadoop-scalding-assembly-0.7.0-SNAPSHOT.jar")
  }

  def sharding() = {
    mongoConfig.modeldataDbSharding must beTrue and
      (mongoConfig.modeldataDbShardKeys must beSome(===(Seq("foo", "bar"))))
  }
}

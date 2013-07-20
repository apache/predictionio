package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class EngineInfosSpec extends Specification { def is =
  "PredictionIO EngineInfos Specification"                                    ^
                                                                              p^
  "EngineInfos can be implemented by:"                                        ^ endp^
    "1. MongoEngineInfos"                                                     ^ mongoEngineInfos^end

  def mongoEngineInfos =                                                      p^
    "MongoEngineInfos should"                                                 ^
      "behave like any EngineInfos implementation"                            ^ engineInfos(newMongoEngineInfos)^
                                                                              Step(MongoConnection()(mongoDbName).dropDatabase())

  def engineInfos(engineInfos: EngineInfos) = {                               t^
    "create and get an engine info"                                           ! insertAndGet(engineInfos)^
    "update an engine info"                                                   ! update(engineInfos)^
    "delete an engine info"                                                   ! delete(engineInfos)^
    "backup and restore existing engine info"                                 ! backuprestore(engineInfos)^
                                                                              bt
  }

  val mongoDbName = "predictionio_mongoengineinfos_test"
  def newMongoEngineInfos = new mongodb.MongoEngineInfos(MongoConnection()(mongoDbName))

  def insertAndGet(engineInfos: EngineInfos) = {
    val itemrec = EngineInfo(
      id = "itemrec",
      name = "Item Recommendation Engine",
      description = Some("Recommend interesting items to each user personally."),
      defaultsettings = Map[String, Any]("numRecs" -> 500),
      defaultalgoinfoid = "mahout-itembased")
    engineInfos.insert(itemrec)
    engineInfos.get("itemrec") must beSome(itemrec)
  }

  def update(engineInfos: EngineInfos) = {
    val itemsim = EngineInfo(
      id = "itemsim",
      name = "Items Similarity Prediction Engine",
      description = Some("Discover similar items."),
      defaultsettings = Map[String, Any](),
      defaultalgoinfoid = "knnitembased")
    engineInfos.insert(itemsim)
    val updatedItemsim = itemsim.copy(defaultalgoinfoid = "mahout-itembasedcf")
    engineInfos.update(updatedItemsim)
    engineInfos.get("itemsim") must beSome(updatedItemsim)
  }

  def delete(engineInfos: EngineInfos) = {
    val foo = EngineInfo(
      id = "foo",
      name = "bar",
      description = None,
      defaultsettings = Map[String, Any](),
      defaultalgoinfoid = "baz")
    engineInfos.insert(foo)
    engineInfos.delete("foo")
    engineInfos.get("foo") must beNone
  }

  def backuprestore(engineInfos: EngineInfos) = {
    val baz = EngineInfo(
      id = "baz",
      name = "beef",
      description = Some("dead"),
      defaultsettings = Map[String, Any]("abc" -> 123.4),
      defaultalgoinfoid = "bar")
    engineInfos.insert(baz)
    val fos = new java.io.FileOutputStream("engineinfos.bin")
    try {
      fos.write(engineInfos.backup())
    } finally {
      fos.close()
    }
    engineInfos.restore(scala.io.Source.fromFile("engineinfos.bin")(scala.io.Codec.ISO8859).map(_.toByte).toArray) map { rengineinfos =>
      rengineinfos must contain(baz)
    } getOrElse 1 === 2
  }
}

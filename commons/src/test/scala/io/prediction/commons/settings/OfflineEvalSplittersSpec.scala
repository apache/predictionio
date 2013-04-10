package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class OfflineEvalSplittersSpec extends Specification { def is =
  "PredictionIO OfflineEvalSplitters Specification"                           ^
                                                                              p^
  "OfflineEvalSplitters can be implemented by:"                               ^ endp^
    "1. MongoOfflineEvalSplitters"                                            ^ mongoOfflineEvalSplitters^end

  def mongoOfflineEvalSplitters =                                             p^
    "MongoOfflineEvalSplitters should"                                        ^
      "behave like any OfflineEvalSplitters implementation"                   ^ offlineEvalSplitters(newMongoOfflineEvalSplitters)^
                                                                              Step(MongoConnection()(mongoDbName).dropDatabase())

  def offlineEvalSplitters(splitters: OfflineEvalSplitters) = {               t^
    "create an OfflineEvalSplitter"                                           ! insert(splitters)^
    "update an OfflineEvalSplitter"                                           ! update(splitters)^
    "delete an OfflineEvalSplitter"                                           ! delete(splitters)^
                                                                              bt
  }

  val mongoDbName = "predictionio_mongoofflineevalsplitters_test"
  def newMongoOfflineEvalSplitters = new mongodb.MongoOfflineEvalSplitters(MongoConnection()(mongoDbName))

  def insert(splitters: OfflineEvalSplitters) = {
    val splitter = OfflineEvalSplitter(
      id = 0,
      evalid = 123,
      name = "insert",
      infoid = "insert",
      settings = Map())
    val i = splitters.insert(splitter)
    splitters.get(i) must beSome(splitter.copy(id = i))
  }

  def update(splitters: OfflineEvalSplitters) = {
    val id = splitters.insert(OfflineEvalSplitter(
      id = 0,
      evalid = 345,
      name = "update",
      infoid = "update",
      settings = Map()
    ))
    val updatedSplitter = OfflineEvalSplitter(
      id = id,
      evalid = 345,
      name = "updated",
      infoid = "updated",
      settings = Map("set1" -> "dat1", "set2" -> "dat2")
    )
    splitters.update(updatedSplitter)
    splitters.get(id) must beSome(updatedSplitter)
  }

  def delete(splitters: OfflineEvalSplitters) = {
    val id = splitters.insert(OfflineEvalSplitter(
      id = 0,
      evalid = 456,
      name = "deleteByIdAndAppid",
      infoid = "deleteByIdAndAppid",
      settings = Map("x" -> "y")
    ))
    splitters.delete(id)
    splitters.get(id) must beNone
  }
}

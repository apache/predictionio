package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class EnginesSpec extends Specification { def is =
  "PredictionIO Engines Specification"                                        ^
                                                                              p^
  "Engines can be implemented by:"                                            ^ endp^
    "1. MongoEngines"                                                         ^ mongoEngines^end

  def mongoEngines =                                                          p^
    "MongoEngines should"                                                     ^
      "behave like any Engines implementation"                                ^ engines(newMongoEngines)^
                                                                              Step(MongoConnection()(mongoDbName).dropDatabase())

  def engines(engines: Engines) = {                                           t^
    "create an engine"                                                        ! insert(engines)^
    "get two engines"                                                         ! getByAppid(engines)^
    "update an engine"                                                        ! update(engines)^
    "delete an engine"                                                        ! deleteByIdAndAppid(engines)^
    "checking existence of engines"                                           ! existsByAppidAndName(engines)^
                                                                              bt
  }

  val mongoDbName = "predictionio_mongoengines_test"
  def newMongoEngines = new mongodb.MongoEngines(MongoConnection()(mongoDbName))

  def insert(engines: Engines) = {
    val engine = Engine(
      id = 0,
      appid = 123,
      name = "insert",
      enginetype = "insert",
      itypes = Option(List("foo", "bar")),
      settings = Map()
    )
    val engineid = engines.insert(engine)
    engines.get(engineid) must beSome(engine.copy(id = engineid))
  }

  def getByAppid(engines: Engines) = {
    val obj1 = Engine(
      id = 0,
      appid = 234,
      name = "getByAppid1",
      enginetype = "getByAppid1",
      itypes = Option(List("foo", "bar")),
      settings = Map("apple" -> "red")
    )
    val obj2 = Engine(
      id = 0,
      appid = 234,
      name = "getByAppid2",
      enginetype = "getByAppid2",
      itypes = None,
      settings = Map("foo2" -> "bar2")
    )
    val id1 = engines.insert(obj1)
    val id2 = engines.insert(obj2)
    val engine12 = engines.getByAppid(234)
    val engine1 = engine12.next()
    val engine2 = engine12.next()
    engine1 must be equalTo(obj1.copy(id = id1)) and
      (engine2 must be equalTo(obj2.copy(id = id2)))
  }

  def update(engines: Engines) = {
    val id = engines.insert(Engine(
      id = 0,
      appid = 345,
      name = "update",
      enginetype = "update",
      itypes = Option(List("foo", "bar")),
      settings = Map()
    ))
    val updatedEngine = Engine(
      id = id,
      appid = 345,
      name = "updated",
      enginetype = "updated",
      itypes = Option(List("foo", "baz")),
      settings = Map("set1" -> "dat1", "set2" -> "dat2")
    )
    engines.update(updatedEngine)
    engines.getByAppidAndName(345, "updated") must beSome(updatedEngine)
  }

  def deleteByIdAndAppid(engines: Engines) = {
    val id = engines.insert(Engine(
      id = 0,
      appid = 456,
      name = "deleteByIdAndAppid",
      enginetype = "deleteByIdAndAppid",
      itypes = Option(List("foo", "bar")),
      settings = Map("x" -> "y")
    ))
    engines.deleteByIdAndAppid(id, 456)
    engines.getByAppidAndName(456, "deleteByIdAndAppid") must beNone
  }

  def existsByAppidAndName(engines: Engines) = {
    val id = engines.insert(Engine(
      id = 0,
      appid = 567,
      name = "existsByAppidAndName",
      enginetype = "existsByAppidAndName",
      itypes = None,
      settings = Map()
    ))
    engines.existsByAppidAndName(567, "existsByAppidAndName") must beTrue and
      (engines.existsByAppidAndName(568, "foobar") must beFalse)
  }
}
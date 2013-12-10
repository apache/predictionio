package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class EnginesSpec extends Specification {
  def is =
    "PredictionIO Engines Specification" ^
      p ^
      "Engines can be implemented by:" ^ endp ^
      "1. MongoEngines" ^ mongoEngines ^ end

  def mongoEngines = p ^
    "MongoEngines should" ^
    "behave like any Engines implementation" ^ engines(newMongoEngines) ^
    Step(MongoConnection()(mongoDbName).dropDatabase())

  def engines(engines: Engines) = {
    t ^
      "create an engine" ! insert(engines) ^
      "get two engines" ! getByAppid(engines) ^
      "get by id and appid" ! getByIdAndAppid(engines) ^
      "update an engine" ! update(engines) ^
      "delete an engine" ! deleteByIdAndAppid(engines) ^
      "checking existence of engines" ! existsByAppidAndName(engines) ^
      "backup and restore existing engines" ! backuprestore(engines) ^
      bt
  }

  val mongoDbName = "predictionio_mongoengines_test"
  def newMongoEngines = new mongodb.MongoEngines(MongoConnection()(mongoDbName))

  def insert(engines: Engines) = {
    val engine = Engine(
      id = 0,
      appid = 123,
      name = "insert",
      infoid = "insert",
      itypes = Option(List("foo", "bar")),
      params = Map()
    )
    val engineid = engines.insert(engine)
    engines.get(engineid) must beSome(engine.copy(id = engineid))
  }

  def getByAppid(engines: Engines) = {
    val obj1 = Engine(
      id = 0,
      appid = 234,
      name = "getByAppid1",
      infoid = "getByAppid1",
      itypes = Option(List("foo", "bar")),
      params = Map("apple" -> "red")
    )
    val obj2 = Engine(
      id = 0,
      appid = 234,
      name = "getByAppid2",
      infoid = "getByAppid2",
      itypes = None,
      params = Map("foo2" -> "bar2")
    )
    val id1 = engines.insert(obj1)
    val id2 = engines.insert(obj2)
    val engine12 = engines.getByAppid(234)
    val engine1 = engine12.next()
    val engine2 = engine12.next()
    engine1 must be equalTo (obj1.copy(id = id1)) and
      (engine2 must be equalTo (obj2.copy(id = id2)))
  }

  def getByIdAndAppid(engines: Engines) = {
    val obj1 = Engine(
      id = 0,
      appid = 2345,
      name = "getByIdAndAppid",
      infoid = "getByIdAndAppid",
      itypes = Option(List("foo", "bar")),
      params = Map("apple" -> "red")
    )
    val obj2 = obj1.copy()

    val obj3 = obj1.copy(appid = 2346, name = "getByIdAndAppid3")

    val id1 = engines.insert(obj1)
    val id2 = engines.insert(obj2)
    val id3 = engines.insert(obj3)
    val engine1 = engines.getByIdAndAppid(id1, 2345)
    val engine1b = engines.getByIdAndAppid(id1, 2346)
    val engine2 = engines.getByIdAndAppid(id2, 2345)
    val engine2b = engines.getByIdAndAppid(id2, 2346)
    val engine3b = engines.getByIdAndAppid(id3, 2345)
    val engine3 = engines.getByIdAndAppid(id3, 2346)

    engine1 must beSome(obj1.copy(id = id1)) and
      (engine1b must beNone) and
      (engine2 must beSome(obj2.copy(id = id2))) and
      (engine2b must beNone) and
      (engine3 must beSome(obj3.copy(id = id3))) and
      (engine3b must beNone)
  }

  def update(engines: Engines) = {
    val id = engines.insert(Engine(
      id = 0,
      appid = 345,
      name = "update",
      infoid = "update",
      itypes = Some(List("foo", "bar")),
      params = Map()
    ))
    val updatedEngine = Engine(
      id = id,
      appid = 345,
      name = "updated",
      infoid = "updated",
      itypes = Some(List("foo", "baz")),
      params = Map("set1" -> "dat1", "set2" -> "dat2")
    )
    engines.update(updatedEngine)
    engines.getByAppidAndName(345, "updated") must beSome(updatedEngine)
  }

  def deleteByIdAndAppid(engines: Engines) = {
    val id = engines.insert(Engine(
      id = 0,
      appid = 456,
      name = "deleteByIdAndAppid",
      infoid = "deleteByIdAndAppid",
      itypes = Some(List("foo", "bar")),
      params = Map("x" -> "y")
    ))
    engines.deleteByIdAndAppid(id, 456)
    engines.getByAppidAndName(456, "deleteByIdAndAppid") must beNone
  }

  def existsByAppidAndName(engines: Engines) = {
    val id = engines.insert(Engine(
      id = 0,
      appid = 567,
      name = "existsByAppidAndName",
      infoid = "existsByAppidAndName",
      itypes = None,
      params = Map()
    ))
    engines.existsByAppidAndName(567, "existsByAppidAndName") must beTrue and
      (engines.existsByAppidAndName(568, "foobar") must beFalse)
  }

  def backuprestore(engines: Engines) = {
    val eng = Engine(
      id = 0,
      appid = 678,
      name = "backuprestore",
      infoid = "backuprestore",
      itypes = Some(Seq("dead", "beef")),
      params = Map("foo" -> "bar")
    )
    val eid = engines.insert(eng)
    val fn = "engines.json"
    val fos = new java.io.FileOutputStream(fn)
    try {
      fos.write(engines.backup())
    } finally {
      fos.close()
    }
    engines.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.UTF8).mkString.getBytes("UTF-8")) map { rengines =>
      rengines must contain(eng.copy(id = eid))
    } getOrElse 1 === 2
  }
}
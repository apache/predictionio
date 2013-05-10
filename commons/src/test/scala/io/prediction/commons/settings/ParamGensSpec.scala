package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class ParamGensSpec extends Specification { def is =
  "PredictionIO ParamGens Specification"               ^
                                                                p^
  "ParamGens can be implemented by:"                   ^ endp^
    "1. MongoParamGens"                                ^ mongoParamGens^end

  def mongoParamGens =                                 p^
    "MongoParamGens should"                            ^
      "behave like any ParamGens implementation"       ^ paramGensTest(newMongoParamGens)^
                                                                Step(MongoConnection()(mongoDbName).dropDatabase())

  def paramGensTest(paramGens: ParamGens) = {    t^
    "create an ParamGen"                                           ! insert(paramGens)^
    "update an ParamGen"                                           ! update(paramGens)^
    "get two ParamGens by Tuneid"                                  ! getByTuneid(paramGens)^
    "delete an ParamGen"                                           ! delete(paramGens)^
                                                                            bt
  }

  val mongoDbName = "predictionio_mongoparamgens_test"
  def newMongoParamGens = new mongodb.MongoParamGens(MongoConnection()(mongoDbName))

  /**
   * insert and get by id
   */
  def insert(paramGens: ParamGens) = {
    val obj = ParamGen(
      id = -1,
      infoid = "paramGen-insert1",
      tuneid = 42,
      params = Map(("abc" -> 3), ("bar" -> "foo1 foo2"))
    )

    val insertid = paramGens.insert(obj)
    paramGens.get(insertid) must beSome(obj.copy(id = insertid))
  }

  /**
   * insert one and then update with new data and get back
   */
  def update(paramGens: ParamGens) = {
    val obj1 = ParamGen(
      id = -1,
      infoid = "paramGen-update1",
      tuneid = 16,
      params = Map(("def" -> "a1 a2 a3"), ("def2" -> 1), ("def3" -> "food"))
    )

    val updateid = paramGens.insert(obj1)
    val data1 = paramGens.get(updateid)

    val obj2 = obj1.copy(
      id = updateid,
      infoid = "paramGen-update2",
      tuneid = 99,
      params = Map()
    )

    paramGens.update(obj2)

    val data2 = paramGens.get(updateid)

    data1 must beSome(obj1.copy(id = updateid)) and
      (data2 must beSome(obj2))

  }

 /**
   * insert a few and get by offline tune id
   */
  def getByTuneid(paramGens: ParamGens) = {
    val obj1 = ParamGen(
      id = -1,
      infoid = "paramGen-update1",
      tuneid = 121,
      params = Map(("def" -> "a1 a2 a3"), ("def2" -> 1), ("def3" -> "food"))
    )
    val obj2 = ParamGen(
      id = -1,
      infoid = "paramGen-update1",
      tuneid = 121,
      params = Map(("def" -> "a1 a3 a4"), ("def2" -> 1), ("def3" -> "food"))
    )
    val obj3 = ParamGen(
      id = -1,
      infoid = "paramGen-update1",
      tuneid = 122, // diff tuneid
      params = Map(("def" -> "a1 a2 a3"), ("def2" -> 1), ("def3" -> "food"))
    )

    val id1 = paramGens.insert(obj1)
    val id2 = paramGens.insert(obj2)
    val id3 = paramGens.insert(obj3)

    val it = paramGens.getByTuneid(121)

    val it1 = it.next()
    val it2 = it.next()
    val left = it.hasNext // make sure it has 2 only

    it1 must be equalTo(obj1.copy(id = id1)) and
      (it2 must be equalTo(obj2.copy(id = id2))) and
      (left must be_==(false))

  }

  /**
   * insert one and delete and get back
   */
  def delete(paramGens: ParamGens) = {
    val obj1 = ParamGen(
      id = -1,
      infoid = "paramGen-delete1",
      tuneid = 3,
      params = Map(("x" -> 1))
    )

    val id1 = paramGens.insert(obj1)
    val data1 = paramGens.get(id1)

    paramGens.delete(id1)
    val data2 = paramGens.get(id1)

    data1 must beSome(obj1.copy(id = id1)) and
      (data2 must beNone)

  }

}
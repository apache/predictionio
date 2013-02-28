package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step

import com.mongodb.casbah.Imports._
import com.github.nscala_time.time.Imports._

class AlgosSpec extends Specification { def is =
  "PredictionIO Algos Specification"                                          ^
                                                                              p^
  "Algos can be implemented by:"                                              ^ endp^
    "1. MongoAlgos"                                                           ^ mongoAlgos^end

  def mongoAlgos =                                                            p^
    "MongoAlgos should"                                                       ^
      "behave like any Algos implementation"                                  ^ algos(newMongoAlgos)^
                                                                              Step(MongoConnection()(mongoDbName).dropDatabase())

  def algos(algos: Algos) = {                                                 t^
    "create an algo"                                                          ! insert(algos)^
    "get two algos by engineid"                                               ! getByEngineid(algos)^
    "get a deployed algo by engineid"                                         ! getDeployedByEngineid(algos)^
    "get two algos by offlineevalid"                                          ! getByOfflineEvalid(algos)^
    "update an algo"                                                          ! update(algos)^
    "delete an algo"                                                          ! delete(algos)^
    "checking existence of algo"                                              ! existsByEngineidAndName(algos)^
                                                                              bt
  }

  val mongoDbName = "predictionio_mongoalgos_test"
  def newMongoAlgos = new mongodb.MongoAlgos(MongoConnection()(mongoDbName))

  def insert(algos: Algos) = {
    val algo = Algo(
      id       = 0,
      engineid = 123,
      name     = "insert",
      infoid   = "abc",
      pkgname  = "dummy",
      deployed = false,
      command  = "insert",
      params   = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now.hour(4).minute(56).second(35),
      offlineevalid = None
    )
    val insertid = algos.insert(algo)
    algos.get(insertid) must beSome(algo.copy(id = insertid))
  }

  def getByEngineid(algos: Algos) = {
    val algo1 = Algo(
      id       = 0,
      engineid = 234,
      name     = "getByEngineid1",
      infoid   = "apple",
      pkgname  = "dummy",
      deployed = true,
      command  = "getByEngineid1",
      params   = Map("baz" -> "bah"),
      settings = Map("qwe" -> "rty"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now.hour(1).minute(2).second(3),
      offlineevalid = Some(2)
    )
    val algo2 = Algo(
      id       = 0,
      engineid = 234,
      name     = "getByEngineid2",
      infoid   = "abc2",
      pkgname  = "dummy",
      deployed = false,
      command  = "getByEngineid2",
      params   = Map("az" -> "ba"),
      settings = Map("we" -> "rt"),
      modelset = false,
      createtime = DateTime.now.hour(4).minute(5).second(6),
      updatetime = DateTime.now,
      offlineevalid = None
    )
    val id1 = algos.insert(algo1)
    val id2 = algos.insert(algo2)
    val algo12 = algos.getByEngineid(234)
    val algo121 = algo12.next()
    val algo122 = algo12.next()
    algo121 must be equalTo(algo1.copy(id = id1)) and
      (algo122 must be equalTo(algo2.copy(id = id2)))
  }

  def getDeployedByEngineid(algos: Algos) = {
    val algo1 = Algo(
      id       = 0,
      engineid = 567,
      name     = "getDeployedByEngineid1",
      infoid   = "def",
      pkgname  = "dummy",
      deployed = false,
      command  = "getDeployedByEngineid1",
      params   = Map("baz" -> "bah"),
      settings = Map("qwe" -> "rty"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = Some(2)
    )
    val algo2 = Algo(
      id       = 0,
      engineid = 567,
      name     = "getDeployedByEngineid2",
      infoid   = "id3",
      pkgname  = "dummy",
      deployed = true,
      command  = "getDeployedByEngineid2",
      params   = Map("az" -> "ba"),
      settings = Map("we" -> "rt"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = None
    )
    val id1 = algos.insert(algo1)
    val id2 = algos.insert(algo2)
    val algo12 = algos.getDeployedByEngineid(567)
    algo12.next must be equalTo(algo2.copy(id = id2)) and
      (algo12.hasNext must beFalse)
  }

  def getByOfflineEvalid(algos: Algos) = {
    val algo1 = Algo(
      id       = 0,
      engineid = 234,
      name     = "getByOfflineEvalid1",
      infoid   = "banana",
      pkgname  = "dummy",
      deployed = true,
      command  = "getByOfflineEvalid1",
      params   = Map("baz1" -> "bah1"),
      settings = Map("qwe1" -> "rty1"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = Some(20)
    )
    val algo2 = Algo(
      id       = 0,
      engineid = 233,
      name     = "getByOfflineEvalid2",
      infoid   = "banana2",
      pkgname  = "dummy",
      deployed = false,
      command  = "getByOfflineEvalid2",
      params   = Map("az2" -> "ba2"),
      settings = Map("we2" -> "rt2"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = Some(20)
    )
    val id1 = algos.insert(algo1)
    val id2 = algos.insert(algo2)
    val algo12 = algos.getByOfflineEvalid(20)
    val algo121 = algo12.next()
    val algo122 = algo12.next()
    algo121 must be equalTo(algo1.copy(id = id1)) and
      (algo122 must be equalTo(algo2.copy(id = id2)))
  }

  def update(algos: Algos) = {
    val algo = Algo(
      id       = 0,
      engineid = 345,
      name     = "update",
      infoid   = "food",
      pkgname  = "dummy",
      deployed = false,
      command  = "update",
      params   = Map("az" -> "ba"),
      settings = Map("we" -> "rt"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = None
    )
    val updateid = algos.insert(algo)
    val updatedAlgo = algo.copy(
      id       = updateid,
      name     = "updated",
      infoid   = "food2",
      pkgname  = "dummy",
      deployed = true,
      command  = "updated",
      params   = Map("def" -> "ghi"),
      settings = Map(),
      updatetime = DateTime.now.hour(2).minute(45).second(10),
      offlineevalid = Some(3)
    )
    algos.update(updatedAlgo)
    algos.get(updateid) must beSome(updatedAlgo)
  }

  def delete(algos: Algos) = {
    val id = algos.insert(Algo(
      id       = 0,
      engineid = 456,
      name     = "delete",
      infoid   = "abc4",
      pkgname  = "dummy",
      deployed = false,
      command  = "delete",
      params   = Map("az" -> "ba"),
      settings = Map("we" -> "rt"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = None
    ))
    algos.delete(id)
    algos.get(id) must beNone
  }

  def existsByEngineidAndName(algos: Algos) = {
    val id = algos.insert(Algo(
      id       = 0,
      engineid = 456,
      name     = "existsByEngineidAndName-1",
      infoid   = "abcdef",
      pkgname  = "dummy",
      deployed = false,
      command  = "delete",
      params   = Map("az" -> "ba"),
      settings = Map("we" -> "rt"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = None
    ))
    
    algos.existsByEngineidAndName(456, "existsByEngineidAndName-1") must beTrue and
      (algos.existsByEngineidAndName(456, "existsByEngineidAndName-2") must beFalse) and
      (algos.existsByEngineidAndName(457, "existsByEngineidAndName-1") must beFalse)
  }
}

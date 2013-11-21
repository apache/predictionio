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
    "get an auto tune subject"                                                ! getTuneSubjectByOfflineTuneid(algos)^
    "update an algo"                                                          ! update(algos)^
    "delete an algo"                                                          ! delete(algos)^
    "checking existence of algo"                                              ! existsByEngineidAndName(algos)^
    "backup and restore existing algos"                                       ! backuprestore(algos)^
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
      command  = "insert",
      params   = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now.hour(4).minute(56).second(35),
      status = "apple",
      offlineevalid = None,
      offlinetuneid = Some(134),
      loop = None,
      paramset = Some(4)
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
      command  = "getByEngineid1",
      params   = Map("baz" -> "bah"),
      settings = Map("qwe" -> "rty"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now.hour(1).minute(2).second(3),
      status = "orange",
      offlineevalid = Some(2),
      offlinetuneid = None,
      loop = Some(4),
      paramset = Some(123)
    )
    val algo2 = Algo(
      id       = 0,
      engineid = 234,
      name     = "getByEngineid2",
      infoid   = "abc2",
      command  = "getByEngineid2",
      params   = Map("az" -> "ba"),
      settings = Map("we" -> "rt"),
      modelset = false,
      createtime = DateTime.now.hour(4).minute(5).second(6),
      updatetime = DateTime.now,
      status = "abcdef",
      offlineevalid = None,
      offlinetuneid = Some(3),
      loop = Some(5),
      paramset = None
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
      command  = "getDeployedByEngineid1",
      params   = Map("baz" -> "bah"),
      settings = Map("qwe" -> "rty"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "good",
      offlineevalid = Some(2),
      offlinetuneid = Some(33),
      loop = None,
      paramset = Some(6)
    )
    val algo2 = Algo(
      id       = 0,
      engineid = 567,
      name     = "getDeployedByEngineid2",
      infoid   = "id3",
      command  = "getDeployedByEngineid2",
      params   = Map("az" -> "ba"),
      settings = Map("we" -> "rt"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed", // NOTE!
      offlineevalid = None,
      offlinetuneid = Some(44),
      loop = Some(3),
      paramset = Some(56)
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
      command  = "getByOfflineEvalid1",
      params   = Map("baz1" -> "bah1"),
      settings = Map("qwe1" -> "rty1"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "sleep",
      offlineevalid = Some(20),
      offlinetuneid = None,
      loop = None,
      paramset = Some(155)
    )
    val algo2 = Algo(
      id       = 0,
      engineid = 233,
      name     = "getByOfflineEvalid2",
      infoid   = "banana2",
      command  = "getByOfflineEvalid2",
      params   = Map("az2" -> "ba2"),
      settings = Map("we2" -> "rt2"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "start",
      offlineevalid = Some(20),
      offlinetuneid = Some(21),
      loop = Some(14),
      paramset = Some(1)
    )
    val id1 = algos.insert(algo1)
    val id2 = algos.insert(algo2)
    val algo12 = algos.getByOfflineEvalid(20)
    val algo121 = algo12.next()
    val algo122 = algo12.next()
    algo121 must be equalTo(algo1.copy(id = id1)) and
      (algo122 must be equalTo(algo2.copy(id = id2)))
  }

  def getTuneSubjectByOfflineTuneid(algos: Algos) = {
    val algo1 = Algo(
      id       = 0,
      engineid = 678,
      name     = "getTuneSubjectByTuneid1",
      infoid   = "def",
      command  = "getTuneSubjectByTuneid1",
      params   = Map("baz" -> "bah"),
      settings = Map("qwe" -> "rty"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "good",
      offlineevalid = Some(2),
      offlinetuneid = Some(567),
      loop = None,
      paramset = Some(6)
    )
    val algo2 = Algo(
      id       = 0,
      engineid = 678,
      name     = "getTuneSubjectByTuneid2",
      infoid   = "id3",
      command  = "getTuneSubjectByTuneid2",
      params   = Map("az" -> "ba"),
      settings = Map("we" -> "rt"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed", // NOTE!
      offlineevalid = None,
      offlinetuneid = Some(567),
      loop = None,
      paramset = None
    )
    val id1 = algos.insert(algo1)
    val id2 = algos.insert(algo2)
    algos.getTuneSubjectByOfflineTuneid(567) must beSome(algo2.copy(id = id2))
  }

  def update(algos: Algos) = {
    val algo = Algo(
      id       = 0,
      engineid = 345,
      name     = "update",
      infoid   = "food",
      command  = "update",
      params   = Map("az" -> "ba"),
      settings = Map("we" -> "rt"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "abc",
      offlineevalid = None,
      offlinetuneid = None,
      loop = None,
      paramset = None
    )
    val updateid = algos.insert(algo)
    val updatedAlgo = algo.copy(
      id       = updateid,
      name     = "updated",
      infoid   = "food2",
      command  = "updated",
      params   = Map("def" -> "ghi"),
      settings = Map(),
      updatetime = DateTime.now.hour(2).minute(45).second(10),
      status = "ready",
      offlineevalid = Some(3),
      offlinetuneid = Some(4),
      loop = Some(10),
      paramset = Some(9)
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
      command  = "delete",
      params   = Map("az" -> "ba"),
      settings = Map("we" -> "rt"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "ok",
      offlineevalid = None,
      offlinetuneid = Some(34),
      loop = None,
      paramset = Some(42)
    ))
    algos.delete(id)
    algos.get(id) must beNone
  }

  def existsByEngineidAndName(algos: Algos) = {

    val algo1 = Algo(
      id       = 0,
      engineid = 456,
      name     = "existsByEngineidAndName-1",
      infoid   = "abcdef",
      command  = "delete",
      params   = Map("az" -> "ba"),
      settings = Map("we" -> "rt"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "food",
      offlineevalid = None,
      offlinetuneid = None,
      loop = None,
      paramset = None
    )

    val id1 = algos.insert(algo1)

    // algo with offlineevalid, existence is ignored
    algos.insert(algo1.copy(
      name = "existsByEngineidAndName-1a",
      offlineevalid = Some(5)
    ))
    // algo with offlinetuneid, existence is not ignored
    algos.insert(algo1.copy(
      name = "existsByEngineidAndName-1b",
      offlinetuneid = Some(6)
    ))
    algos.existsByEngineidAndName(456, "existsByEngineidAndName-1") must beTrue and // match engineid and name
      (algos.existsByEngineidAndName(456, "existsByEngineidAndName-2") must beFalse) and // same engineid, diff name
      (algos.existsByEngineidAndName(457, "existsByEngineidAndName-1") must beFalse) and // diff engineid, same name
      (algos.existsByEngineidAndName(456, "existsByEngineidAndName-1a") must beFalse) and // algo with offlineevalid
      (algos.existsByEngineidAndName(456, "existsByEngineidAndName-1b") must beTrue) // algo with offlinetuneid
  }

  def backuprestore(algos: Algos) = {
    val algo1 = Algo(
      id       = 0,
      engineid = 456,
      name     = "backuprestore-1",
      infoid   = "abcdef",
      command  = "delete",
      params   = Map("az" -> "ba"),
      settings = Map("we" -> "rt"),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "food",
      offlineevalid = None,
      offlinetuneid = Some(3),
      loop = None,
      paramset = None)
    val algo2 = Algo(
      id       = 0,
      engineid = 5839,
      name     = "backuprestore-2",
      infoid   = "abcdef",
      command  = "delete",
      params   = Map("az" -> "ba"),
      settings = Map(),
      modelset = false,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "tuned",
      offlineevalid = Some(43),
      offlinetuneid = Some(3),
      loop = None,
      paramset = Some(5))
    val id1 = algos.insert(algo1)
    val id2 = algos.insert(algo2)
    val ralgo1 = algo1.copy(id = id1)
    val ralgo2 = algo2.copy(id = id2)
    val fos = new java.io.FileOutputStream("algos.bin")
    try {
      fos.write(algos.backup())
    } finally {
      fos.close()
    }
    algos.restore(scala.io.Source.fromFile("algos.bin")(scala.io.Codec.ISO8859).map(_.toByte).toArray) map { ralgos =>
      val falgo1 = ralgos.find(_.id == id1).get
      val falgo2 = ralgos.find(_.id == id2).get
      algos.update(falgo1)
      algos.update(falgo2)
      (algos.get(id1) must beSome(ralgo1)) and (algos.get(id2) must beSome(ralgo2))
    } getOrElse 1 === 2
  }
}

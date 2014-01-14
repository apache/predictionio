package io.prediction.commons.modeldata

import io.prediction.commons.Config
import io.prediction.commons.settings.{ Algo, App }

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._
import com.github.nscala_time.time.Imports._

class ItemRecScoresSpec extends Specification {
  def is =
    "PredictionIO Model Data Item Recommendation Scores Specification" ^
      p ^
      "ItemRecScores can be implemented by:" ^ endp ^
      "1. MongoItemRecScores" ^ mongoItemRecScores ^ end

  def mongoItemRecScores = p ^
    "MongoItemRecScores should" ^
    "behave like any ItemRecScores implementation" ^ itemRecScores(newMongoItemRecScores) ^
    Step(MongoConnection()(mongoDbName).dropDatabase())

  def itemRecScores(itemRecScores: ItemRecScores) = {
    t ^
      "inserting and getting 3 ItemRecScores" ! insert(itemRecScores) ^
      "getting Top N Iids" ! getTopNIids(itemRecScores) ^
      "delete ItemRecScores by algoid" ! deleteByAlgoid(itemRecScores) ^
      "existence by Algo" ! existByAlgo(itemRecScores) ^
      bt
  }

  val mongoDbName = "predictionio_modeldata_mongoitemrecscore_test"

  def newMongoItemRecScores = new mongodb.MongoItemRecScores(new Config, MongoConnection()(mongoDbName))

  def insert(itemRecScores: ItemRecScores) = {
    implicit val app = App(
      id = 0,
      userid = 0,
      appkey = "",
      display = "",
      url = None,
      cat = None,
      desc = None,
      timezone = "UTC"
    )
    implicit val algo = Algo(
      id = 110101,
      engineid = 0,
      name = "",
      infoid = "abc",
      command = "",
      params = Map(),
      settings = Map(),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None,
      offlinetuneid = None,
      loop = None,
      paramset = None
    )
    val itemScores = List(ItemRecScore(
      uid = "testUser",
      iids = Seq("testUserItem4", "testUserItem3", "testUserItem2", "testUserItem1"),
      scores = Seq(999, 124.678, 10, -5.6),
      itypes = Seq(List("invalid"), List("7", "8", "9"), List("4", "5", "6"), List("1", "2", "3")),
      appid = app.id,
      algoid = algo.id,
      modelset = algo.modelset
    ), ItemRecScore(
      uid = "testUser2",
      iids = Seq("b", "c", "d", "e"),
      scores = Seq(8, 5.4, 2, 1),
      itypes = Seq(List("invalid"), List("7"), List("6"), List("1", "2", "3")),
      appid = app.id,
      algoid = algo.id,
      modelset = algo.modelset
    ), ItemRecScore(
      uid = "testUser3",
      iids = Seq("b", "c", "e", "s"),
      scores = Seq(999, 124.678, 10, -5.6),
      itypes = Seq(List("1"), List("7", "8", "9"), List("4", "5", "6"), List("1")),
      appid = app.id,
      algoid = algo.id,
      modelset = algo.modelset
    ))
    val dbItemScores = itemScores map {
      itemRecScores.insert(_)
    }
    val results = itemRecScores.getByUid("testUser")
    val results2 = itemRecScores.getByUid("testUser2")
    val results3 = itemRecScores.getByUid("testUser3")

    results must beSome(dbItemScores(0)) and
      (results2 must beSome(dbItemScores(1))) and
      (results3 must beSome(dbItemScores(2)))
  }

  def getTopNIids(itemRecScores: ItemRecScores) = {
    implicit val app = App(
      id = 234,
      userid = 0,
      appkey = "",
      display = "",
      url = None,
      cat = None,
      desc = None,
      timezone = "UTC"
    )
    implicit val algo = Algo(
      id = 234,
      engineid = 0,
      name = "",
      infoid = "abc",
      command = "",
      params = Map(),
      settings = Map(),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None,
      offlinetuneid = None,
      loop = None,
      paramset = None
    )
    val itemScores = List(ItemRecScore(
      uid = "testUser",
      iids = Seq("testUserItem10", "testUserItem8", "testUserItem4", "testUserItem9", "testUserItem7", "testUserItem3", "testUserItem2", "testUserItem6", "testUserItem1", "testUserItem5"),
      scores = Seq(10000, 999, 999, 124.678, 124.678, 124.678, 10, 10, -5.6, -5.6),
      itypes = Seq(List("invalid"), List("invalid"), List("invalid"), List("1", "2", "3"), List("1", "2", "4"), List("3"), List("5", "6", "7"), List("5", "6", "8"), List("1", "2", "3"), List("1", "2", "3")),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ))

    val dbItemScores = itemScores map {
      itemRecScores.insert(_)
    }

    val resultsAllTop5 = itemRecScores.getTopNIids("testUser", 5, None).toSeq
    val resultsAllTop1 = itemRecScores.getTopNIids("testUser", 1, None).toSeq
    val resultsAllTop0 = itemRecScores.getTopNIids("testUser", 0, None).toSeq
    val results23Top4 = itemRecScores.getTopNIids("testUser", 4, Some(List("2", "3"))).toSeq
    val results23Top100 = itemRecScores.getTopNIids("testUser", 100, Some(List("2", "3"))).toSeq
    val results8Top4 = itemRecScores.getTopNIids("testUser", 4, Some(List("8"))).toSeq
    val results8Top0 = itemRecScores.getTopNIids("testUser", 0, Some(List("8"))).toSeq
    val resultUnknownAllTop4 = itemRecScores.getTopNIids("unknown", 4, None).toSeq
    val resultUnknown18Top4 = itemRecScores.getTopNIids("unknown", 4, Some(List("1", "8"))).toSeq

    resultsAllTop5 must beEqualTo(Seq("testUserItem10", "testUserItem8", "testUserItem4", "testUserItem9", "testUserItem7")) and
      (resultsAllTop1 must beEqualTo(Seq("testUserItem10"))) and
      (resultsAllTop0 must beEqualTo(Seq("testUserItem10", "testUserItem8", "testUserItem4", "testUserItem9", "testUserItem7", "testUserItem3", "testUserItem2", "testUserItem6", "testUserItem1", "testUserItem5"))) and
      (results23Top4 must beEqualTo(Seq("testUserItem9", "testUserItem7", "testUserItem3", "testUserItem1"))) and
      (results23Top100 must beEqualTo(Seq("testUserItem9", "testUserItem7", "testUserItem3", "testUserItem1", "testUserItem5"))) and
      (results8Top4 must beEqualTo(Seq("testUserItem6"))) and
      (results8Top0 must beEqualTo(Seq("testUserItem6"))) and
      (resultUnknownAllTop4 must beEqualTo(Seq())) and
      (resultUnknown18Top4 must beEqualTo(Seq()))

  }

  def deleteByAlgoid(itemRecScores: ItemRecScores) = {

    implicit val app = App(
      id = 0,
      userid = 0,
      appkey = "",
      display = "",
      url = None,
      cat = None,
      desc = None,
      timezone = "UTC"
    )

    val algo1 = Algo(
      id = 1,
      engineid = 0,
      name = "algo1",
      infoid = "abc",
      command = "",
      params = Map(),
      settings = Map(),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None,
      offlinetuneid = None,
      loop = None,
      paramset = None
    )

    val algo2 = algo1.copy(id = 2) // NOTE: different id

    val itemScores1 = List(ItemRecScore(
      uid = "deleteByAlgoidUser",
      iids = Seq("testUserItem10", "testUserItem8", "testUserItem4", "testUserItem9", "testUserItem7", "testUserItem3", "testUserItem2", "testUserItem6", "testUserItem1", "testUserItem5"),
      scores = Seq(10000, 999, 999, 124.678, 124.678, 124.678, 10, 10, -5.6, -5.6),
      itypes = Seq(List("invalid"), List("invalid"), List("invalid"), List("1", "2", "3"), List("1", "2", "4"), List("2", "3", "4"), List("5", "6", "7"), List("5", "6", "8"), List("1", "2", "3"), List("1", "2", "3")),
      appid = app.id,
      algoid = algo1.id,
      modelset = algo1.modelset
    ), ItemRecScore(
      uid = "deleteByAlgoidUser2",
      iids = Seq("a", "b", "c", "d"),
      scores = Seq(10, 9, 8, 7),
      itypes = Seq(List("invalid"), List("5", "6", "7"), List("5", "6", "8"), List("4")),
      appid = app.id,
      algoid = algo1.id,
      modelset = algo1.modelset
    ))

    val itemScores2 = List(ItemRecScore(
      uid = "deleteByAlgoidUser",
      iids = Seq("testUserItem10", "testUserItem8", "testUserItem4", "testUserItem9", "testUserItem7", "testUserItem3", "testUserItem2", "testUserItem6", "testUserItem1", "testUserItem5"),
      scores = Seq(10000, 999, 999, 124.678, 124.678, 124.678, 10, 10, -5.6, -5.6),
      itypes = Seq(List("invalid"), List("invalid"), List("invalid"), List("1", "2", "3"), List("1", "2", "4"), List("2", "3", "4"), List("5", "6", "7"), List("5", "6", "8"), List("1", "2", "3"), List("1", "2", "3")),
      appid = app.id,
      algoid = algo2.id,
      modelset = algo2.modelset
    ), ItemRecScore(
      uid = "deleteByAlgoidUser2",
      iids = Seq("a", "b", "c", "d"),
      scores = Seq(10, 9, 8, 7),
      itypes = Seq(List("invalid"), List("5", "6", "7"), List("5", "6", "8"), List("4")),
      appid = app.id,
      algoid = algo2.id,
      modelset = algo2.modelset
    ))

    val dbItemScores1 = itemScores1 map {
      itemRecScores.insert(_)
    }

    val dbItemScores2 = itemScores2 map {
      itemRecScores.insert(_)
    }

    val results1 = itemRecScores.getByUid("deleteByAlgoidUser")(app, algo1)
    val results1u2 = itemRecScores.getByUid("deleteByAlgoidUser2")(app, algo1)
    val results2 = itemRecScores.getByUid("deleteByAlgoidUser")(app, algo2)
    val results2u2 = itemRecScores.getByUid("deleteByAlgoidUser2")(app, algo2)

    itemRecScores.deleteByAlgoid(algo1.id)

    val results1b = itemRecScores.getByUid("deleteByAlgoidUser")(app, algo1)
    val results1bu2 = itemRecScores.getByUid("deleteByAlgoidUser2")(app, algo1)
    val results2b = itemRecScores.getByUid("deleteByAlgoidUser")(app, algo2)
    val results2bu2 = itemRecScores.getByUid("deleteByAlgoidUser2")(app, algo2)

    itemRecScores.deleteByAlgoid(algo2.id)

    val results2c = itemRecScores.getByUid("deleteByAlgoidUser")(app, algo2)
    val results2cu2 = itemRecScores.getByUid("deleteByAlgoidUser2")(app, algo2)

    results1 must beSome(dbItemScores1(0)) and
      (results1u2 must beSome(dbItemScores1(1))) and
      (results2 must beSome(dbItemScores2(0))) and
      (results2u2 must beSome(dbItemScores2(1))) and
      (results1b must beNone) and
      (results1bu2 must beNone) and
      (results2b must beSome(dbItemScores2(0))) and
      (results2bu2 must beSome(dbItemScores2(1))) and
      (results2c must beNone) and
      (results2cu2 must beNone)

  }

  def existByAlgo(itemRecScores: ItemRecScores) = {
    implicit val app = App(
      id = 345,
      userid = 0,
      appkey = "",
      display = "",
      url = None,
      cat = None,
      desc = None,
      timezone = "UTC"
    )
    val algo1 = Algo(
      id = 345,
      engineid = 0,
      name = "",
      infoid = "dummy",
      command = "",
      params = Map(),
      settings = Map(),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None,
      offlinetuneid = None,
      loop = None,
      paramset = None
    )
    val algo2 = algo1.copy(id = 3456)
    itemRecScores.insert(ItemRecScore(
      uid = "testUser",
      iids = Seq("testUserItem4"),
      scores = Seq(999),
      itypes = Seq(List("invalid")),
      appid = app.id,
      algoid = algo1.id,
      modelset = algo1.modelset
    ))
    itemRecScores.existByAlgo(algo1) must beTrue and
      (itemRecScores.existByAlgo(algo2) must beFalse)
  }
}

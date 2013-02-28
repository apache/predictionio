package io.prediction.commons.modeldata

import io.prediction.commons.settings.{Algo, App}

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._
import com.github.nscala_time.time.Imports._
import scala.Some

/**
 * Created with IntelliJ IDEA.
 * User: Cong
 * Date: 1/29/13
 * Time: 10:15 PM
 * To change this template use File | Settings | File Templates.
 */
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
      "inserting and getting 3 ItemRecScores"     ! insert(itemRecScores) ^
      "getting 4+4+2 ItemRecScores"               ! getTopN(itemRecScores) ^
      "delete ItemRecScores by algoid"            ! deleteByAlgoid(itemRecScores) ^
      "existence by Algo"                         ! existByAlgo(itemRecScores) ^
      bt
  }

  val mongoDbName = "predictionio_modeldata_mongoitemrecscore_test"

  def newMongoItemRecScores = new mongodb.MongoItemRecScores(MongoConnection()(mongoDbName))

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
      id = 1,
      engineid = 0,
      name = "",
      infoid = "abc",
      deployed = true,
      command = "",
      params = Map(),
      settings = Map(),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = None
    )
    val itemScores = List(ItemRecScore(
      uid = "testUser",
      iid = "testUserItem1",
      score = -5.6,
      itypes = List("1","2","3"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem2",
      score = 10,
      itypes = List("4","5","6"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem3",
      score = 124.678,
      itypes = List("7","8","9"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem4",
      score = 999,
      itypes = List("invalid"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ))
    val dbItemScores = itemScores map {
      itemRecScores.insert(_)
    }
    val results = itemRecScores.getTopN("testUser", 4, Some(List("1", "5", "9")), None)
    val r1 = results.next
    val r2 = results.next
    val r3 = results.next()
    results.hasNext must beFalse and
      (r1 must beEqualTo(dbItemScores(2))) and
      (r2 must beEqualTo(dbItemScores(1))) and
      (r3 must beEqualTo(dbItemScores(0)))
  }

  def getTopN(itemRecScores: ItemRecScores) = {
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
      deployed = true,
      command = "",
      params = Map(),
      settings = Map(),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = None
    )
    val itemScores = List(ItemRecScore(
      uid = "testUser",
      iid = "testUserItem1",
      score = -5.6,
      itypes = List("1","2","3"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem2",
      score = 10,
      itypes = List("4","5","6"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem3",
      score = 124.678,
      itypes = List("7","8","9"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem4",
      score = 999,
      itypes = List("invalid"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem5",
      score = -5.6,
      itypes = List("1","2","3"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem6",
      score = 10,
      itypes = List("4","5","6"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem7",
      score = 124.678,
      itypes = List("7","8","9"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem8",
      score = 999,
      itypes = List("invalid"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem9",
      score = 124.678,
      itypes = List("7","8","9"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem10",
      score = 999,
      itypes = List("invalid"),
      appid = app.id,
      algoid = algo.id,
      modelset = true
    ))
    val dbItemScores = itemScores map {
      itemRecScores.insert(_)
    }
    val results1234 = itemRecScores.getTopN("testUser", 4, Some(List("invalid", "8", "7", "6", "4", "3", "2", "1", "5", "9")), None)
    val r1 = results1234.next
    val r2 = results1234.next
    val r3 = results1234.next
    val r4 = results1234.next
    val results5678 = itemRecScores.getTopN("testUser", 4, Some(List("invalid", "8", "7", "6", "4", "3", "2", "1", "5", "9")), Some(r4))
    val r5 = results5678.next
    val r6 = results5678.next
    val r7 = results5678.next
    val r8 = results5678.next
    val results910 = itemRecScores.getTopN("testUser", 4, Some(List("invalid", "8", "7", "6", "4", "3", "2", "1", "5", "9")), Some(r8))
    val r9 = results910.next
    val r10 = results910.next
    results1234.hasNext must beFalse and
      (r1 must beEqualTo(dbItemScores(3))) and
      (r2 must beEqualTo(dbItemScores(7))) and
      (r3 must beEqualTo(dbItemScores(9))) and
      (r4 must beEqualTo(dbItemScores(2))) and
      (results5678.hasNext must beFalse) and
      (r5 must beEqualTo(dbItemScores(6))) and
      (r6 must beEqualTo(dbItemScores(8))) and
      (r7 must beEqualTo(dbItemScores(1))) and
      (r8 must beEqualTo(dbItemScores(5))) and
      (results910.hasNext must beFalse) and
      (r9 must beEqualTo(dbItemScores(0))) and
      (r10 must beEqualTo(dbItemScores(4)))
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
      deployed = true,
      command = "",
      params = Map(),
      settings = Map(),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = None
    )

    val algo2 = algo1.copy(id = 2) // NOTE: different id

    val itemScores1 = List(ItemRecScore(
      uid = "deleteByAlgoidUser",
      iid = "testUserItem1",
      score = -5.6,
      itypes = List("1","2","3"),
      appid = app.id,
      algoid = algo1.id,
      modelset = algo1.modelset
    ), ItemRecScore(
      uid = "deleteByAlgoidUser",
      iid = "testUserItem2",
      score = 10,
      itypes = List("4","5","6"),
      appid = app.id,
      algoid = algo1.id,
      modelset = algo1.modelset
    ), ItemRecScore(
      uid = "deleteByAlgoidUser",
      iid = "testUserItem3",
      score = 124.678,
      itypes = List("7","8","9"),
      appid = app.id,
      algoid = algo1.id,
      modelset = algo1.modelset
    ), ItemRecScore(
      uid = "deleteByAlgoidUser",
      iid = "testUserItem4",
      score = 999,
      itypes = List("invalid"),
      appid = app.id,
      algoid = algo1.id,
      modelset = algo1.modelset
    ))

    val itemScores2 = List(ItemRecScore(
      uid = "deleteByAlgoidUser",
      iid = "testUserItem1",
      score = 3,
      itypes = List("1","2","3"),
      appid = app.id,
      algoid = algo2.id,
      modelset = algo2.modelset
    ), ItemRecScore(
      uid = "deleteByAlgoidUser",
      iid = "testUserItem2",
      score = 2,
      itypes = List("4","5","6"),
      appid = app.id,
      algoid = algo2.id,
      modelset = algo2.modelset
    ), ItemRecScore(
      uid = "deleteByAlgoidUser",
      iid = "testUserItem3",
      score = 1,
      itypes = List("7","8","9"),
      appid = app.id,
      algoid = algo2.id,
      modelset = algo2.modelset
    ), ItemRecScore(
      uid = "deleteByAlgoidUser",
      iid = "testUserItem4",
      score = 0,
      itypes = List("invalid"),
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

    val results1 = itemRecScores.getTopN("deleteByAlgoidUser", 4, None, None)(app, algo1)
    val r1r1 = results1.next
    val r1r2 = results1.next
    val r1r3 = results1.next
    val r1r4 = results1.next

    val results2 = itemRecScores.getTopN("deleteByAlgoidUser", 4, None, None)(app, algo2)
    val r2r1 = results2.next
    val r2r2 = results2.next
    val r2r3 = results2.next
    val r2r4 = results2.next

    itemRecScores.deleteByAlgoid(algo1.id)

    val results1b = itemRecScores.getTopN("deleteByAlgoidUser", 4, None, None)(app, algo1)

    val results2b = itemRecScores.getTopN("deleteByAlgoidUser", 4, None, None)(app, algo2)
    val r2br1 = results2b.next
    val r2br2 = results2b.next
    val r2br3 = results2b.next
    val r2br4 = results2b.next

    itemRecScores.deleteByAlgoid(algo2.id)
    val results2c = itemRecScores.getTopN("deleteByAlgoidUser", 4, None, None)(app, algo2)

    results1.hasNext must beFalse and
      (r1r1 must beEqualTo(dbItemScores1(3))) and
      (r1r2 must beEqualTo(dbItemScores1(2))) and
      (r1r3 must beEqualTo(dbItemScores1(1))) and
      (r1r4 must beEqualTo(dbItemScores1(0))) and
      (results2.hasNext must beFalse) and
      (r2r1 must beEqualTo(dbItemScores2(0))) and
      (r2r2 must beEqualTo(dbItemScores2(1))) and
      (r2r3 must beEqualTo(dbItemScores2(2))) and
      (r2r4 must beEqualTo(dbItemScores2(3))) and
      (results1b.hasNext must beFalse) and
      (results2b.hasNext must beFalse) and
      (r2br1 must beEqualTo(dbItemScores2(0))) and
      (r2br2 must beEqualTo(dbItemScores2(1))) and
      (r2br3 must beEqualTo(dbItemScores2(2))) and
      (r2br4 must beEqualTo(dbItemScores2(3))) and
      (results2c.hasNext must beFalse)
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
      deployed = true,
      command = "",
      params = Map(),
      settings = Map(),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = None,
      infoid = "dummy"
    )
    val algo2 = algo1.copy(id = 3456)
    itemRecScores.insert(ItemRecScore(
      uid = "testUser",
      iid = "testUserItem4",
      score = 999,
      itypes = List("invalid"),
      appid = app.id,
      algoid = algo1.id,
      modelset = algo1.modelset
    ))
    itemRecScores.existByAlgo(algo1) must beTrue and
      (itemRecScores.existByAlgo(algo2) must beFalse)
  }
}

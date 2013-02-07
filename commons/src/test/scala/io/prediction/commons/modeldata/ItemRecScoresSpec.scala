package io.prediction.commons.modeldata

import io.prediction.commons.settings.{Algo, App}

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._
import org.scala_tools.time.Imports._
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
      "inserting and getting 3 ItemRecScores" ! insert(itemRecScores) ^
      "delete ItemRecScores by algoid" ! deleteByAlgoid(itemRecScores) ^
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
      pkgname = "",
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
      appid = 0,
      algoid = 1,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem2",
      score = 10,
      itypes = List("4","5","6"),
      appid = 0,
      algoid = 1,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem3",
      score = 124.678,
      itypes = List("7","8","9"),
      appid = 0,
      algoid = 1,
      modelset = true
    ), ItemRecScore(
      uid = "testUser",
      iid = "testUserItem4",
      score = 999,
      itypes = List("invalid"),
      appid = 0,
      algoid = 1,
      modelset = true
    ))
    itemScores foreach {
      itemRecScores.insert(_)
    }
    val results = itemRecScores.getTopN("testUser", 4, Some(List("1", "5", "9")))
    val r1 = results.next
    val r2 = results.next
    val r3 = results.next()
    results.hasNext must beFalse and
      (r1 must beEqualTo(itemScores(2))) and
      (r2 must beEqualTo(itemScores(1))) and
      (r3 must beEqualTo(itemScores(0)))
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
      pkgname = "pkgname1",
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
    
    itemScores1 foreach {
      itemRecScores.insert(_)
    }
    
    itemScores2 foreach {
      itemRecScores.insert(_)
    }
    
    val results1 = itemRecScores.getTopN("deleteByAlgoidUser", 4, None)(app, algo1)
    val r1r1 = results1.next
    val r1r2 = results1.next
    val r1r3 = results1.next
    val r1r4 = results1.next
    
    val results2 = itemRecScores.getTopN("deleteByAlgoidUser", 4, None)(app, algo2)
    val r2r1 = results2.next
    val r2r2 = results2.next
    val r2r3 = results2.next
    val r2r4 = results2.next
    
    itemRecScores.deleteByAlgoid(algo1.id)
    
    val results1b = itemRecScores.getTopN("deleteByAlgoidUser", 4, None)(app, algo1)
    
    val results2b = itemRecScores.getTopN("deleteByAlgoidUser", 4, None)(app, algo2)
    val r2br1 = results2b.next
    val r2br2 = results2b.next
    val r2br3 = results2b.next
    val r2br4 = results2b.next
    
    itemRecScores.deleteByAlgoid(algo2.id)
    val results2c = itemRecScores.getTopN("deleteByAlgoidUser", 4, None)(app, algo2)
    
    results1.hasNext must beFalse and
      (r1r1 must beEqualTo(itemScores1(3))) and
      (r1r2 must beEqualTo(itemScores1(2))) and
      (r1r3 must beEqualTo(itemScores1(1))) and
      (r1r4 must beEqualTo(itemScores1(0))) and
      (results2.hasNext must beFalse) and
      (r2r1 must beEqualTo(itemScores2(0))) and
      (r2r2 must beEqualTo(itemScores2(1))) and
      (r2r3 must beEqualTo(itemScores2(2))) and
      (r2r4 must beEqualTo(itemScores2(3))) and
      (results1b.hasNext must beFalse) and
      (results2b.hasNext must beFalse) and
      (r2br1 must beEqualTo(itemScores2(0))) and
      (r2br2 must beEqualTo(itemScores2(1))) and
      (r2br3 must beEqualTo(itemScores2(2))) and
      (r2br4 must beEqualTo(itemScores2(3))) and
      (results2c.hasNext must beFalse)
  }
}

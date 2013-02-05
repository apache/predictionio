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
}

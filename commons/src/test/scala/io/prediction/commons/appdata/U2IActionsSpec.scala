package io.prediction.commons.appdata

import org.specs2._
import org.specs2.specification.Step

import com.mongodb.casbah.Imports._
import org.scala_tools.time.Imports._

class U2IActionsSpec extends Specification { def is =
  "PredictionIO App Data User-to-item Actions Specification"                  ^
                                                                              p ^
  "U2IActions can be implemented by:"                                         ^ endp ^
    "1. MongoU2IActions"                                                      ^ mongoU2IActions ^ end

  def mongoU2IActions =                                                       p ^
    "MongoU2IActions should"                                                  ^
      "behave like any U2IActions implementation"                             ^ u2iActions(newMongoU2IActions) ^
                                                                              Step(MongoConnection()(mongoDbName).dropDatabase())

  def u2iActions(u2iActions: U2IActions) = {                                  t ^
    "inserting and getting 3 U2IAction's"                                     ! insert(u2iActions) ^
                                                                              bt
  }

  val mongoDbName = "predictionio_appdata_mongou2iactions_test"
  def newMongoU2IActions = new mongodb.MongoU2IActions(MongoConnection()(mongoDbName))

  def insert(u2iActions: U2IActions) = {
    val appid = 0
    val actions = List(U2IAction(
      appid  = appid,
      action = u2iActions.rate,
      uid    = "dead",
      iid    = "meat",
      t      = DateTime.now,
      latlng = None,
      v      = Some(3),
      price  = None,
      evalid = None
    ), U2IAction(
      appid  = appid,
      action = u2iActions.view,
      uid    = "avatar",
      iid    = "creeper",
      t      = DateTime.now,
      latlng = Some((94.3904, -29.4839)),
      v      = None,
      price  = None,
      evalid = Some(1)
    ), U2IAction(
      appid  = appid,
      action = u2iActions.likeDislike,
      uid    = "pub",
      iid    = "sub",
      t      = DateTime.now,
      latlng = None,
      v      = Some(1),
      price  = Some(49.40),
      evalid = Some(100)
    ))
    actions foreach { u2iActions.insert(_) }
    val results = u2iActions.getAll(appid)
    val r1 = results.next
    val r2 = results.next
    val r3 = results.next
    results.hasNext must beFalse and
      (r1 must beEqualTo(actions(0))) and
      (r2 must beEqualTo(actions(1))) and
      (r3 must beEqualTo(actions(2)))
  }
}

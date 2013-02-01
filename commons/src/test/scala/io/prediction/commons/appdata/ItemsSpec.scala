package io.prediction.commons.appdata

import org.specs2._
import org.specs2.specification.Step

import com.mongodb.casbah.Imports._
import org.scala_tools.time.Imports._

class ItemsSpec extends Specification { def is =
  "PredictionIO App Data Items Specification"                                 ^
                                                                              p ^
  "Items can be implemented by:"                                              ^ endp ^
    "1. MongoItems"                                                           ^ mongoItems ^ end

  def mongoItems =                                                            p ^
    "MongoItems should"                                                       ^
      "behave like any Items implementation"                                  ^ items(newMongoItems) ^
                                                                              Step(MongoConnection()(mongoDbName).dropDatabase())

  def items(items: Items) = {                                                 t ^
    "inserting and getting an item"                                           ! insert(items) ^
    "updating an item"                                                        ! update(items) ^
    "deleting an item"                                                        ! delete(items) ^
                                                                              bt
  }

  val mongoDbName = "predictionio_appdata_mongoitems_test"
  def newMongoItems = new mongodb.MongoItems(MongoConnection()(mongoDbName))

  def insert(items: Items) = {
    val appid = 0
    val id = "insert"
    val item = Item(
      id         = id,
      appid      = appid,
      ct         = DateTime.now,
      itypes     = List("fresh", "meat"),
      startt     = Some(DateTime.now.hour(23).minute(13)),
      endt       = None,
      price      = Some(49.394),
      profit     = None,
      latlng     = Some((47.8948, -29.79783)),
      inactive   = None,
      attributes = Some(Map("foo" -> "bar"))
    )
    items.insert(item)
    items.get(appid, id) must beSome(item)
  }

  def update(items: Items) = {
    val appid = 1
    val id = "update"
    val item = Item(
      id         = id,
      appid      = appid,
      ct         = DateTime.now,
      itypes     = List("slash", "dot"),
      startt     = None,
      endt       = None,
      price      = None,
      profit     = None,
      latlng     = None,
      inactive   = None,
      attributes = Some(Map("foo" -> "baz"))
    )

    val updatedItem = item.copy(
      endt       = Some(DateTime.now.minute(47)),
      price      = Some(99.99),
      latlng     = Some((43, 48.378)),
      attributes = Some(Map("raw" -> "beef"))
    )
    items.insert(item)
    items.update(updatedItem)
    items.get(appid, id) must beSome(updatedItem)
  }

  def delete(items: Items) = {
    val appid = 2
    val id = "delete"
    val item = Item(
      id         = id,
      appid      = appid,
      ct         = DateTime.now,
      itypes     = List("fresh", "meat"),
      startt     = Some(DateTime.now.hour(23).minute(13)),
      endt       = None,
      price      = Some(49.394),
      profit     = None,
      latlng     = Some((47.8948, -29.79783)),
      inactive   = None,
      attributes = Some(Map("foo" -> "bar"))
    )
    items.delete(item)
    items.get(appid, id) must beNone
  }
}

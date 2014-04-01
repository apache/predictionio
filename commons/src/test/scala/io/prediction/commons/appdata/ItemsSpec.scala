package io.prediction.commons.appdata

import org.specs2._
import org.specs2.specification.Step

import com.mongodb.casbah.Imports._
import com.github.nscala_time.time.Imports._

class ItemsSpec extends Specification {
  def is =
    "PredictionIO App Data Items Specification" ^
      p ^
      "Items can be implemented by:" ^ endp ^
      "1. MongoItems" ^ mongoItems ^ end

  def mongoItems = p ^
    "MongoItems should" ^
    "behave like any Items implementation" ^ items(newMongoItems) ^
    Step(MongoConnection()(mongoDbName).dropDatabase())

  def items(items: Items) = {
    t ^
      "inserting and getting an item" ! insert(items) ^
      "getting items by App ID and geo data" ! getByAppidAndLatlng(items) ^
      "getting items by App ID and itypes" ! getByAppidAndItypes(items) ^
      "getting items by IDs" ! getByIds(items) ^
      "getting items by IDs sorted by start time" ! getRecentByIds(items) ^
      "updating an item" ! update(items) ^
      "deleting an item" ! delete(items) ^
      "deleting items by appid" ! deleteByAppid(items) ^
      "count items by appid" ! countByAppid(items) ^
      bt
  }

  val mongoDbName = "predictionio_appdata_mongoitems_test"
  def newMongoItems = new mongodb.MongoItems(MongoConnection()(mongoDbName))

  def insert(items: Items) = {
    val appid = 0
    val id1 = "insert1"
    val item1 = Item(
      id = id1,
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(23).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar"))
    )
    val id2 = "insert2"
    val item2 = Item(
      id = id2,
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(23).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((47.8948, -29.79783)),
      inactive = Some(true),
      attributes = None
    )
    items.insert(item1)
    items.insert(item2)
    (items.get(appid, id1) must beSome(item1)) and
      (items.get(appid, id2) must beSome(item2))
  }

  def getByAppidAndLatlng(items: Items) = {
    val id = "getByAppidAndLatlng"
    val appid = 5
    val dac = Item(
      id = id + "dac",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(14).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.3197611, -122.0466141)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar", "foo2" -> "bar2")))
    val hsh = Item(
      id = id + "hsh",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(23).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.3370801, -122.0493201)),
      inactive = None,
      attributes = None)
    val mvh = Item(
      id = id + "mvh",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(17).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.3154153, -122.0566829)),
      inactive = None,
      attributes = Some(Map("foo3" -> "bar3")))
    val lbh = Item(
      id = id + "lbh",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(3).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.2997029, -122.0034684)),
      inactive = None,
      attributes = Some(Map("foo4" -> "bar4", "foo5" -> "bar5")))
    val allItems = Seq(dac, hsh, lbh, mvh)
    allItems foreach { items.insert(_) }
    (items.getByAppidAndLatlng(appid, (37.336402, -122.040467), None, None).toSeq must beEqualTo(Seq(hsh, dac, mvh, lbh))) and
      (items.getByAppidAndLatlng(appid, (37.3229978, -122.0321823), None, None).toSeq must beEqualTo(Seq(dac, hsh, mvh, lbh))) and
      (items.getByAppidAndLatlng(appid, (37.3229978, -122.0321823), Some(2.2), None).toSeq must beEqualTo(Seq(dac, hsh))) and
      (items.getByAppidAndLatlng(appid, (37.3229978, -122.0321823), Some(2.2), Some("mi")).toSeq must beEqualTo(Seq(dac, hsh, mvh)))
  }

  def getByAppidAndItypes(items: Items) = {
    val id = "getByAppidAndItypes"
    val appid = 56
    val dac = Item(
      id = id + "dac",
      appid = appid,
      ct = DateTime.now,
      itypes = List("type1", "type2"),
      starttime = Some(DateTime.now.hour(14).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.3197611, -122.0466141)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar", "foo2" -> "bar2")))
    val hsh = Item(
      id = id + "hsh",
      appid = appid,
      ct = DateTime.now,
      itypes = List("type1"),
      starttime = Some(DateTime.now.hour(23).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.3370801, -122.0493201)),
      inactive = None,
      attributes = None)
    val mvh = Item(
      id = id + "mvh",
      appid = appid,
      ct = DateTime.now,
      itypes = List("type2", "type3"),
      starttime = Some(DateTime.now.hour(17).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.3154153, -122.0566829)),
      inactive = None,
      attributes = Some(Map("foo3" -> "bar3")))
    val lbh = Item(
      id = id + "lbh",
      appid = appid,
      ct = DateTime.now,
      itypes = List("type4"),
      starttime = Some(DateTime.now.hour(3).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.2997029, -122.0034684)),
      inactive = None,
      attributes = Some(Map("foo4" -> "bar4", "foo5" -> "bar5")))

    val allItems = Seq(dac, hsh, lbh, mvh)
    allItems foreach { items.insert(_) }

    (items.getByAppidAndItypes(appid, Seq("type1", "type2", "type3", "type4"))).toSeq must beEqualTo(Seq(dac, hsh, lbh, mvh)) and
      ((items.getByAppidAndItypes(appid, Seq("type1"))).toSeq must beEqualTo(Seq(dac, hsh))) and
      ((items.getByAppidAndItypes(appid, Seq("type2"))).toSeq must beEqualTo(Seq(dac, mvh))) and
      ((items.getByAppidAndItypes(appid, Seq("type3", "type4"))).toSeq must beEqualTo(Seq(lbh, mvh)))

  }

  def getByIds(items: Items) = {
    val id = "getByIds"
    val appid = 4
    val someItems = List(Item(
      id = id + "foo",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(14).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar", "foo2" -> "bar2"))
    ), Item(
      id = id + "bar",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(23).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = None
    ), Item(
      id = id + "baz",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(17).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo3" -> "bar3"))
    ), Item(
      id = id + "pub",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(3).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo4" -> "bar4", "foo5" -> "bar5"))
    ))
    someItems foreach { items.insert(_) }
    val setOfItems = items.getByIds(appid, List(id + "pub", id + "bar", id + "baz")).toSet
    setOfItems.contains(someItems(1)) and setOfItems.contains(someItems(2)) and setOfItems.contains(someItems(3))
  }

  def getRecentByIds(items: Items) = {
    val id = "getRecentByIds"
    val appid = 3
    val timedItems = List(Item(
      id = id + "foo",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(14).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar"))
    ), Item(
      id = id + "bar",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(23).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar"))
    ), Item(
      id = id + "baz",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(17).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar"))
    ), Item(
      id = id + "pub",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(3).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar"))
    ))
    timedItems foreach { items.insert(_) }
    items.getRecentByIds(appid, List(id + "pub", id + "bar", id + "baz")) must beEqualTo(List(timedItems(1), timedItems(2), timedItems(3)))
  }

  def update(items: Items) = {
    val appid = 1
    val id = "update"
    val item = Item(
      id = id,
      appid = appid,
      ct = DateTime.now,
      itypes = List("slash", "dot"),
      starttime = None,
      endtime = None,
      price = None,
      profit = None,
      latlng = None,
      inactive = None,
      attributes = Some(Map("foo" -> "baz"))
    )

    val updatedItem = item.copy(
      endtime = Some(DateTime.now.minute(47)),
      price = Some(99.99),
      latlng = Some((43, 48.378)),
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
      id = id,
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(23).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar"))
    )
    items.delete(item)
    items.get(appid, id) must beNone
  }

  def deleteByAppid(items: Items) = {
    // insert a few items with appid1 and a few items with appid2.
    // delete all items of appid1.
    // items of appid1 should be deleted and items of appid2 should still exist.
    // delete all items of appid2
    // items of appid2 should be deleted

    val appid1 = 10
    val appid2 = 11

    val ida = "deleteByAppid-ida"
    val idb = "deleteByAppid-idb"
    val idc = "deleteByAppid-idc"

    val item1a = Item(
      id = ida,
      appid = appid1,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(23).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar"))
    )
    val item1b = item1a.copy(
      id = idb,
      price = Some(1.23)
    )
    val item1c = item1a.copy(
      id = idc,
      price = Some(2.45)
    )

    val item2a = item1a.copy(
      appid = appid2
    )
    val item2b = item1b.copy(
      appid = appid2
    )
    val item2c = item1c.copy(
      appid = appid2
    )

    items.insert(item1a)
    items.insert(item1b)
    items.insert(item1c)
    items.insert(item2a)
    items.insert(item2b)
    items.insert(item2c)

    val g1_1a = items.get(appid1, ida)
    val g1_1b = items.get(appid1, idb)
    val g1_1c = items.get(appid1, idc)

    val g1_2a = items.get(appid2, ida)
    val g1_2b = items.get(appid2, idb)
    val g1_2c = items.get(appid2, idc)

    items.deleteByAppid(appid1)

    val g2_1a = items.get(appid1, ida)
    val g2_1b = items.get(appid1, idb)
    val g2_1c = items.get(appid1, idc)

    val g2_2a = items.get(appid2, ida)
    val g2_2b = items.get(appid2, idb)
    val g2_2c = items.get(appid2, idc)

    items.deleteByAppid(appid2)

    val g3_2a = items.get(appid2, ida)
    val g3_2b = items.get(appid2, idb)
    val g3_2c = items.get(appid2, idc)

    (g1_1a, g1_1b, g1_1c) must be_==((Some(item1a), Some(item1b), Some(item1c))) and
      ((g1_2a, g1_2b, g1_2c) must be_==((Some(item2a), Some(item2b), Some(item2c)))) and
      ((g2_1a, g2_1b, g2_1c) must be_==((None, None, None))) and
      ((g2_2a, g2_2b, g2_2c) must be_==((Some(item2a), Some(item2b), Some(item2c)))) and
      ((g3_2a, g3_2b, g3_2c) must be_==((None, None, None)))

  }

  def countByAppid(items: Items) = {
    val appid1 = 20
    val appid2 = 21
    val appid3 = 22

    val ida = "countByAppid-ida"
    val idb = "countByAppid-idb"

    val item1a = Item(
      id = ida,
      appid = appid1,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now.hour(23).minute(13)),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((47.8948, -29.79783)),
      inactive = None,
      attributes = Some(Map("foo" -> "bar"))
    )
    val item1b = item1a.copy(
      id = idb
    )
    val item2a = item1a.copy(
      appid = appid2
    )

    items.insert(item1a)
    items.insert(item1b)
    items.insert(item2a)

    items.countByAppid(appid1) must be_==(2) and
      (items.countByAppid(appid2) must be_==(1)) and
      (items.countByAppid(appid3) must be_==(0))
  }
}

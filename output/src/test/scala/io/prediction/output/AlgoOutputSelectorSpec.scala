package io.prediction.output

import io.prediction.commons.Config
import io.prediction.commons.appdata._
import io.prediction.commons.appdata.mongodb._
import io.prediction.commons.modeldata._
import io.prediction.commons.modeldata.mongodb._
import io.prediction.commons.settings._
import io.prediction.commons.settings.mongodb._

import org.specs2._
import org.specs2.specification.Step

import com.mongodb.casbah.Imports._
import com.github.nscala_time.time.Imports._

class AlgoOutputSelectorSpec extends Specification {
  def is =
    "PredictionIO AlgoOutputSelector Specification" ^
      p ^
      "get itemrec output from a valid engine" ! itemRecOutputSelection(algoOutputSelector) ^
      "get itemrec output from a valid engine with freshness" ! itemRecOutputSelectionWithFreshness(algoOutputSelector) ^
      "get itemrec output with geo from a valid engine" ! itemRecOutputSelectionWithLatlng(algoOutputSelector) ^
      "get itemrec output with time constraints from a valid engine" ! itemRecOutputSelectionWithTime(algoOutputSelector) ^
      "get itemrec output with dedup from a valid engine" ! itemRecOutputSelectionDedupByAttribute(algoOutputSelector) ^
      //"get itemrec output from a valid engine without seen items"               ! itemRecOutputSelectionUnseenOnly(algoOutputSelector) ^
      //"get itemrec output from a valid engine with an unsupported algorithm"    ! itemRecOutputSelectionUnsupportedAlgo(algoOutputSelector) ^
      "get itemrec output from a valid engine with no algorithm" ! itemRecOutputSelectionNoAlgo(algoOutputSelector) ^
      "get itemrec output from an invalid engine" ! itemRecOutputSelectionBadEngine(algoOutputSelector) ^
      "get itemsim output from a valid engine" ! itemSimOutputSelection(algoOutputSelector) ^
      "get itemsim output with geo from a valid engine" ! itemSimOutputSelectionWithLatlng(algoOutputSelector) ^
      "get itemsim output with time constraints from a valid engine" ! itemSimOutputSelectionWithTime(algoOutputSelector) ^
      //"get itemsim output from a valid engine with an unsupported algorithm"    ! itemSimOutputSelectionUnsupportedAlgo(algoOutputSelector) ^
      "get itemsim output from a valid engine with no algorithm" ! itemSimOutputSelectionNoAlgo(algoOutputSelector) ^
      "get itemsim output from an invalid engine" ! itemSimOutputSelectionBadEngine(algoOutputSelector) ^
      Step(mongoDb.dropDatabase()) ^
      end

  val mongoDbName = "predictionio_algooutputselection_test"
  val mongoDb = MongoConnection()(mongoDbName)
  val mongoEngines = new MongoEngines(mongoDb)
  val mongoAlgos = new MongoAlgos(mongoDb)
  val mongoItems = new MongoItems(mongoDb)
  val mongoU2IActions = new MongoU2IActions(mongoDb)
  val mongoItemRecScores = new MongoItemRecScores(new Config, mongoDb)
  val mongoItemSimScores = new MongoItemSimScores(new Config, mongoDb)
  val algoOutputSelector = new AlgoOutputSelector(mongoAlgos)

  val dummyApp = App(
    id = 0,
    userid = 0,
    appkey = "dummy",
    display = "dummy",
    url = None,
    cat = None,
    desc = None,
    timezone = "UTC"
  )

  val items = Seq(Item(
    id = "item_x",
    appid = dummyApp.id,
    ct = DateTime.now,
    itypes = Seq("bar"),
    starttime = Some(DateTime.now.minusHours(2)),
    endtime = None,
    price = None,
    profit = None,
    latlng = None,
    inactive = None,
    //attributes = Some(Map("ca_attr1"->"a", "ca_attr2"->"b"))
    attributes = None
  ), Item(
    id = "item_y",
    appid = dummyApp.id,
    ct = DateTime.now,
    itypes = Seq("bar"),
    starttime = Some(DateTime.now.minusHours(8)),
    endtime = None,
    price = None,
    profit = None,
    latlng = None,
    inactive = None,
    attributes = None), Item(
    id = "item_a",
    appid = dummyApp.id,
    ct = DateTime.now,
    itypes = Seq("bar"),
    starttime = Some(DateTime.now.minusHours(5)),
    endtime = None,
    price = None,
    profit = None,
    latlng = None,
    inactive = None,
    attributes = None), Item(
    id = "item_b",
    appid = dummyApp.id,
    ct = DateTime.now,
    itypes = Seq("bar"),
    starttime = Some(DateTime.now.minusHours(3)),
    endtime = None,
    price = None,
    profit = None,
    latlng = None,
    inactive = None,
    attributes = None), Item(
    id = "item_c",
    appid = dummyApp.id,
    ct = DateTime.now,
    itypes = Seq("bar"),
    starttime = Some(DateTime.now.minusHours(10)),
    endtime = None,
    price = None,
    profit = None,
    latlng = None,
    inactive = None,
    attributes = None), Item(
    id = "item_d",
    appid = dummyApp.id,
    ct = DateTime.now,
    itypes = Seq("bar"),
    starttime = Some(DateTime.now.minusHours(7)),
    endtime = None,
    price = None,
    profit = None,
    latlng = None,
    inactive = None,
    attributes = None), Item(
    id = "item_e",
    appid = dummyApp.id,
    ct = DateTime.now,
    itypes = Seq("bar"),
    starttime = Some(DateTime.now.minusHours(1)),
    endtime = None,
    price = None,
    profit = None,
    latlng = None,
    inactive = None,
    attributes = None), Item(
    id = "item_f",
    appid = dummyApp.id,
    ct = DateTime.now,
    itypes = Seq("bar"),
    starttime = Some(DateTime.now.minusHours(10)),
    endtime = None,
    price = None,
    profit = None,
    latlng = None,
    inactive = None,
    attributes = None), Item(
    id = "item_g",
    appid = dummyApp.id,
    ct = DateTime.now,
    itypes = Seq("bar"),
    starttime = Some(DateTime.now.minusHours(4)),
    endtime = None,
    price = None,
    profit = None,
    latlng = None,
    inactive = None,
    attributes = None), Item(
    id = "item_h",
    appid = dummyApp.id,
    ct = DateTime.now,
    itypes = Seq("bar"),
    starttime = Some(DateTime.now.minusHours(6)),
    endtime = None,
    price = None,
    profit = None,
    latlng = None,
    inactive = None,
    attributes = None))

  items foreach { mongoItems.insert(_) }

  /** ItemRec engine. */
  def itemRecOutputSelection(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = dummyApp.id,
      name = "itemRecOutputSelection",
      infoid = "itemrec",
      itypes = Some(Seq("foo", "bar")),
      params = Map("serendipity" -> 5, "freshness" -> 5,
        "freshnessTimeUnit" -> 3600L))
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id = 0,
      engineid = engineid,
      name = "itemRecOutputSelection",
      infoid = "pdio-knnitembased",
      command = "itemRecOutputSelection",
      params = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    val scores = Seq(ItemRecScore(
      uid = "user1",
      iids = Seq("item_z", "item_h", "item_d", "item_g", "item_e", "item_f", "item_x", "item_y", "item_b", "item_c", "item_a"),
      scores = Seq(11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1),
      itypes = Seq(Seq("unrelated"), Seq("foo"), Seq("foo"), Seq("bar"), Seq("bar"), Seq("foo"), Seq("bar"), Seq("foo"), Seq("foo"), Seq("bar"), Seq("bar")),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true))

    scores foreach { mongoItemRecScores.insert(_) }

    val result = algoOutputSelector.itemRecSelection("user1", 10, Some(Seq("bar", "foo")), None, None, None)(dummyApp, engine.copy(id = engineid))
    val resultBar = algoOutputSelector.itemRecSelection("user1", 10, Some(Seq("bar")), None, None, None)(dummyApp, engine.copy(id = engineid))

    result must contain(
      "item_x",
      "item_y",
      "item_a",
      "item_b",
      "item_c",
      "item_d",
      "item_e",
      "item_f",
      "item_g",
      "item_h") and
      (resultBar must contain(
        "item_a",
        "item_c",
        "item_e",
        "item_x",
        "item_g"))

  }

  def itemRecOutputSelectionWithFreshness(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = dummyApp.id,
      name = "itemRecOutputSelection",
      infoid = "itemrec",
      itypes = Some(Seq("foo", "bar")),
      params = Map("serendipity" -> 0, "freshness" -> 5,
        "freshnessTimeUnit" -> 3600L))
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id = 0,
      engineid = engineid,
      name = "itemRecOutputSelection",
      infoid = "pdio-knnitembased",
      command = "itemRecOutputSelection",
      params = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    val scores = Seq(ItemRecScore(
      uid = "user1",
      iids = Seq("item_z", "item_h", "item_d", "item_g", "item_e", "item_f", "item_x", "item_y", "item_b", "item_c", "item_a"),
      scores = Seq(11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1),
      itypes = Seq(Seq("unrelated"), Seq("foo"), Seq("foo"), Seq("bar"), Seq("bar"), Seq("foo"), Seq("bar"), Seq("foo"), Seq("foo"), Seq("bar"), Seq("bar")),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true))

    scores foreach { mongoItemRecScores.insert(_) }

    val result = algoOutputSelector.itemRecSelection("user1", 10, Some(Seq("bar", "foo")), None, None, None)(dummyApp, engine.copy(id = engineid))
    val resultBar = algoOutputSelector.itemRecSelection("user1", 10, Some(Seq("bar")), None, None, None)(dummyApp, engine.copy(id = engineid))

    result must_== Seq(
      "item_g",
      "item_e",
      "item_x",
      "item_h",
      "item_d",
      "item_b",
      "item_f",
      "item_y",
      "item_a",
      "item_c") and
      (resultBar must_== Seq(
        "item_g",
        "item_e",
        "item_x",
        "item_a",
        "item_c"))
  }

  def itemRecOutputSelectionWithLatlng(algoOutputSelector: AlgoOutputSelector) = {
    val appid = dummyApp.id
    val engine = Engine(
      id = 0,
      appid = appid,
      name = "itemRecOutputSelectionWithLatlng",
      infoid = "itemrec",
      itypes = Some(Seq("foo", "bar")),
      params = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id = 0,
      engineid = engineid,
      name = "itemRecOutputSelectionWithLatlng",
      infoid = "pdio-knnitembased",
      command = "itemRecOutputSelectionWithLatlng",
      params = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    val id = "itemRecOutputSelectionWithLatlng"

    val dac = Item(
      id = id + "dac",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now),
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
      starttime = Some(DateTime.now),
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
      starttime = Some(DateTime.now),
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
      starttime = Some(DateTime.now),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.2997029, -122.0034684)),
      inactive = None,
      attributes = Some(Map("foo4" -> "bar4", "foo5" -> "bar5")))
    val allItems = Seq(dac, hsh, lbh, mvh)
    allItems foreach { mongoItems.insert(_) }

    val scores: Seq[(String, Double, Seq[String])] = Seq(
      // (iid, score, itypes)
      (id + "hsh", 4, Seq("foo")),
      (id + "mvh", 3, Seq("unrelated")),
      (id + "lbh", 2, Seq("unrelated")),
      (id + "dac", 1, Seq("bar")))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iids = scores.map(_._1),
      scores = scores.map(_._2),
      itypes = scores.map(_._3),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    algoOutputSelector.itemRecSelection("user1", 10, None, Some((37.3229978, -122.0321823)), Some(2.2), None)(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq(id + "hsh", id + "dac"))
  }

  def itemRecOutputSelectionWithTime(algoOutputSelector: AlgoOutputSelector) = {
    val appid = dummyApp.id
    val engine = Engine(
      id = 0,
      appid = appid,
      name = "itemRecOutputSelectionWithTime",
      infoid = "itemrec",
      itypes = Some(Seq("foo", "bar")),
      params = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id = 0,
      engineid = engineid,
      name = "itemRecOutputSelectionWithTime",
      infoid = "pdio-knnitembased",
      command = "itemRecOutputSelectionWithTime",
      params = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    val id = "itemRecOutputSelectionWithTime"

    val dac = Item(
      id = id + "dac",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now),
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
      starttime = Some(DateTime.lastHour),
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
      starttime = None,
      endtime = Some(DateTime.nextHour),
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
      starttime = None,
      endtime = Some(DateTime.lastHour),
      price = Some(49.394),
      profit = None,
      latlng = Some((37.2997029, -122.0034684)),
      inactive = None,
      attributes = Some(Map("foo4" -> "bar4", "foo5" -> "bar5")))
    val lbh2 = Item(
      id = id + "lbh2",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = None,
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.2997029, -122.0034684)),
      inactive = None,
      attributes = Some(Map("foo4" -> "bar4", "foo5" -> "bar5")))
    val allItems = Seq(dac, hsh, lbh, lbh2, mvh)
    allItems foreach { mongoItems.insert(_) }

    val scores: Seq[(String, Double, Seq[String])] = Seq(
      // (iid, score, itypes)
      (id + "hsh", 5, Seq("foo")),
      (id + "mvh", 4, Seq("foo")),
      (id + "lbh", 3, Seq("bar")),
      (id + "lbh2", 2, Seq("bar")),
      (id + "dac", 1, Seq("bar")))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iids = scores.map(_._1),
      scores = scores.map(_._2),
      itypes = scores.map(_._3),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    algoOutputSelector.itemRecSelection("user1", 10, None, Some((37.3229978, -122.0321823)), Some(10), None)(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq(id + "hsh", id + "mvh", id + "lbh2", id + "dac"))
  }

  def itemRecOutputSelectionUnseenOnly(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = dummyApp.id,
      name = "itemRecOutputSelection",
      infoid = "itemrec",
      itypes = Some(Seq("foo", "bar")),
      params = Map("unseenonly" -> true)
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id = 0,
      engineid = engineid,
      name = "itemRecOutputSelection",
      infoid = "pdio-knnitembased",
      command = "itemRecOutputSelection",
      params = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    val scores: Seq[(String, Double, Seq[String])] = Seq(
      // (iid, score, itypes)
      ("item_x", 5, Seq("bar")),
      ("item_y", 4, Seq("foo")),
      ("item_z", 3, Seq("bar")),
      ("item_c", 2, Seq("foo", "bar")),
      ("item_b", 1, Seq("foo")),
      ("item_a", 0, Seq("bar")),
      ("item_d", -1, Seq("foo", "bar"))
    )

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iids = scores.map(_._1),
      scores = scores.map(_._2),
      itypes = scores.map(_._3),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoU2IActions.insert(U2IAction(
      appid = dummyApp.id,
      action = mongoU2IActions.view,
      uid = "user1",
      iid = "item_b",
      t = DateTime.now,
      latlng = None,
      v = None,
      price = None
    ))

    algoOutputSelector.itemRecSelection("user1", 5, Some(Seq("bar", "foo")), None, None, None)(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq("item_x", "item_y", "item_z", "item_c", "item_a"))
  }

  def itemRecOutputSelectionDedupByAttribute(algoOutputSelector: AlgoOutputSelector) = {
    val appid = dummyApp.id
    val engine = Engine(
      id = 0,
      appid = appid,
      name = "itemRecOutputSelectionDedupByAttribute",
      infoid = "itemrec",
      itypes = Some(Seq("foo", "bar")),
      params = Map("dedupByAttribute" -> "foo")
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id = 0,
      engineid = engineid,
      name = "itemRecOutputSelectionDedupByAttribute",
      infoid = "pdio-knnitembased",
      command = "itemRecOutputSelectionDedupByAttribute",
      params = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    val id = "itemRecOutputSelectionDedupByAttribute"

    val fooA1 = Item(
      id = id + "fooA1",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = None,
      inactive = None,
      attributes = Some(Map("foo" -> "a", "bar" -> "a")))
    val fooA2 = Item(
      id = id + "fooA2",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = None,
      inactive = None,
      attributes = Some(Map("foo" -> "a", "bar" -> "b")))
    val noFoo1 = Item(
      id = id + "noFoo1",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.3154153, -122.0566829)),
      inactive = None,
      attributes = Some(Map("bar" -> "c")))
    val fooB1 = Item(
      id = id + "fooB1",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.2997029, -122.0034684)),
      inactive = None,
      attributes = Some(Map("foo" -> "b")))
    val allItems = Seq(fooA1, fooA2, noFoo1, fooB1)
    allItems foreach { mongoItems.insert(_) }

    val scores1: Seq[(String, Double, Seq[String])] = Seq(
      // (iid, score, itypes)
      (id + "fooA2", 4, Seq("foo")),
      (id + "noFoo1", 3, Seq("unrelated")),
      (id + "fooA1", 2, Seq("unrelated")),
      (id + "fooB1", 1, Seq("bar")))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iids = scores1.map(_._1),
      scores = scores1.map(_._2),
      itypes = scores1.map(_._3),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    val result1 = algoOutputSelector.itemRecSelection(
      "user1", 10, None, None, None, None)(
        dummyApp, engine.copy(id = engineid))

    result1 must beEqualTo(Seq(id + "fooA2", id + "fooB1"))

    val scores2: Seq[(String, Double, Seq[String])] = Seq(
      // (iid, score, itypes)
      (id + "noFoo1", 3, Seq("unrelated")))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user2",
      iids = scores2.map(_._1),
      scores = scores2.map(_._2),
      itypes = scores2.map(_._3),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    val result2 = algoOutputSelector.itemRecSelection(
      "user2", 10, None, None, None, None)(
        dummyApp, engine.copy(id = engineid))

    result2 must beEqualTo(Seq())
  }

  def itemRecOutputSelectionUnsupportedAlgo(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = dummyApp.id,
      name = "itemRecOutputSelection",
      infoid = "itemrec",
      itypes = Some(Seq("foo", "bar")),
      params = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id = 0,
      engineid = engineid,
      name = "itemRecOutputSelection",
      infoid = "abc4",
      command = "itemRecOutputSelection",
      params = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    algoOutputSelector.itemRecSelection("", 10, None, None, None, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }

  def itemRecOutputSelectionNoAlgo(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = dummyApp.id,
      name = "itemRecOutputSelectionNoAlgo",
      infoid = "itemrec",
      itypes = Some(Seq("foo", "bar")),
      params = Map()
    )
    val engineid = mongoEngines.insert(engine)
    algoOutputSelector.itemRecSelection("", 10, None, None, None, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }

  def itemRecOutputSelectionBadEngine(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = dummyApp.id,
      name = "itemRecOutputSelectionBadEngine",
      infoid = "itemRecOutputSelectionBadEngine",
      itypes = Some(Seq("foo", "bar")),
      params = Map()
    )
    val engineid = mongoEngines.insert(engine)
    algoOutputSelector.itemRecSelection("", 10, None, None, None, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }

  /** ItemSim engine. */
  def itemSimOutputSelection(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = dummyApp.id,
      name = "itemSimOutputSelection",
      infoid = "itemsim",
      itypes = Some(Seq("foo", "bar")),
      params = Map("freshness" -> 4, "serendipity" -> 6))
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id = 0,
      engineid = engineid,
      name = "itemSimOutputSelection",
      infoid = "io.prediction.algorithms.scalding.itemsim.itemsimcf",
      command = "itemSimOutputSelection",
      params = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    val scores = Seq(ItemSimScore(
      iid = "user1",
      simiids = Seq("item_z", "item_h", "item_d", "item_g", "item_e", "item_f", "item_x", "item_y", "item_b", "item_c", "item_a"),
      scores = Seq(11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1),
      itypes = Seq(Seq("unrelated"), Seq("foo"), Seq("foo"), Seq("bar"), Seq("bar"), Seq("foo"), Seq("bar"), Seq("foo"), Seq("foo"), Seq("bar"), Seq("bar")),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true))

    scores foreach { mongoItemSimScores.insert(_) }

    val result = algoOutputSelector.itemSimSelection("user1", 10, Some(Seq("bar", "foo")), None, None, None)(dummyApp, engine.copy(id = engineid))
    val resultBar = algoOutputSelector.itemSimSelection("user1", 10, Some(Seq("bar")), None, None, None)(dummyApp, engine.copy(id = engineid))

    result must contain(
      "item_x",
      "item_y",
      "item_a",
      "item_b",
      "item_c",
      "item_d",
      "item_e",
      "item_f",
      "item_g",
      "item_h") and
      (resultBar must contain(
        "item_a",
        "item_c",
        "item_e",
        "item_x",
        "item_g"))
  }

  def itemSimOutputSelectionWithLatlng(algoOutputSelector: AlgoOutputSelector) = {
    val appid = dummyApp.id
    val engine = Engine(
      id = 0,
      appid = appid,
      name = "itemSimOutputSelectionWithLatlng",
      infoid = "itemsim",
      itypes = Some(Seq("foo", "bar")),
      params = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id = 0,
      engineid = engineid,
      name = "itemRecOutputSelectionWithLatlng",
      infoid = "pdio-knnitembased",
      command = "itemRecOutputSelectionWithLatlng",
      params = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    val id = "itemSimOutputSelectionWithLatlng"

    val dac = Item(
      id = id + "dac",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now),
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
      starttime = Some(DateTime.now),
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
      starttime = Some(DateTime.now),
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
      starttime = Some(DateTime.now),
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.2997029, -122.0034684)),
      inactive = None,
      attributes = Some(Map("foo4" -> "bar4", "foo5" -> "bar5")))
    val allItems = Seq(dac, hsh, lbh, mvh)
    allItems foreach { mongoItems.insert(_) }

    val scores: Seq[(String, Double, Seq[String])] = Seq(
      // (iid, score, itypes)
      (id + "hsh", 4, Seq("foo")),
      (id + "mvh", 3, Seq("unrelated")),
      (id + "lbh", 2, Seq("unrelated")),
      (id + "dac", 1, Seq("bar")))

    mongoItemSimScores.insert(ItemSimScore(
      iid = "user1",
      simiids = scores.map(_._1),
      scores = scores.map(_._2),
      itypes = scores.map(_._3),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    algoOutputSelector.itemSimSelection("user1", 10, None, Some((37.3229978, -122.0321823)), Some(2.2), None)(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq(id + "hsh", id + "dac"))
  }

  def itemSimOutputSelectionWithTime(algoOutputSelector: AlgoOutputSelector) = {
    val appid = dummyApp.id
    val engine = Engine(
      id = 0,
      appid = appid,
      name = "itemSimOutputSelectionWithTime",
      infoid = "itemsim",
      itypes = Some(Seq("foo", "bar")),
      params = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id = 0,
      engineid = engineid,
      name = "itemSimOutputSelectionWithTime",
      infoid = "pdio-knnitembased",
      command = "itemSimOutputSelectionWithTime",
      params = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    val id = "itemSimOutputSelectionWithTime"

    val dac = Item(
      id = id + "dac",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = Some(DateTime.now),
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
      starttime = Some(DateTime.lastHour),
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
      starttime = None,
      endtime = Some(DateTime.nextHour),
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
      starttime = None,
      endtime = Some(DateTime.lastHour),
      price = Some(49.394),
      profit = None,
      latlng = Some((37.2997029, -122.0034684)),
      inactive = None,
      attributes = Some(Map("foo4" -> "bar4", "foo5" -> "bar5")))
    val lbh2 = Item(
      id = id + "lbh2",
      appid = appid,
      ct = DateTime.now,
      itypes = List("fresh", "meat"),
      starttime = None,
      endtime = None,
      price = Some(49.394),
      profit = None,
      latlng = Some((37.2997029, -122.0034684)),
      inactive = None,
      attributes = Some(Map("foo4" -> "bar4", "foo5" -> "bar5")))
    val allItems = Seq(dac, hsh, lbh, lbh2, mvh)
    allItems foreach { mongoItems.insert(_) }

    val scores: Seq[(String, Double, Seq[String])] = Seq(
      // (iid, score, itypes)
      (id + "hsh", 5, Seq("foo")),
      (id + "mvh", 4, Seq("foo")),
      (id + "lbh", 3, Seq("bar")),
      (id + "lbh2", 2, Seq("bar")),
      (id + "dac", 1, Seq("bar")))

    mongoItemSimScores.insert(ItemSimScore(
      iid = "item1",
      simiids = scores.map(_._1),
      scores = scores.map(_._2),
      itypes = scores.map(_._3),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    algoOutputSelector.itemSimSelection("item1", 10, None, Some((37.3229978, -122.0321823)), Some(10), None)(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq(id + "hsh", id + "mvh", id + "lbh2", id + "dac"))
  }

  def itemSimOutputSelectionUnsupportedAlgo(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = dummyApp.id,
      name = "itemSimOutputSelection",
      infoid = "itemsim",
      itypes = Some(Seq("foo", "bar")),
      params = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id = 0,
      engineid = engineid,
      name = "itemSimOutputSelection",
      infoid = "abc",
      command = "itemSimOutputSelection",
      params = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    algoOutputSelector.itemSimSelection("", 10, None, None, None, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }

  def itemSimOutputSelectionNoAlgo(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = dummyApp.id,
      name = "itemSimOutputSelectionNoAlgo",
      infoid = "itemsim",
      itypes = Some(Seq("foo", "bar")),
      params = Map()
    )
    val engineid = mongoEngines.insert(engine)
    algoOutputSelector.itemSimSelection("", 10, None, None, None, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }

  def itemSimOutputSelectionBadEngine(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = dummyApp.id,
      name = "itemSimOutputSelectionBadEngine",
      infoid = "itemSimOutputSelectionBadEngine",
      itypes = Some(Seq("foo", "bar")),
      params = Map()
    )
    val engineid = mongoEngines.insert(engine)
    algoOutputSelector.itemSimSelection("", 10, None, None, None, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }
}

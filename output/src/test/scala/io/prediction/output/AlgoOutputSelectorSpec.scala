package io.prediction.output

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

class AlgoOutputSelectorSpec extends Specification { def is =
  "PredictionIO AlgoOutputSelector Specification"                             ^
                                                                              p ^
    "get itemrec output from a valid engine"                                  ! itemRecOutputSelection(algoOutputSelector) ^
    "get itemrec output with geo from a valid engine"                         ! itemRecOutputSelectionWithLatlng(algoOutputSelector) ^
    //"get itemrec output from a valid engine without seen items"               ! itemRecOutputSelectionUnseenOnly(algoOutputSelector) ^
    //"get itemrec output from a valid engine with an unsupported algorithm"    ! itemRecOutputSelectionUnsupportedAlgo(algoOutputSelector) ^
    "get itemrec output from a valid engine with no algorithm"                ! itemRecOutputSelectionNoAlgo(algoOutputSelector) ^
    "get itemrec output from an invalid engine"                               ! itemRecOutputSelectionBadEngine(algoOutputSelector) ^
    "get itemsim output from a valid engine"                                  ! itemSimOutputSelection(algoOutputSelector) ^
    "get itemsim output with geo from a valid engine"                         ! itemSimOutputSelectionWithLatlng(algoOutputSelector) ^
    //"get itemsim output from a valid engine with an unsupported algorithm"    ! itemSimOutputSelectionUnsupportedAlgo(algoOutputSelector) ^
    "get itemsim output from a valid engine with no algorithm"                ! itemSimOutputSelectionNoAlgo(algoOutputSelector) ^
    "get itemsim output from an invalid engine"                               ! itemSimOutputSelectionBadEngine(algoOutputSelector) ^
                                                                              Step(mongoDb.dropDatabase()) ^
                                                                              end

  val mongoDbName = "predictionio_algooutputselection_test"
  val mongoDb = MongoConnection()(mongoDbName)
  val mongoEngines = new MongoEngines(mongoDb)
  val mongoAlgos = new MongoAlgos(mongoDb)
  val mongoItems = new MongoItems(mongoDb)
  val mongoU2IActions = new MongoU2IActions(mongoDb)
  val mongoItemRecScores = new MongoItemRecScores(mongoDb)
  val mongoItemSimScores = new MongoItemSimScores(mongoDb)
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

  /** ItemRec engine. */
  def itemRecOutputSelection(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id         = 0,
      appid      = 123,
      name       = "itemRecOutputSelection",
      infoid = "itemrec",
      itypes     = Some(Seq("foo", "bar")),
      settings   = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id       = 0,
      engineid = engineid,
      name     = "itemRecOutputSelection",
      infoid   = "pdio-knnitembased",
      command  = "itemRecOutputSelection",
      params   = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = "item_x",
      score = 5,
      itypes = Seq("bar"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = "item_y",
      score = 4,
      itypes = Seq("foo"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = "item_z",
      score = 3,
      itypes = Seq("unrelated"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    algoOutputSelector.itemRecSelection("user1", 10, Some(Seq("bar", "foo")), None, None, None)(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq("item_x", "item_y"))
  }

  def itemRecOutputSelectionWithLatlng(algoOutputSelector: AlgoOutputSelector) = {
    val appid = dummyApp.id
    val engine = Engine(
      id       = 0,
      appid    = appid,
      name     = "itemRecOutputSelectionWithLatlng",
      infoid   = "itemrec",
      itypes   = Some(Seq("foo", "bar")),
      settings = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id       = 0,
      engineid = engineid,
      name     = "itemRecOutputSelectionWithLatlng",
      infoid   = "pdio-knnitembased",
      command  = "itemRecOutputSelectionWithLatlng",
      params   = Map("foo" -> "bar"),
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
      id         = id + "dac",
      appid      = appid,
      ct         = DateTime.now,
      itypes     = List("fresh", "meat"),
      starttime  = Some(DateTime.now.hour(14).minute(13)),
      endtime    = None,
      price      = Some(49.394),
      profit     = None,
      latlng     = Some((37.3197611, -122.0466141)),
      inactive   = None,
      attributes = Some(Map("foo" -> "bar", "foo2" -> "bar2")))
    val hsh = Item(
      id         = id + "hsh",
      appid      = appid,
      ct         = DateTime.now,
      itypes     = List("fresh", "meat"),
      starttime  = Some(DateTime.now.hour(23).minute(13)),
      endtime    = None,
      price      = Some(49.394),
      profit     = None,
      latlng     = Some((37.3370801, -122.0493201)),
      inactive   = None,
      attributes = None)
    val mvh = Item(
      id         = id + "mvh",
      appid      = appid,
      ct         = DateTime.now,
      itypes     = List("fresh", "meat"),
      starttime  = Some(DateTime.now.hour(17).minute(13)),
      endtime    = None,
      price      = Some(49.394),
      profit     = None,
      latlng     = Some((37.3154153, -122.0566829)),
      inactive   = None,
      attributes = Some(Map("foo3" -> "bar3")))
    val lbh = Item(
      id         = id + "lbh",
      appid      = appid,
      ct         = DateTime.now,
      itypes     = List("fresh", "meat"),
      starttime  = Some(DateTime.now.hour(3).minute(13)),
      endtime    = None,
      price      = Some(49.394),
      profit     = None,
      latlng     = Some((37.2997029, -122.0034684)),
      inactive   = None,
      attributes = Some(Map("foo4" -> "bar4", "foo5" -> "bar5")))
    val allItems = Seq(dac, hsh, lbh, mvh)
    allItems foreach { mongoItems.insert(_) }

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = id + "dac",
      score = 1,
      itypes = Seq("bar"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = id + "hsh",
      score = 4,
      itypes = Seq("foo"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = id + "mvh",
      score = 3,
      itypes = Seq("unrelated"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = id + "lbh",
      score = 2,
      itypes = Seq("unrelated"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    algoOutputSelector.itemRecSelection("user1", 10, None, Some((37.3229978, -122.0321823)), Some(2.2), None)(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq(id + "hsh", id + "dac"))
  }

  def itemRecOutputSelectionUnseenOnly(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id         = 0,
      appid      = dummyApp.id,
      name       = "itemRecOutputSelection",
      infoid = "itemrec",
      itypes     = Some(Seq("foo", "bar")),
      settings   = Map("unseenonly" -> true)
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id       = 0,
      engineid = engineid,
      name     = "itemRecOutputSelection",
      infoid   = "pdio-knnitembased",
      command  = "itemRecOutputSelection",
      params   = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = "item_x",
      score = 5,
      itypes = Seq("bar"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = "item_y",
      score = 4,
      itypes = Seq("foo"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = "item_z",
      score = 3,
      itypes = Seq("bar"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = "item_a",
      score = 0,
      itypes = Seq("bar"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = "item_b",
      score = 1,
      itypes = Seq("foo"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = "item_c",
      score = 2,
      itypes = Seq("foo", "bar"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = "item_d",
      score = -1,
      itypes = Seq("foo", "bar"),
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

  def itemRecOutputSelectionUnsupportedAlgo(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id         = 0,
      appid      = dummyApp.id,
      name       = "itemRecOutputSelection",
      infoid = "itemrec",
      itypes     = Some(Seq("foo", "bar")),
      settings   = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id       = 0,
      engineid = engineid,
      name     = "itemRecOutputSelection",
      infoid   = "abc4",
      command  = "itemRecOutputSelection",
      params   = Map("foo" -> "bar"),
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
      settings = Map()
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
      settings = Map()
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
      settings = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id       = 0,
      engineid = engineid,
      name     = "itemSimOutputSelection",
      infoid  = "io.prediction.algorithms.scalding.itemsim.itemsimcf",
      command  = "itemSimOutputSelection",
      params   = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    mongoItemSimScores.insert(ItemSimScore(
      iid = "user1",
      simiid = "item_x",
      score = 5,
      itypes = Seq("bar"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemSimScores.insert(ItemSimScore(
      iid = "user1",
      simiid = "item_y",
      score = 4,
      itypes = Seq("foo"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemSimScores.insert(ItemSimScore(
      iid = "user1",
      simiid = "item_z",
      score = 3,
      itypes = Seq("unrelated"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    algoOutputSelector.itemSimSelection("user1", 10, Some(Seq("bar", "foo")), None, None, None)(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq("item_x", "item_y"))
  }

  def itemSimOutputSelectionWithLatlng(algoOutputSelector: AlgoOutputSelector) = {
    val appid = dummyApp.id
    val engine = Engine(
      id       = 0,
      appid    = appid,
      name     = "itemSimOutputSelectionWithLatlng",
      infoid   = "itemsim",
      itypes   = Some(Seq("foo", "bar")),
      settings = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id       = 0,
      engineid = engineid,
      name     = "itemRecOutputSelectionWithLatlng",
      infoid   = "pdio-knnitembased",
      command  = "itemRecOutputSelectionWithLatlng",
      params   = Map("foo" -> "bar"),
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
      id         = id + "dac",
      appid      = appid,
      ct         = DateTime.now,
      itypes     = List("fresh", "meat"),
      starttime  = Some(DateTime.now.hour(14).minute(13)),
      endtime    = None,
      price      = Some(49.394),
      profit     = None,
      latlng     = Some((37.3197611, -122.0466141)),
      inactive   = None,
      attributes = Some(Map("foo" -> "bar", "foo2" -> "bar2")))
    val hsh = Item(
      id         = id + "hsh",
      appid      = appid,
      ct         = DateTime.now,
      itypes     = List("fresh", "meat"),
      starttime  = Some(DateTime.now.hour(23).minute(13)),
      endtime    = None,
      price      = Some(49.394),
      profit     = None,
      latlng     = Some((37.3370801, -122.0493201)),
      inactive   = None,
      attributes = None)
    val mvh = Item(
      id         = id + "mvh",
      appid      = appid,
      ct         = DateTime.now,
      itypes     = List("fresh", "meat"),
      starttime  = Some(DateTime.now.hour(17).minute(13)),
      endtime    = None,
      price      = Some(49.394),
      profit     = None,
      latlng     = Some((37.3154153, -122.0566829)),
      inactive   = None,
      attributes = Some(Map("foo3" -> "bar3")))
    val lbh = Item(
      id         = id + "lbh",
      appid      = appid,
      ct         = DateTime.now,
      itypes     = List("fresh", "meat"),
      starttime  = Some(DateTime.now.hour(3).minute(13)),
      endtime    = None,
      price      = Some(49.394),
      profit     = None,
      latlng     = Some((37.2997029, -122.0034684)),
      inactive   = None,
      attributes = Some(Map("foo4" -> "bar4", "foo5" -> "bar5")))
    val allItems = Seq(dac, hsh, lbh, mvh)
    allItems foreach { mongoItems.insert(_) }

    mongoItemSimScores.insert(ItemSimScore(
      iid = "user1",
      simiid = id + "dac",
      score = 1,
      itypes = Seq("bar"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemSimScores.insert(ItemSimScore(
      iid = "user1",
      simiid = id + "hsh",
      score = 4,
      itypes = Seq("foo"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemSimScores.insert(ItemSimScore(
      iid = "user1",
      simiid = id + "mvh",
      score = 3,
      itypes = Seq("unrelated"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    mongoItemSimScores.insert(ItemSimScore(
      iid = "user1",
      simiid = id + "lbh",
      score = 2,
      itypes = Seq("unrelated"),
      appid = dummyApp.id,
      algoid = algoid,
      modelset = true
    ))

    algoOutputSelector.itemSimSelection("user1", 10, None, Some((37.3229978, -122.0321823)), Some(2.2), None)(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq(id + "hsh", id + "dac"))
  }

  def itemSimOutputSelectionUnsupportedAlgo(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = dummyApp.id,
      name = "itemSimOutputSelection",
      infoid = "itemsim",
      itypes = Some(Seq("foo", "bar")),
      settings = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id       = 0,
      engineid = engineid,
      name     = "itemSimOutputSelection",
      infoid   = "abc",
      command  = "itemSimOutputSelection",
      params   = Map("foo" -> "bar"),
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
      settings = Map()
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
      settings = Map()
    )
    val engineid = mongoEngines.insert(engine)
    algoOutputSelector.itemSimSelection("", 10, None, None, None, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }
}

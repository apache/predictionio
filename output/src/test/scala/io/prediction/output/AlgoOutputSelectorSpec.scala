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
    //"get itemrec output from a valid engine without seen items"               ! itemRecOutputSelectionUnseenOnly(algoOutputSelector) ^
    //"get itemrec output from a valid engine with an unsupported algorithm"    ! itemRecOutputSelectionUnsupportedAlgo(algoOutputSelector) ^
    "get itemrec output from a valid engine with no algorithm"                ! itemRecOutputSelectionNoAlgo(algoOutputSelector) ^
    "get itemrec output from an invalid engine"                               ! itemRecOutputSelectionBadEngine(algoOutputSelector) ^
    "get itemsim output from a valid engine"                                  ! itemSimOutputSelection(algoOutputSelector) ^
    //"get itemsim output from a valid engine with an unsupported algorithm"    ! itemSimOutputSelectionUnsupportedAlgo(algoOutputSelector) ^
    "get itemsim output from a valid engine with no algorithm"                ! itemSimOutputSelectionNoAlgo(algoOutputSelector) ^
    "get itemsim output from an invalid engine"                               ! itemSimOutputSelectionBadEngine(algoOutputSelector) ^
                                                                              Step(mongoDb.dropDatabase()) ^
                                                                              end

  val mongoDbName = "predictionio_algooutputselection_test"
  val mongoDb = MongoConnection()(mongoDbName)
  val mongoEngines = new MongoEngines(mongoDb)
  val mongoAlgos = new MongoAlgos(mongoDb)
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

    algoOutputSelector.itemRecSelection("user1", 10, Some(Seq("bar", "foo")))(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq("item_x", "item_y"))
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

    algoOutputSelector.itemRecSelection("user1", 5, Some(Seq("bar", "foo")))(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq("item_x", "item_y", "item_z", "item_c", "item_a"))
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

    algoOutputSelector.itemRecSelection("", 10, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
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
    algoOutputSelector.itemRecSelection("", 10, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
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
    algoOutputSelector.itemRecSelection("", 10, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
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

    algoOutputSelector.itemSimSelection("user1", 10, Some(Seq("bar", "foo")))(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq("item_x", "item_y"))
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

    algoOutputSelector.itemSimSelection("", 10, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
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
    algoOutputSelector.itemSimSelection("", 10, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
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
    algoOutputSelector.itemSimSelection("", 10, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }
}

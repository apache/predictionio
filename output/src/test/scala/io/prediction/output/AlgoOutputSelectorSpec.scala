package io.prediction.output

import io.prediction.commons.modeldata._
import io.prediction.commons.modeldata.mongodb._
import io.prediction.commons.settings._
import io.prediction.commons.settings.mongodb._

import org.specs2._
import org.specs2.specification.Step

import com.mongodb.casbah.Imports._
import org.scala_tools.time.Imports._

class AlgoOutputSelectorSpec extends Specification { def is =
  "PredictionIO AlgoOutputSelector Specification"                             ^
                                                                              p ^
    "get itemrec output from a valid engine"                                  ! itemRecOutputSelection(algoOutputSelector) ^
    "get itemrec output from a valid engine with an unsupported algorithm"    ! itemRecOutputSelectionUnsupportedAlgo(algoOutputSelector) ^
    "get itemrec output from a valid engine with no algorithm"                ! itemRecOutputSelectionNoAlgo(algoOutputSelector) ^
    "get itemrec output from an invalid engine"                               ! itemRecOutputSelectionBadEngine(algoOutputSelector) ^
    "get itemsim output from a valid engine"                                  ! itemSimOutputSelection(algoOutputSelector) ^
    "get itemsim output from a valid engine with an unsupported algorithm"    ! itemSimOutputSelectionUnsupportedAlgo(algoOutputSelector) ^
    "get itemsim output from a valid engine with no algorithm"                ! itemSimOutputSelectionNoAlgo(algoOutputSelector) ^
    "get itemsim output from an invalid engine"                               ! itemSimOutputSelectionBadEngine(algoOutputSelector) ^
                                                                              Step(mongoDb.dropDatabase()) ^
                                                                              end

  val mongoDbName = "predictionio_algooutputselection_test"
  val mongoDb = MongoConnection()(mongoDbName)
  val mongoEngines = new MongoEngines(mongoDb)
  val mongoAlgos = new MongoAlgos(mongoDb)
  val mongoItemRecScores = new MongoItemRecScores(mongoDb)
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
      enginetype = "itemrec",
      itypes     = Some(List("foo", "bar")),
      settings   = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id       = 0,
      engineid = engineid,
      name     = "itemRecOutputSelection",
      pkgname  = "io.prediction.algorithms.scalding.itemrec.knnitembased",
      deployed = true,
      command  = "itemRecOutputSelection",
      params   = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = "item_x",
      score = 5,
      itypes = List("bar"),
      appid = 0,
      algoid = algoid,
      modelset = true
    ))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = "item_y",
      score = 4,
      itypes = List("foo"),
      appid = 0,
      algoid = algoid,
      modelset = true
    ))

    mongoItemRecScores.insert(ItemRecScore(
      uid = "user1",
      iid = "item_z",
      score = 3,
      itypes = List("unrelated"),
      appid = 0,
      algoid = algoid,
      modelset = true
    ))

    algoOutputSelector.itemRecSelection("user1", 10, Some(List("bar", "foo")))(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq("item_x", "item_y"))
  }

  def itemRecOutputSelectionUnsupportedAlgo(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id         = 0,
      appid      = 123,
      name       = "itemRecOutputSelection",
      enginetype = "itemrec",
      itypes     = Some(List("foo", "bar")),
      settings   = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id       = 0,
      engineid = engineid,
      name     = "itemRecOutputSelection",
      pkgname  = "dummy",
      deployed = true,
      command  = "itemRecOutputSelection",
      params   = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    algoOutputSelector.itemRecSelection("", 10, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }

  def itemRecOutputSelectionNoAlgo(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = 234,
      name = "itemRecOutputSelectionNoAlgo",
      enginetype = "itemrec",
      itypes = Some(List("foo", "bar")),
      settings = Map()
    )
    val engineid = mongoEngines.insert(engine)
    algoOutputSelector.itemRecSelection("", 10, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }

  def itemRecOutputSelectionBadEngine(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = 345,
      name = "itemRecOutputSelectionBadEngine",
      enginetype = "itemRecOutputSelectionBadEngine",
      itypes = Some(List("foo", "bar")),
      settings = Map()
    )
    val engineid = mongoEngines.insert(engine)
    algoOutputSelector.itemRecSelection("", 10, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }

  /** ItemSim engine. */
  def itemSimOutputSelection(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = 123,
      name = "itemSimOutputSelection",
      enginetype = "itemsim",
      itypes = Some(List("foo", "bar")),
      settings = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id       = 0,
      engineid = engineid,
      name     = "itemSimOutputSelection",
      pkgname  = "io.prediction.algorithms.scalding.itemsim.itemsimcf",
      deployed = true,
      command  = "itemSimOutputSelection",
      params   = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    algoOutputSelector.itemSimSelection("dummy", 10, None)(dummyApp, engine.copy(id = engineid)) must beEqualTo(Seq("itemsim", "dummy", algoid.toString, "foo", "bar"))
  }

  def itemSimOutputSelectionUnsupportedAlgo(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = 123,
      name = "itemSimOutputSelection",
      enginetype = "itemsim",
      itypes = Some(List("foo", "bar")),
      settings = Map()
    )
    val engineid = mongoEngines.insert(engine)

    val algo = Algo(
      id       = 0,
      engineid = engineid,
      name     = "itemSimOutputSelection",
      pkgname  = "dummy",
      deployed = true,
      command  = "itemSimOutputSelection",
      params   = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      offlineevalid = None
    )
    val algoid = mongoAlgos.insert(algo)

    algoOutputSelector.itemSimSelection("", 10, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }

  def itemSimOutputSelectionNoAlgo(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = 234,
      name = "itemSimOutputSelectionNoAlgo",
      enginetype = "itemsim",
      itypes = Some(List("foo", "bar")),
      settings = Map()
    )
    val engineid = mongoEngines.insert(engine)
    algoOutputSelector.itemSimSelection("", 10, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }

  def itemSimOutputSelectionBadEngine(algoOutputSelector: AlgoOutputSelector) = {
    val engine = Engine(
      id = 0,
      appid = 345,
      name = "itemSimOutputSelectionBadEngine",
      enginetype = "itemSimOutputSelectionBadEngine",
      itypes = Some(List("foo", "bar")),
      settings = Map()
    )
    val engineid = mongoEngines.insert(engine)
    algoOutputSelector.itemSimSelection("", 10, None)(dummyApp, engine.copy(id = engineid)) must throwA[RuntimeException]
  }
}

package io.prediction.algorithms.graphchi.itemsim

import io.prediction.commons.Config
import io.prediction.commons.settings.{ App, Algo }
import io.prediction.commons.modeldata.{ ItemSimScore }

import org.specs2.mutable._
import com.github.nscala_time.time.Imports._
import scala.io.Source
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

import com.mongodb.casbah.Imports._

class GraphChiItemSimModelConstructorSpec extends Specification {

  // note: should match the db name defined in the application.conf
  val mongoDbName = "predictionio_modeldata_graphchi_dataprep_test"
  def cleanUp() = {
    // remove the test database
    MongoConnection()(mongoDbName).dropDatabase()
  }

  val commonConfig = new Config
  val modeldataItemSimScores = commonConfig.getModeldataItemSimScores

  def argMapToArray(args: Map[String, Any]): Array[String] = {
    args.toArray.flatMap {
      case (k, v) =>
        Array(s"--${k}", v.toString)
    }
  }

  def writeToFile(lines: List[String], filePath: String) = {
    val writer = new BufferedWriter(new FileWriter(new File(filePath)))
    lines.foreach { line =>
      writer.write(s"${line}\n")
    }
    writer.close()
  }

  "GraphChiItemSimModelConstructor" should {
    val inputDir = "/tmp/pio_test/"

    val inputDirFile = new File(inputDir)
    inputDirFile.mkdirs()

    val itemsIndex = List(
      "1\ti0\tt1,t2",
      "2\ti1\tt1",
      "3\ti2\tt2,t3",
      "4\ti3\tt3"
    )

    val validItemIndex = List(
      "1",
      "2",
      "3",
      "4"
    )

    val scoresTopK = List(
      "1 2 12.6",
      "1 3 1.5",
      "2 4 20.4",
      "2 3 5.6",
      "3 4 2.3",
      "4 1 15.4"
    )

    writeToFile(itemsIndex, s"${inputDir}itemsIndex.tsv")
    writeToFile(validItemIndex, s"${inputDir}validItemsIndex.tsv")
    writeToFile(scoresTopK, s"${inputDir}ratings.mm-topk")

    val appid = 12

    implicit val app = App(
      id = appid,
      userid = 0,
      appkey = "1234",
      display = "12345",
      url = None,
      cat = None,
      desc = None,
      timezone = "UTC"
    )

    "correctly writes ItemSimScores with larger numSimilarItems" in {

      val algoid = 45
      val modelSet = false

      implicit val algo = Algo(
        id = algoid,
        engineid = 1234,
        name = "",
        infoid = "abc",
        command = "",
        params = Map(),
        settings = Map(),
        modelset = modelSet,
        createtime = DateTime.now,
        updatetime = DateTime.now,
        status = "deployed",
        offlineevalid = None,
        offlinetuneid = None,
        loop = None,
        paramset = None
      )

      val args = Map(
        "inputDir" -> inputDir,
        "appid" -> appid,
        "algoid" -> algoid,
        "modelSet" -> modelSet,
        "numSimilarItems" -> 10
      )

      val i0Expected = ItemSimScore(
        iid = "i0",
        simiids = Seq("i3", "i1", "i2"),
        scores = Seq(15.4, 12.6, 1.5),
        itypes = Seq(Seq("t3"), Seq("t1"), Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i1Expected = ItemSimScore(
        iid = "i1",
        simiids = Seq("i3", "i0", "i2"),
        scores = Seq(20.4, 12.6, 5.6),
        itypes = Seq(Seq("t3"), Seq("t1", "t2"), Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i2Expected = ItemSimScore(
        iid = "i2",
        simiids = Seq("i1", "i3", "i0"),
        scores = Seq(5.6, 2.3, 1.5),
        itypes = Seq(Seq("t1"), Seq("t3"), Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i3Expected = ItemSimScore(
        iid = "i3",
        simiids = Seq("i1", "i0", "i2"),
        scores = Seq(20.4, 15.4, 2.3),
        itypes = Seq(Seq("t1"), Seq("t1", "t2"), Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      GraphChiModelConstructor.main(argMapToArray(args))

      val i0ItemSim = modeldataItemSimScores.getByIid("i0")
      val i1ItemSim = modeldataItemSimScores.getByIid("i1")
      val i2ItemSim = modeldataItemSimScores.getByIid("i2")
      val i3ItemSim = modeldataItemSimScores.getByIid("i3")

      // don't check id
      i0ItemSim.map(_.copy(id = None)) must beSome(i0Expected) and
        (i1ItemSim.map(_.copy(id = None)) must beSome(i1Expected)) and
        (i2ItemSim.map(_.copy(id = None)) must beSome(i2Expected)) and
        (i3ItemSim.map(_.copy(id = None)) must beSome(i3Expected))

    }

    "correctly writes ItemSimScores with smaller numSimilarItems" in {

      val algoid = 45
      val modelSet = true

      implicit val algo = Algo(
        id = algoid,
        engineid = 1234,
        name = "",
        infoid = "abc",
        command = "",
        params = Map(),
        settings = Map(),
        modelset = modelSet,
        createtime = DateTime.now,
        updatetime = DateTime.now,
        status = "deployed",
        offlineevalid = None,
        offlinetuneid = None,
        loop = None,
        paramset = None
      )

      val args = Map(
        "inputDir" -> inputDir,
        "appid" -> appid,
        "algoid" -> algoid,
        "modelSet" -> modelSet,
        "numSimilarItems" -> 1
      )

      val i0Expected = ItemSimScore(
        iid = "i0",
        simiids = Seq("i3"),
        scores = Seq(15.4),
        itypes = Seq(Seq("t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i1Expected = ItemSimScore(
        iid = "i1",
        simiids = Seq("i3"),
        scores = Seq(20.4),
        itypes = Seq(Seq("t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i2Expected = ItemSimScore(
        iid = "i2",
        simiids = Seq("i1"),
        scores = Seq(5.6),
        itypes = Seq(Seq("t1")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i3Expected = ItemSimScore(
        iid = "i3",
        simiids = Seq("i1"),
        scores = Seq(20.4),
        itypes = Seq(Seq("t1")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      GraphChiModelConstructor.main(argMapToArray(args))

      val i0ItemSim = modeldataItemSimScores.getByIid("i0")
      val i1ItemSim = modeldataItemSimScores.getByIid("i1")
      val i2ItemSim = modeldataItemSimScores.getByIid("i2")
      val i3ItemSim = modeldataItemSimScores.getByIid("i3")

      // don't check id
      i0ItemSim.map(_.copy(id = None)) must beSome(i0Expected) and
        (i1ItemSim.map(_.copy(id = None)) must beSome(i1Expected)) and
        (i2ItemSim.map(_.copy(id = None)) must beSome(i2Expected)) and
        (i3ItemSim.map(_.copy(id = None)) must beSome(i3Expected))

    }

    // TODO: subset valid items
    "correctly writes ItemSimScores with subset numSimilarItems" in {

      val algoid = 46
      val modelSet = false

      val inputDir = "/tmp/pio_test/subset/"

      val inputDirFile = new File(inputDir)
      inputDirFile.mkdirs()

      val validItemIndex = List(
        "1",
        "4"
      )

      writeToFile(itemsIndex, s"${inputDir}itemsIndex.tsv")
      writeToFile(validItemIndex, s"${inputDir}validItemsIndex.tsv")
      writeToFile(scoresTopK, s"${inputDir}ratings.mm-topk")

      implicit val algo = Algo(
        id = algoid,
        engineid = 1234,
        name = "",
        infoid = "abc",
        command = "",
        params = Map(),
        settings = Map(),
        modelset = modelSet,
        createtime = DateTime.now,
        updatetime = DateTime.now,
        status = "deployed",
        offlineevalid = None,
        offlinetuneid = None,
        loop = None,
        paramset = None
      )

      val args = Map(
        "inputDir" -> inputDir,
        "appid" -> appid,
        "algoid" -> algoid,
        "modelSet" -> modelSet,
        "numSimilarItems" -> 10
      )

      val i0Expected = ItemSimScore(
        iid = "i0",
        simiids = Seq("i3"),
        scores = Seq(15.4),
        itypes = Seq(Seq("t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i1Expected = ItemSimScore(
        iid = "i1",
        simiids = Seq("i3", "i0"),
        scores = Seq(20.4, 12.6),
        itypes = Seq(Seq("t3"), Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i2Expected = ItemSimScore(
        iid = "i2",
        simiids = Seq("i3", "i0"),
        scores = Seq(2.3, 1.5),
        itypes = Seq(Seq("t3"), Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i3Expected = ItemSimScore(
        iid = "i3",
        simiids = Seq("i0"),
        scores = Seq(15.4),
        itypes = Seq(Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      GraphChiModelConstructor.main(argMapToArray(args))

      val i0ItemSim = modeldataItemSimScores.getByIid("i0")
      val i1ItemSim = modeldataItemSimScores.getByIid("i1")
      val i2ItemSim = modeldataItemSimScores.getByIid("i2")
      val i3ItemSim = modeldataItemSimScores.getByIid("i3")

      // don't check id
      i0ItemSim.map(_.copy(id = None)) must beSome(i0Expected) and
        (i1ItemSim.map(_.copy(id = None)) must beSome(i1Expected)) and
        (i2ItemSim.map(_.copy(id = None)) must beSome(i2Expected)) and
        (i3ItemSim.map(_.copy(id = None)) must beSome(i3Expected))

    }

    // TODO: evalid

  }

  // NOTE: clean up when finish test
  step(cleanUp())
}
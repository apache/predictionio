package io.prediction.algorithms.mahout.itemsim

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

class MahoutItemSimModelConstructorSpec extends Specification {

  // note: should match the db name defined in the application.conf
  val mongoDbName = "predictionio_modeldata_mahout_dataprep_test"
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

  "MahoutItemSimModelConstructor" should {
    val inputDir = "/tmp/pio_test/"

    val inputDirFile = new File(inputDir)
    inputDirFile.mkdirs()

    val itemsIndex = List(
      "1\ti1\tt1,t2",
      "2\ti2\tt1",
      "3\ti3\tt2,t3",
      "4\ti4\tt3"
    )

    val validItemIndex = List(
      "1",
      "2",
      "3",
      "4"
    )

    val similarities = List(
      "1\t[2:3.2,3:12.5,4:20]",
      "2\t[1:3.2,3:9.0]",
      "3\t[1:12.5,2:9.0,4:12.0]",
      "4\t[3:12.0,1:20]"
    )

    writeToFile(itemsIndex, s"${inputDir}itemsIndex.tsv")
    writeToFile(validItemIndex, s"${inputDir}validItemsIndex.tsv")
    writeToFile(similarities, s"${inputDir}similarities.tsv")

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

      val i1Expected = ItemSimScore(
        iid = "i1",
        simiids = Seq("i4", "i3", "i2"),
        scores = Seq(20.0, 12.5, 3.2),
        itypes = Seq(Seq("t3"), Seq("t2", "t3"), Seq("t1")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i2Expected = ItemSimScore(
        iid = "i2",
        simiids = Seq("i3", "i1"),
        scores = Seq(9.0, 3.2),
        itypes = Seq(Seq("t2", "t3"), Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i3Expected = ItemSimScore(
        iid = "i3",
        simiids = Seq("i1", "i4", "i2"),
        scores = Seq(12.5, 12.0, 9.0),
        itypes = Seq(Seq("t1", "t2"), Seq("t3"), Seq("t1")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i4Expected = ItemSimScore(
        iid = "i4",
        simiids = Seq("i1", "i3"),
        scores = Seq(20.0, 12.0),
        itypes = Seq(Seq("t1", "t2"), Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      MahoutModelConstructor.main(argMapToArray(args))

      val i1ItemSim = modeldataItemSimScores.getByIid("i1")
      val i2ItemSim = modeldataItemSimScores.getByIid("i2")
      val i3ItemSim = modeldataItemSimScores.getByIid("i3")
      val i4ItemSim = modeldataItemSimScores.getByIid("i4")

      // don't check id
      i1ItemSim.map(_.copy(id = None)) must beSome(i1Expected) and
        (i2ItemSim.map(_.copy(id = None)) must beSome(i2Expected)) and
        (i3ItemSim.map(_.copy(id = None)) must beSome(i3Expected)) and
        (i4ItemSim.map(_.copy(id = None)) must beSome(i4Expected))

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

      val i1Expected = ItemSimScore(
        iid = "i1",
        simiids = Seq("i4"),
        scores = Seq(20.0),
        itypes = Seq(Seq("t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i2Expected = ItemSimScore(
        iid = "i2",
        simiids = Seq("i3"),
        scores = Seq(9.0),
        itypes = Seq(Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i3Expected = ItemSimScore(
        iid = "i3",
        simiids = Seq("i1"),
        scores = Seq(12.5),
        itypes = Seq(Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i4Expected = ItemSimScore(
        iid = "i4",
        simiids = Seq("i1"),
        scores = Seq(20.0),
        itypes = Seq(Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      MahoutModelConstructor.main(argMapToArray(args))

      val i1ItemSim = modeldataItemSimScores.getByIid("i1")
      val i2ItemSim = modeldataItemSimScores.getByIid("i2")
      val i3ItemSim = modeldataItemSimScores.getByIid("i3")
      val i4ItemSim = modeldataItemSimScores.getByIid("i4")

      // don't check id
      i1ItemSim.map(_.copy(id = None)) must beSome(i1Expected) and
        (i2ItemSim.map(_.copy(id = None)) must beSome(i2Expected)) and
        (i3ItemSim.map(_.copy(id = None)) must beSome(i3Expected)) and
        (i4ItemSim.map(_.copy(id = None)) must beSome(i4Expected))

    }
    /* don't test, valid item filtering is not done in mahout itemsim modelcon
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
      writeToFile(similarities, s"${inputDir}similarities.tsv")

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

      val i1Expected = ItemSimScore(
        iid = "i1",
        simiids = Seq("i4"),
        scores = Seq(20.0),
        itypes = Seq(Seq("t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i2Expected = ItemSimScore(
        iid = "i2",
        simiids = Seq("i1"),
        scores = Seq(3.2),
        itypes = Seq(Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i3Expected = ItemSimScore(
        iid = "i3",
        simiids = Seq("i1", "i4"),
        scores = Seq(12.5, 12.0),
        itypes = Seq(Seq("t1", "t2"), Seq("t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val i4Expected = ItemSimScore(
        iid = "i4",
        simiids = Seq("i1"),
        scores = Seq(20.0),
        itypes = Seq(Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      MahoutModelConstructor.main(argMapToArray(args))

      val i1ItemSim = modeldataItemSimScores.getByIid("i1")
      val i2ItemSim = modeldataItemSimScores.getByIid("i2")
      val i3ItemSim = modeldataItemSimScores.getByIid("i3")
      val i4ItemSim = modeldataItemSimScores.getByIid("i4")

      // don't check id
      i1ItemSim.map(_.copy(id = None)) must beSome(i1Expected) and
        (i2ItemSim.map(_.copy(id = None)) must beSome(i2Expected)) and
        (i3ItemSim.map(_.copy(id = None)) must beSome(i3Expected)) and
        (i4ItemSim.map(_.copy(id = None)) must beSome(i4Expected))

    }*/

    // TODO: evalid

  }

  // clean up when finish test
  step(cleanUp())
}
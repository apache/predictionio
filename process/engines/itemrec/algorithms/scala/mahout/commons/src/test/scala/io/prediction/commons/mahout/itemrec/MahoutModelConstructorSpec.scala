package io.prediction.algorithms.mahout.itemrec

import io.prediction.commons.Config
import io.prediction.commons.settings.{ App, Algo }
import io.prediction.commons.modeldata.{ ItemRecScore }

import org.specs2.mutable._
import com.github.nscala_time.time.Imports._
import scala.io.Source
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

import com.mongodb.casbah.Imports._

class MahoutModelConstructorSpec extends Specification {

  // note: should match the db name defined in the application.conf
  val mongoDbName = "predictionio_modeldata_mahout_dataprep_test"
  def cleanUp() = {
    // remove the test database
    MongoConnection()(mongoDbName).dropDatabase()
  }

  val commonConfig = new Config
  val modeldataItemRecScores = commonConfig.getModeldataItemRecScores

  // NOTE: use HALF_UP mode to avoid error caused by rounding when compare data
  // (eg. 3.5 vs 3.499999999999).
  // (eg. 0.6666666666 vs 0.666666667)
  def roundUpScores(irec: ItemRecScore): ItemRecScore = {
    irec.copy(
      scores = irec.scores.map { x =>
        BigDecimal(x).setScale(9, BigDecimal.RoundingMode.HALF_UP).toDouble
      }
    )
  }

  def argMapToArray(args: Map[String, Any]): Array[String] = {
    args.toArray.flatMap {
      case (k, v) =>
        Array(s"--${k}", v.toString)
    }
  }

  "MahoutModelConstructor" should {

    val inputDir = "/tmp/pio_test/"

    val inputDirFile = new File(inputDir)
    inputDirFile.mkdirs()

    val usersIndex = List(
      "1\tu0",
      "2\tu1",
      "3\tu2")

    val itemsIndex = List(
      "1\ti0\tt1,t2",
      "2\ti1\tt1",
      "3\ti2\tt2,t3",
      "4\ti3\tt3",
      "5\ti4\tt2,t3",
      "6\ti5\tt1,t2"
    )

    val ratingsCSV = List(
      "1,1,3",
      "1,2,4",
      "1,3,1",
      "2,1,2",
      "2,2,1",
      "3,2,5",
      "3,4,4"
    )

    val predicted = List(
      "1\t[4:0.6123,5:31.432,6:11.3,1:2.3]",
      "2\t[3:1.2,4:11.4,5:3.0,6:2.55]",
      "3\t[1:4.5,3:22.5,2:3.3,5:2.2]")

    def writeToFile(lines: List[String], filePath: String) = {
      val writer = new BufferedWriter(new FileWriter(new File(filePath)))
      lines.foreach { line =>
        writer.write(s"${line}\n")
      }
      writer.close()
    }

    writeToFile(usersIndex, s"${inputDir}usersIndex.tsv")
    writeToFile(itemsIndex, s"${inputDir}itemsIndex.tsv")
    writeToFile(ratingsCSV, s"${inputDir}ratings.csv")
    writeToFile(predicted, s"${inputDir}predicted.tsv")

    val appid = 24

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

    "correctly writes ItemRecScores with larger numRecommendations" in {

      val algoid = 25
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
        "unseenOnly" -> false,
        "numRecommendations" -> 5,
        "booleanData" -> false,
        "implicitFeedback" -> false
      )

      val u0Expected = ItemRecScore(
        uid = "u0",
        iids = Seq("i4", "i5", "i0", "i3"),
        scores = Seq(31.432, 11.3, 2.3, 0.6123),
        itypes = Seq(Seq("t2", "t3"), Seq("t1", "t2"), Seq("t1", "t2"), Seq("t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u1Expected = ItemRecScore(
        uid = "u1",
        iids = Seq("i3", "i4", "i5", "i2"),
        scores = Seq(11.4, 3.0, 2.55, 1.2),
        itypes = Seq(Seq("t3"), Seq("t2", "t3"), Seq("t1", "t2"), Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u2Expected = ItemRecScore(
        uid = "u2",
        iids = Seq("i2", "i0", "i1", "i4"),
        scores = Seq(22.5, 4.5, 3.3, 2.2),
        itypes = Seq(Seq("t2", "t3"), Seq("t1", "t2"), Seq("t1"), Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      MahoutModelConstructor.main(argMapToArray(args))

      val u0ItemRec = modeldataItemRecScores.getByUid("u0")
      val u1ItemRec = modeldataItemRecScores.getByUid("u1")
      val u2ItemRec = modeldataItemRecScores.getByUid("u2")

      // don't check id
      u0ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u0Expected)) and
        (u1ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u1Expected))) and
        (u2ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u2Expected)))

    }

    "correctly writes ItemRecScores with smaller numRecommendations" in {

      val algoid = 26
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
        "unseenOnly" -> false,
        "numRecommendations" -> 2,
        "booleanData" -> false,
        "implicitFeedback" -> false
      )

      val u0Expected = ItemRecScore(
        uid = "u0",
        iids = Seq("i4", "i5"),
        scores = Seq(31.432, 11.3),
        itypes = Seq(Seq("t2", "t3"), Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u1Expected = ItemRecScore(
        uid = "u1",
        iids = Seq("i3", "i4"),
        scores = Seq(11.4, 3.0),
        itypes = Seq(Seq("t3"), Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u2Expected = ItemRecScore(
        uid = "u2",
        iids = Seq("i2", "i0"),
        scores = Seq(22.5, 4.5),
        itypes = Seq(Seq("t2", "t3"), Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      MahoutModelConstructor.main(argMapToArray(args))

      val u0ItemRec = modeldataItemRecScores.getByUid("u0")
      val u1ItemRec = modeldataItemRecScores.getByUid("u1")
      val u2ItemRec = modeldataItemRecScores.getByUid("u2")

      // don't check id
      u0ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u0Expected)) and
        (u1ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u1Expected))) and
        (u2ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u2Expected)))

    }

    "correctly writes ItemRecScores with subset itemsIndex.tsv" in {

      val inputDir = "/tmp/pio_test/subset/"

      val inputDirFile = new File(inputDir)
      inputDirFile.mkdirs()

      val itemsIndex = List(
        "1\ti0\tt1,t2",
        "3\ti2\tt2,t3",
        "4\ti3\tt3"
      )

      writeToFile(usersIndex, s"${inputDir}usersIndex.tsv")
      writeToFile(itemsIndex, s"${inputDir}itemsIndex.tsv")
      writeToFile(ratingsCSV, s"${inputDir}ratings.csv")
      writeToFile(predicted, s"${inputDir}predicted.tsv")

      val algoid = 27
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
        "unseenOnly" -> false,
        "numRecommendations" -> 5,
        "booleanData" -> false,
        "implicitFeedback" -> false
      )

      val u0Expected = ItemRecScore(
        uid = "u0",
        iids = Seq("i0", "i3"),
        scores = Seq(2.3, 0.6123),
        itypes = Seq(Seq("t1", "t2"), Seq("t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u1Expected = ItemRecScore(
        uid = "u1",
        iids = Seq("i3", "i2"),
        scores = Seq(11.4, 1.2),
        itypes = Seq(Seq("t3"), Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u2Expected = ItemRecScore(
        uid = "u2",
        iids = Seq("i2", "i0"),
        scores = Seq(22.5, 4.5),
        itypes = Seq(Seq("t2", "t3"), Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      MahoutModelConstructor.main(argMapToArray(args))

      val u0ItemRec = modeldataItemRecScores.getByUid("u0")
      val u1ItemRec = modeldataItemRecScores.getByUid("u1")
      val u2ItemRec = modeldataItemRecScores.getByUid("u2")

      // don't check id
      u0ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u0Expected)) and
        (u1ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u1Expected))) and
        (u2ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u2Expected)))

    }

    "correctly writes ItemRecScores with unseenOnly=true" in {

      val algoid = 28
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
        "unseenOnly" -> true,
        "numRecommendations" -> 4,
        "booleanData" -> false,
        "implicitFeedback" -> false
      )

      val u0Expected = ItemRecScore(
        uid = "u0",
        iids = Seq("i4", "i5", "i3"),
        scores = Seq(31.432, 11.3, 0.6123),
        itypes = Seq(Seq("t2", "t3"), Seq("t1", "t2"), Seq("t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u1Expected = ItemRecScore(
        uid = "u1",
        iids = Seq("i3", "i4", "i5", "i2"),
        scores = Seq(11.4, 3.0, 2.55, 1.2),
        itypes = Seq(Seq("t3"), Seq("t2", "t3"), Seq("t1", "t2"), Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u2Expected = ItemRecScore(
        uid = "u2",
        iids = Seq("i2", "i0", "i4"),
        scores = Seq(22.5, 4.5, 2.2),
        itypes = Seq(Seq("t2", "t3"), Seq("t1", "t2"), Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      MahoutModelConstructor.main(argMapToArray(args))

      val u0ItemRec = modeldataItemRecScores.getByUid("u0")
      val u1ItemRec = modeldataItemRecScores.getByUid("u1")
      val u2ItemRec = modeldataItemRecScores.getByUid("u2")

      // don't check id
      u0ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u0Expected)) and
        (u1ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u1Expected))) and
        (u2ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u2Expected)))

    }

  }

  // TODO: test evalid != None

  // clean up when finish test
  step(cleanUp())
}

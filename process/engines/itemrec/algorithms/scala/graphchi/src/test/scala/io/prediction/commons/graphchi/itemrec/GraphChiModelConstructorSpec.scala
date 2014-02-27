package io.prediction.commons.graphchi.itemrec

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

class GraphChiModelConstructorSpec extends Specification {

  // note: should match the db name defined in the application.conf
  val mongoDbName = "predictionio_modeldata_graphchi_dataprep_test"
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

  "GraphChiModelConstructor" should {

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
      "4\ti3\tt3"
    )

    val ratingsMM = List(
      "%%MatrixMarket matrix coordinate real general",
      "3 4 9",
      "1 1 3",
      "1 2 4",
      "1 3 1",
      "2 1 2",
      "2 2 1",
      "2 4 3",
      "3 2 5",
      "3 3 1",
      "3 4 4"
    )

    /*
     * 1.2 1.1
     * 4.3 2.3
     * 2.4 1.9
     */
    val ratingsUMM = List(
      "%%MatrixMarket matrix array real general",
      "%This file contains ALS output matrix U. In each row D factors of a single user node.",
      "3 2",
      "1.2",
      "4.3",
      "2.4",
      "1.1",
      "2.3",
      "1.9"
    )

    /*
     * 2.3 2.6
     * 5.1 0.3
     * 3.1 1.9
     * 1.2 4.0
     */
    val ratingsVMM = List(
      "%%MatrixMarket matrix array real general",
      "%This file contains ALS output matrix V. In each row D factors of a single item node.",
      "4 2",
      "2.3",
      "5.1",
      "3.1",
      "1.2",
      "2.6",
      "0.3",
      "1.9",
      "4.0"
    )

    /* UV:
     * 5.62 6.45 5.81 5.84
     * 15.87 22.62 17.7 14.36
     * 10.46 12.81 11.05 10.48
     */

    def writeToFile(lines: List[String], filePath: String) = {
      val writer = new BufferedWriter(new FileWriter(new File(filePath)))
      lines.foreach { line =>
        writer.write(s"${line}\n")
      }
      writer.close()
    }

    writeToFile(usersIndex, s"${inputDir}usersIndex.tsv")
    writeToFile(itemsIndex, s"${inputDir}itemsIndex.tsv")
    writeToFile(ratingsMM, s"${inputDir}ratings.mm")
    writeToFile(ratingsUMM, s"${inputDir}ratings.mm_U.mm")
    writeToFile(ratingsVMM, s"${inputDir}ratings.mm_V.mm")

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
        "numRecommendations" -> 5
      )

      val u0Expected = ItemRecScore(
        uid = "u0",
        iids = Seq("i1", "i3", "i2", "i0"),
        scores = Seq(6.45, 5.84, 5.81, 5.62),
        itypes = Seq(Seq("t1"), Seq("t3"), Seq("t2", "t3"), Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u1Expected = ItemRecScore(
        uid = "u1",
        iids = Seq("i1", "i2", "i0", "i3"),
        scores = Seq(22.62, 17.7, 15.87, 14.36),
        itypes = Seq(Seq("t1"), Seq("t2", "t3"), Seq("t1", "t2"), Seq("t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u2Expected = ItemRecScore(
        uid = "u2",
        iids = Seq("i1", "i2", "i3", "i0"),
        scores = Seq(12.81, 11.05, 10.48, 10.46),
        itypes = Seq(Seq("t1"), Seq("t2", "t3"), Seq("t3"), Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      GraphChiModelConstructor.main(argMapToArray(args))

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
        "numRecommendations" -> 2
      )

      val u0Expected = ItemRecScore(
        uid = "u0",
        iids = Seq("i1", "i3"),
        scores = Seq(6.45, 5.84),
        itypes = Seq(Seq("t1"), Seq("t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u1Expected = ItemRecScore(
        uid = "u1",
        iids = Seq("i1", "i2"),
        scores = Seq(22.62, 17.7),
        itypes = Seq(Seq("t1"), Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u2Expected = ItemRecScore(
        uid = "u2",
        iids = Seq("i1", "i2"),
        scores = Seq(12.81, 11.05),
        itypes = Seq(Seq("t1"), Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      GraphChiModelConstructor.main(argMapToArray(args))

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
      writeToFile(ratingsMM, s"${inputDir}ratings.mm")
      writeToFile(ratingsUMM, s"${inputDir}ratings.mm_U.mm")
      writeToFile(ratingsVMM, s"${inputDir}ratings.mm_V.mm")

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
        "numRecommendations" -> 5
      )

      val u0Expected = ItemRecScore(
        uid = "u0",
        iids = Seq("i3", "i2", "i0"),
        scores = Seq(5.84, 5.81, 5.62),
        itypes = Seq(Seq("t3"), Seq("t2", "t3"), Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u1Expected = ItemRecScore(
        uid = "u1",
        iids = Seq("i2", "i0", "i3"),
        scores = Seq(17.7, 15.87, 14.36),
        itypes = Seq(Seq("t2", "t3"), Seq("t1", "t2"), Seq("t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u2Expected = ItemRecScore(
        uid = "u2",
        iids = Seq("i2", "i3", "i0"),
        scores = Seq(11.05, 10.48, 10.46),
        itypes = Seq(Seq("t2", "t3"), Seq("t3"), Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      GraphChiModelConstructor.main(argMapToArray(args))

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
        "numRecommendations" -> 4
      )

      val u0Expected = ItemRecScore(
        uid = "u0",
        iids = Seq("i3"),
        scores = Seq(5.84),
        itypes = Seq(Seq("t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u1Expected = ItemRecScore(
        uid = "u1",
        iids = Seq("i2"),
        scores = Seq(17.7),
        itypes = Seq(Seq("t2", "t3")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      val u2Expected = ItemRecScore(
        uid = "u2",
        iids = Seq("i0"),
        scores = Seq(10.46),
        itypes = Seq(Seq("t1", "t2")),
        appid = appid,
        algoid = algoid,
        modelset = modelSet)

      GraphChiModelConstructor.main(argMapToArray(args))

      val u0ItemRec = modeldataItemRecScores.getByUid("u0")
      val u1ItemRec = modeldataItemRecScores.getByUid("u1")
      val u2ItemRec = modeldataItemRecScores.getByUid("u2")

      // don't check id
      u0ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u0Expected)) and
        (u1ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u1Expected))) and
        (u2ItemRec.map(roundUpScores(_).copy(id = None)) must beSome(roundUpScores(u2Expected)))

    }

  }

  // clean up when finish test
  step(cleanUp())
}
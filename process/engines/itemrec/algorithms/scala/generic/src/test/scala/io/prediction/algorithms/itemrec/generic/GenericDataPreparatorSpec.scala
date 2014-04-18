package io.prediction.algorithms.generic.itemrec

import io.prediction.commons.Config
import io.prediction.commons.appdata.{ User, Item, U2IAction }

import org.apache.commons.io.FileUtils
import java.io.File
import org.specs2.mutable._
import com.github.nscala_time.time.Imports._
import scala.io.Source
import com.mongodb.casbah.Imports._

class GenericDataPreparatorSpec extends Specification {

  // note: should match the db name defined in the application.conf
  val parentDir = "/tmp/pio_test/io.prediction.algorithms.generic.itemrec.GenericDataPreparatorSpec"

  val mongoDbName = "predictionio_appdata_generic_dataprep_test"
  def cleanUp() = {
    // remove the test database
    MongoConnection()(mongoDbName).dropDatabase()
    FileUtils.deleteDirectory(new File(parentDir))
  }

  val commonConfig = new Config
  val appdataUsers = commonConfig.getAppdataUsers
  val appdataItems = commonConfig.getAppdataItems
  val appdataU2IActions = commonConfig.getAppdataU2IActions

  val appid = 23
  // insert a few users into db
  val user = User(
    id = "u1",
    appid = appid,
    ct = DateTime.now,
    latlng = None,
    inactive = None,
    attributes = None)

  appdataUsers.insert(user.copy(id = "u1"))
  appdataUsers.insert(user.copy(id = "u2"))
  appdataUsers.insert(user.copy(id = "u3"))

  // insert a few items into db
  val itemStartTime = DateTime.now
  val itemStartTimeMillis = itemStartTime.millis
  val item = Item(
    id = "i1",
    appid = appid,
    ct = DateTime.now,
    itypes = List("t1", "t2"),
    starttime = Some(itemStartTime),
    endtime = None,
    price = None,
    profit = None,
    latlng = None,
    inactive = None,
    attributes = None)

  appdataItems.insert(item.copy(id = "i1", itypes = List("t1", "t2")))
  appdataItems.insert(item.copy(id = "i2", itypes = List("t1")))
  appdataItems.insert(item.copy(id = "i3", itypes = List("t2", "t3")))
  appdataItems.insert(item.copy(id = "i4", itypes = List("t3")))

  // insert a few u2i into db
  val u2i = U2IAction(
    appid = appid,
    action = "rate",
    uid = "u0",
    iid = "i0",
    t = DateTime.now,
    latlng = None,
    v = Some(3),
    price = None)

  // test mixed and conflict actions
  appdataU2IActions.insert(u2i.copy(uid = "u1", iid = "i1", action = "view"))
  appdataU2IActions.insert(u2i.copy(uid = "u1", iid = "i1", action = "rate", v = Some(3)))
  appdataU2IActions.insert(u2i.copy(uid = "u1", iid = "i2", action = "rate", v = Some(4)))
  appdataU2IActions.insert(u2i.copy(uid = "u1", iid = "i3", action = "rate", v = Some(1)))

  appdataU2IActions.insert(u2i.copy(uid = "u2", iid = "i1", action = "view"))
  appdataU2IActions.insert(u2i.copy(uid = "u2", iid = "i1", action = "rate", v = Some(2)))
  appdataU2IActions.insert(u2i.copy(uid = "u2", iid = "i2", action = "view"))
  appdataU2IActions.insert(u2i.copy(uid = "u2", iid = "i2", action = "rate", v = Some(5)))
  appdataU2IActions.insert(u2i.copy(uid = "u2", iid = "i2", action = "rate", v = Some(1)))
  appdataU2IActions.insert(u2i.copy(uid = "u2", iid = "i4", action = "dislike"))
  appdataU2IActions.insert(u2i.copy(uid = "u2", iid = "i4", action = "rate", v = Some(3)))

  appdataU2IActions.insert(u2i.copy(uid = "u3", iid = "i2", action = "like"))
  appdataU2IActions.insert(u2i.copy(uid = "u3", iid = "i2", action = "rate", v = Some(5)))
  appdataU2IActions.insert(u2i.copy(uid = "u3", iid = "i3", action = "view"))
  appdataU2IActions.insert(u2i.copy(uid = "u3", iid = "i3", action = "view"))
  appdataU2IActions.insert(u2i.copy(uid = "u3", iid = "i3", action = "rate", v = Some(1)))
  appdataU2IActions.insert(u2i.copy(uid = "u3", iid = "i4", action = "view"))
  appdataU2IActions.insert(u2i.copy(uid = "u3", iid = "i4", action = "rate", v = Some(4)))

  // unknown user and item actions (not exist in user and items appdata)
  appdataU2IActions.insert(u2i.copy(uid = "u3", iid = "i5", action = "view"))
  appdataU2IActions.insert(u2i.copy(uid = "u3", iid = "i6", action = "rate", v = Some(4)))
  appdataU2IActions.insert(u2i.copy(uid = "u4", iid = "i2", action = "view"))
  appdataU2IActions.insert(u2i.copy(uid = "u4", iid = "i1", action = "rate", v = Some(3)))

  "GenericDataPreparator with matrixMarket = true" should {

    val outputDir = s"${parentDir}/mmtrue"
    val args = Map(
      "outputDir" -> outputDir,
      "appid" -> appid,
      "viewParam" -> 4,
      "likeParam" -> 3,
      "dislikeParam" -> 1,
      "conversionParam" -> 2,
      "conflictParam" -> "latest",
      "matrixMarket" -> true
    )

    val argsArray = args.toArray.flatMap {
      case (k, v) =>
        Array(s"--${k}", v.toString)
    }

    GenericDataPreparator.main(argsArray)

    "correctly generate usersIndex.tsv" in {
      val usersIndex = Source.fromFile(s"${outputDir}usersIndex.tsv")
        .getLines()
        .toList

      val expected = List(
        "1\tu1",
        "2\tu2",
        "3\tu3")

      usersIndex must containTheSameElementsAs(expected)
    }

    "correctly generate itemsIndex.tsv" in {
      val itemsIndex = Source.fromFile(s"${outputDir}itemsIndex.tsv")
        .getLines()
        .toList

      val expected = List(
        s"1\ti1\tt1,t2\t${itemStartTimeMillis}",
        s"2\ti2\tt1\t${itemStartTimeMillis}",
        s"3\ti3\tt2,t3\t${itemStartTimeMillis}",
        s"4\ti4\tt3\t${itemStartTimeMillis}"
      )
      itemsIndex must containTheSameElementsAs(expected)
    }

    "correctly generate ratings.mm" in {
      val ratingsLines = Source.fromFile(s"${outputDir}ratings.mm")
        .getLines().toList

      val headers = ratingsLines.take(2)

      val ratings = ratingsLines.drop(2)

      val expectedHeaders = List(
        "%%MatrixMarket matrix coordinate real general",
        "3 4 9"
      )

      val expected = List(
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
      headers must be_==(expectedHeaders) and
        (ratings must containTheSameElementsAs(expected))
    }

    "don't write seen.csv" in {
      val seenFile = new File(s"${outputDir}seen.csv")
      seenFile.exists() must be_==(false)
    }

  }

  "GenericDataPreparator with matrixMarket = false" should {

    val outputDir = s"${parentDir}/mmfalse"
    val args = Map(
      "outputDir" -> outputDir,
      "appid" -> appid,
      "viewParam" -> 4,
      "likeParam" -> 3,
      "dislikeParam" -> 1,
      "conversionParam" -> 2,
      "conflictParam" -> "latest",
      "matrixMarket" -> false
    )

    val argsArray = args.toArray.flatMap {
      case (k, v) =>
        Array(s"--${k}", v.toString)
    }

    GenericDataPreparator.main(argsArray)

    "correctly generate usersIndex.tsv" in {
      val usersIndex = Source.fromFile(s"${outputDir}usersIndex.tsv")
        .getLines()
        .toList

      val expected = List(
        "1\tu1",
        "2\tu2",
        "3\tu3")

      usersIndex must containTheSameElementsAs(expected)
    }

    "correctly generate itemsIndex.tsv" in {
      val itemsIndex = Source.fromFile(s"${outputDir}itemsIndex.tsv")
        .getLines()
        .toList

      val expected = List(
        s"1\ti1\tt1,t2\t${itemStartTimeMillis}",
        s"2\ti2\tt1\t${itemStartTimeMillis}",
        s"3\ti3\tt2,t3\t${itemStartTimeMillis}",
        s"4\ti4\tt3\t${itemStartTimeMillis}"
      )
      itemsIndex must containTheSameElementsAs(expected)
    }

    "correctly generate ratings.csv" in {
      val ratings = Source.fromFile(s"${outputDir}ratings.csv")
        .getLines().toList

      val expected = List(
        "1,1,3",
        "1,2,4",
        "1,3,1",
        "2,1,2",
        "2,2,1",
        "2,4,3",
        "3,2,5",
        "3,3,1",
        "3,4,4"
      )

      ratings must containTheSameElementsAs(expected)
    }

    "don't write seen.csv" in {
      val seenFile = new File(s"${outputDir}seen.csv")
      seenFile.exists() must be_==(false)
    }
  }

  "GenericDataPreparator with matrixMarket = false and seenAction" should {

    val outputDir = s"${parentDir}/seenActions"
    val args = Map(
      "outputDir" -> outputDir,
      "appid" -> appid,
      "viewParam" -> 4,
      "likeParam" -> 3,
      "dislikeParam" -> 1,
      "conversionParam" -> 2,
      "conflictParam" -> "latest",
      "seenActions" -> Array("view", "like"),
      "matrixMarket" -> false
    )

    val argsArray = args.toArray.flatMap {
      case (k, v) =>
        v match {
          case x: Array[String] => Array(s"--${k}") ++ x
          case _ => Array(s"--${k}", v.toString)
        }

    }

    GenericDataPreparator.main(argsArray)

    "correctly generate usersIndex.tsv" in {
      val usersIndex = Source.fromFile(s"${outputDir}usersIndex.tsv")
        .getLines()
        .toList

      val expected = List(
        "1\tu1",
        "2\tu2",
        "3\tu3")

      usersIndex must containTheSameElementsAs(expected)
    }

    "correctly generate itemsIndex.tsv" in {
      val itemsIndex = Source.fromFile(s"${outputDir}itemsIndex.tsv")
        .getLines()
        .toList

      val expected = List(
        s"1\ti1\tt1,t2\t${itemStartTimeMillis}",
        s"2\ti2\tt1\t${itemStartTimeMillis}",
        s"3\ti3\tt2,t3\t${itemStartTimeMillis}",
        s"4\ti4\tt3\t${itemStartTimeMillis}"
      )
      itemsIndex must containTheSameElementsAs(expected)
    }

    "correctly generate ratings.csv" in {
      val ratings = Source.fromFile(s"${outputDir}ratings.csv")
        .getLines().toList

      val expected = List(
        "1,1,3",
        "1,2,4",
        "1,3,1",
        "2,1,2",
        "2,2,1",
        "2,4,3",
        "3,2,5",
        "3,3,1",
        "3,4,4"
      )

      ratings must containTheSameElementsAs(expected)
    }

    "correctly write seen.csv" in {
      val seen = Source.fromFile(s"${outputDir}seen.csv").getLines().toList
      val expected = List(
        "1,1",
        "2,1",
        "2,2",
        "3,2",
        "3,3",
        "3,4"
      )
      seen must containTheSameElementsAs(expected)
    }
  }

  // TODO: test start and end time

  // TODO: test evalid != None

  // clean up when finish test
  step(cleanUp())
}

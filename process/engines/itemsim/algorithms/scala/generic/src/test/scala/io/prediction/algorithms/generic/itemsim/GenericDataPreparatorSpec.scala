package io.prediction.algorithms.generic.itemsim

import io.prediction.commons.Config
import io.prediction.commons.appdata.{ User, Item, U2IAction }

import org.specs2.mutable._
import com.github.nscala_time.time.Imports._
import scala.io.Source
import com.mongodb.casbah.Imports._

class GenericDataPreparatorSpec extends Specification {

  // note: should match the db name defined in the application.conf
  val mongoDbName = "predictionio_appdata_generic_dataprep_test"
  def cleanUp() = {
    // remove the test database
    MongoConnection()(mongoDbName).dropDatabase()
  }

  val commonConfig = new Config
  val appdataUsers = commonConfig.getAppdataUsers
  val appdataItems = commonConfig.getAppdataItems
  val appdataU2IActions = commonConfig.getAppdataU2IActions

  "GenericDataPreparator with basic rate action app data" should {
    val appid = 23
    // insert a few users into db
    val user = User(
      id = "u0",
      appid = appid,
      ct = DateTime.now,
      latlng = None,
      inactive = None,
      attributes = None)

    appdataUsers.insert(user.copy(id = "u0"))
    appdataUsers.insert(user.copy(id = "u1"))
    appdataUsers.insert(user.copy(id = "u2"))

    // insert a few items into db
    val item = Item(
      id = "i0",
      appid = appid,
      ct = DateTime.now,
      itypes = List("t1", "t2"),
      starttime = None,
      endtime = None,
      price = None,
      profit = None,
      latlng = None,
      inactive = None,
      attributes = None)

    appdataItems.insert(item.copy(id = "i0", itypes = List("t1", "t2")))
    appdataItems.insert(item.copy(id = "i1", itypes = List("t1")))
    appdataItems.insert(item.copy(id = "i2", itypes = List("t2", "t3")))
    appdataItems.insert(item.copy(id = "i3", itypes = List("t3")))

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

    appdataU2IActions.insert(u2i.copy(uid = "u0", iid = "i0", action = "rate", v = Some(3)))
    appdataU2IActions.insert(u2i.copy(uid = "u0", iid = "i1", action = "rate", v = Some(4)))
    appdataU2IActions.insert(u2i.copy(uid = "u0", iid = "i2", action = "rate", v = Some(1)))

    appdataU2IActions.insert(u2i.copy(uid = "u1", iid = "i0", action = "rate", v = Some(2)))
    appdataU2IActions.insert(u2i.copy(uid = "u1", iid = "i1", action = "rate", v = Some(1)))
    appdataU2IActions.insert(u2i.copy(uid = "u1", iid = "i3", action = "rate", v = Some(3)))

    appdataU2IActions.insert(u2i.copy(uid = "u2", iid = "i1", action = "rate", v = Some(5)))
    appdataU2IActions.insert(u2i.copy(uid = "u2", iid = "i2", action = "rate", v = Some(1)))
    appdataU2IActions.insert(u2i.copy(uid = "u2", iid = "i3", action = "rate", v = Some(4)))

    val outputDir = "/tmp/pio_test/"
    val args = Map(
      "outputDir" -> outputDir,
      "appid" -> appid,
      "viewParam" -> 4,
      "likeParam" -> 3,
      "dislikeParam" -> 1,
      "conversionParam" -> 2,
      "conflictParam" -> "highest"
    )

    val argsArray = args.toArray.flatMap {
      case (k, v) =>
        Array(s"--${k}", v.toString)
    }

    GenericDataPreparator.main(argsArray)

    "correctly generate itemsIndex.tsv" in {
      val itemsIndex = Source.fromFile(s"${outputDir}itemsIndex.tsv")
        .getLines()
        .toList

      val expected = List(
        "1\ti0\tt1,t2",
        "2\ti1\tt1",
        "3\ti2\tt2,t3",
        "4\ti3\tt3"
      )
      itemsIndex must containTheSameElementsAs(expected)
    }

    "correctly write validItemsIndex.tsv" in {
      val validItemsIndex = Source.fromFile(s"${outputDir}validItemsIndex.tsv")
        .getLines()
        .toList

      val expected = List(
        "1",
        "2",
        "3",
        "4"
      )
      validItemsIndex must containTheSameElementsAs(expected)
    }

    "correctly generate ratings.mm" in {
      val ratingsLines = Source.fromFile(s"${outputDir}ratings.mm")
        .getLines()

      val headers = ratingsLines.take(2).toList

      val ratings = ratingsLines.toList

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
  }

  // TODO: test csv output

  // TODO: test mixed and conflict actions

  // TODO: test start and end time

  // TODO: test evalid != None

  // clean up when finish test
  step(cleanUp())
}

package io.prediction.evaluations.scalding.commons.u2itrainingtestsplit

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{ Users, Items, U2iActions }
import io.prediction.commons.filepath.U2ITrainingTestSplitFile
import io.prediction.commons.appdata.{ User, Item }

class U2ITrainingTestSplitTimeTest extends Specification with TupleConversions {

  def test(itypes: List[String], trainingPercent: Double, validationPercent: Double, testPercent: Double, timeorder: Boolean,
    appid: Int, evalid: Int,
    items: List[(String, String, String, String, String)],
    users: List[(String, String, String)],
    u2iActions: List[(String, String, String, String, String)],
    selectedItems: List[(String, String, String, String, String)],
    selectedUsers: List[(String, String, String)],
    selectedU2iActions: List[(String, String, String, String, String)]) = {

    val dbType = "file"
    val dbName = "testpath/"
    val dbHost = None
    val dbPort = None

    val training_dbType = "file"
    val training_dbName = "trainingsetpath/"
    val training_dbHost = None
    val training_dbPort = None

    val validation_dbType = "file"
    val validation_dbName = "validationpath/"
    val validation_dbHost = None
    val validation_dbPort = None

    val test_dbType = "file"
    val test_dbName = "testsetpath/"
    val test_dbHost = None
    val test_dbPort = None

    val hdfsRoot = "testroot/"

    val engineid = 4

    val originalCount = selectedU2iActions.size

    val totalPercent = (trainingPercent + validationPercent + testPercent)
    val evalCount: Int = scala.math.floor(totalPercent * originalCount).toInt
    val trainingCount: Int = scala.math.floor((trainingPercent * originalCount)).toInt
    val validationCount: Int = scala.math.floor((validationPercent * originalCount)).toInt
    val testCount: Int = evalCount - trainingCount - validationCount
    /*
    println("originalCount=" + originalCount)
    println("evalCount="+ evalCount )
    println("trainingCount="+ trainingCount)
    println("validationCount="+ validationCount)
    println("testCount="+testCount)
    */

    JobTest("io.prediction.evaluations.scalding.commons.u2itrainingtestsplit.U2ITrainingTestSplitTimePrep")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      .arg("training_dbType", training_dbType)
      .arg("training_dbName", training_dbName)
      .arg("validation_dbType", validation_dbType)
      .arg("validation_dbName", validation_dbName)
      .arg("test_dbType", test_dbType)
      .arg("test_dbName", test_dbName)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("evalid", evalid.toString)
      .arg("trainingPercent", trainingPercent.toString)
      .arg("validationPercent", validationPercent.toString)
      .arg("testPercent", testPercent.toString)
      .arg("timeorder", timeorder.toString)
      .source(Users(appId = appid, dbType = dbType, dbName = dbName, dbHost = dbHost, dbPort = dbPort).getSource, users)
      .source(Items(appId = appid, itypes = Some(itypes), dbType = dbType, dbName = dbName, dbHost = dbHost, dbPort = dbPort).getSource, items)
      .source(U2iActions(appId = appid, dbType = dbType, dbName = dbName, dbHost = dbHost, dbPort = dbPort).getSource, u2iActions)
      .sink[(String, String, String)](Users(appId = evalid, dbType = training_dbType, dbName = training_dbName, dbHost = training_dbHost, dbPort = training_dbPort).getSource) { outputBuffer =>
        "correctly write trainingUsers" in {
          outputBuffer must containTheSameElementsAs(selectedUsers)
        }
      }
      .sink[(String, String, String, String, String)](Items(appId = evalid, itypes = None, dbType = training_dbType, dbName = training_dbName, dbHost = training_dbHost, dbPort = training_dbPort).getSource) { outputBuffer =>
        "correctly write trainingItems" in {
          outputBuffer must containTheSameElementsAs(selectedItems)
        }
      }
      .sink[(String, String, String, String, String)](U2iActions(appId = evalid,
        dbType = "file", dbName = U2ITrainingTestSplitFile(hdfsRoot, appid, engineid, evalid, ""), dbHost = None, dbPort = None).getSource) { outputBuffer =>
        "correctly write u2iActions" in {
          outputBuffer must containTheSameElementsAs(selectedU2iActions)
        }
      }
      .sink[(Int)](Tsv(U2ITrainingTestSplitFile(hdfsRoot, appid, engineid, evalid, "u2iCount.tsv"))) { outputBuffer =>
        "correctly write u2iActions count" in {
          outputBuffer must containTheSameElementsAs(List(originalCount))
        }
      }
      .run
      .finish

    def splitTest() = {

      val results = new scala.collection.mutable.HashMap[String, List[(String, String, String, String, String)]] with scala.collection.mutable.SynchronizedMap[String, List[(String, String, String, String, String)]]

      JobTest("io.prediction.evaluations.scalding.commons.u2itrainingtestsplit.U2ITrainingTestSplitTime")
        .arg("dbType", dbType)
        .arg("dbName", dbName)
        .arg("training_dbType", training_dbType)
        .arg("training_dbName", training_dbName)
        .arg("validation_dbType", validation_dbType)
        .arg("validation_dbName", validation_dbName)
        .arg("test_dbType", test_dbType)
        .arg("test_dbName", test_dbName)
        .arg("hdfsRoot", hdfsRoot)
        .arg("appid", appid.toString)
        .arg("engineid", engineid.toString)
        .arg("evalid", evalid.toString)
        .arg("trainingPercent", trainingPercent.toString)
        .arg("validationPercent", validationPercent.toString)
        .arg("testPercent", testPercent.toString)
        .arg("timeorder", timeorder.toString)
        .arg("totalCount", originalCount.toString)
        .source(U2iActions(appId = evalid,
          dbType = "file", dbName = U2ITrainingTestSplitFile(hdfsRoot, appid, engineid, evalid, ""), dbHost = None, dbPort = None).getSource, selectedU2iActions)
        .sink[(String, String, String, String, String)](U2iActions(appId = evalid,
          dbType = training_dbType, dbName = training_dbName, dbHost = training_dbHost, dbPort = training_dbPort).getSource) { outputBuffer =>
          "generate training set" in {
            val output = outputBuffer.toList
            results += ("training" -> output) // remember the output for later checking purpose

            // note: since the selection is random, can't know the expected selection beforehand.
            // so just check if the original data contain the selected data and the size is correct.
            // Randomness and time order is checked in later stages.
            selectedU2iActions must containAllOf(output) and
              (output.size must be_==(trainingCount))
          }
        }
        .sink[(String, String, String, String, String)](U2iActions(appId = evalid,
          dbType = validation_dbType, dbName = validation_dbName, dbHost = validation_dbHost, dbPort = validation_dbPort).getSource) { outputBuffer =>
          "generate validation set" in {
            val output = outputBuffer.toList
            results += ("validation" -> output)
            selectedU2iActions must containAllOf(output) and
              (output.size must be_==(validationCount))
          }
        }
        .sink[(String, String, String, String, String)](U2iActions(appId = evalid,
          dbType = test_dbType, dbName = test_dbName, dbHost = test_dbHost, dbPort = test_dbPort).getSource) { outputBuffer =>
          "generate test set" in {
            val output = outputBuffer.toList
            results += ("test" -> output)
            selectedU2iActions must containAllOf(output) and
              (output.size must be_==(testCount))
          }
        }
        .run
        .finish

      "all sets are mutually exclusive" in {
        // make sure all 3 sinks are flushed
        while (results.keys.size < 3) Thread.sleep(1000)
        (results("training") must not(containAnyOf(results("validation")))) and
          (results("training") must not(containAnyOf(results("test")))) and
          (results("validation") must not(containAnyOf(results("test"))))
      }

      def getTimeOnly(dataSet: List[(String, String, String, String, String)]): List[Long] = {
        dataSet map { case (action, uid, iid, t, v) => t.toLong }
      }

      if (timeorder) {
        // check time order
        if (validationPercent != 0) {
          "validation set must be newer than training set" in {
            while (results.keys.size < 3) Thread.sleep(1000)
            getTimeOnly(results("validation")).min must be_>=(getTimeOnly(results("training")).max)
          }
          "test set must be newer than validation set" in {
            while (results.keys.size < 3) Thread.sleep(1000)
            getTimeOnly(results("test")).min must be_>=(getTimeOnly(results("validation")).max)
          }
        }

        "test set must be newer than training set" in {
          while (results.keys.size < 3) Thread.sleep(1000)
          getTimeOnly(results("test")).min must be_>=(getTimeOnly(results("training")).max)
        }
      }

      results
    }

    val firstSplit = splitTest()
    val secondSplit = splitTest()

    // simple check for randomness
    if (timeorder) {
      "at least one set of two split is different" in {
        // for timeorder=true case, some sets may still be the same even resplit 2nd time
        // because the original data is small, we select most of them (say > 90%) and
        // split according to time order. The chance of ending up same data in the set is high.
        // so here just do simple check: as long as 1 set is different, consider OK.
        // (it's possible to check all difference if the test input data is large enough and selected percentage is relative small.)
        (firstSplit("training") must not(containTheSameElementsAs(secondSplit("training")))) or
          (firstSplit("validation") must not(containTheSameElementsAs(secondSplit("validation")))) or
          (firstSplit("test") must not(containTheSameElementsAs(secondSplit("test"))))
      }
    } else {
      "all sets of two splits are different" in {
        if (validationPercent == 0) {
          // don't check validation set since it is empty
          (firstSplit("training") must not(containTheSameElementsAs(secondSplit("training")))) and
            (firstSplit("test") must not(containTheSameElementsAs(secondSplit("test"))))
        } else {
          (firstSplit("training") must not(containTheSameElementsAs(secondSplit("training")))) and
            (firstSplit("validation") must not(containTheSameElementsAs(secondSplit("validation")))) and
            (firstSplit("test") must not(containTheSameElementsAs(secondSplit("test"))))
        }
      }
    }

  }

  val appid = 2
  val evalid = 101
  val users = List(
    (appid + "_u0", appid.toString, "123456"),
    (appid + "_u1", appid.toString, "23456"),
    (appid + "_u2", appid.toString, "455677"),
    (appid + "_u3", appid.toString, "876563111"))

  val items = List(
    (appid + "_i0", "t1,t2,t3", appid.toString, "2293300", "1266673"),
    (appid + "_i1", "t2,t3", appid.toString, "14526361", "12345135"),
    (appid + "_i2", "t4", appid.toString, "14526361", "23423424"),
    (appid + "_i3", "t3,t4", appid.toString, "1231415", "378462511"))

  val u2iActions = List(
    ("4", appid + "_u0", appid + "_i1", "1234500", "5"),
    ("3", appid + "_u3", appid + "_i0", "1234505", "1"),
    ("4", appid + "_u1", appid + "_i3", "1234501", "3"),
    ("4", appid + "_u1", appid + "_i2", "1234506", "4"),
    ("2", appid + "_u1", appid + "_i0", "1234507", "5"),
    ("3", appid + "_u2", appid + "_i3", "1234502", "2"),
    ("4", appid + "_u0", appid + "_i2", "1234508", "3"),
    ("4", appid + "_u2", appid + "_i0", "1234509", "1"),
    ("4", appid + "_u0", appid + "_i1", "1234503", "2"),
    ("4", appid + "_u3", appid + "_i3", "1234504", "3"),
    ("4", appid + "_u3", appid + "_i3", "1234503", "3"),
    ("4", appid + "_u2", appid + "_i3", "1234504", "3"),
    ("4", appid + "_u1", appid + "_i3", "1234505", "3"),
    ("4", appid + "_u0", appid + "_i3", "1234509", "3"),
    ("view", appid + "_u0", appid + "_i0", "1234509", "PIO_NONE"), // test missing v field case (non-rate action)
    ("like", appid + "_u1", appid + "_i2", "1234509", "PIO_NONE")) // test missing v field case (non-rate action)

  val selectedUsers = List(
    (evalid + "_u0", evalid.toString, "123456"),
    (evalid + "_u1", evalid.toString, "23456"),
    (evalid + "_u2", evalid.toString, "455677"),
    (evalid + "_u3", evalid.toString, "876563111"))

  val selectedItemsAll = List(
    (evalid + "_i0", "t1,t2,t3", evalid.toString, "2293300", "1266673"),
    (evalid + "_i1", "t2,t3", evalid.toString, "14526361", "12345135"),
    (evalid + "_i2", "t4", evalid.toString, "14526361", "23423424"),
    (evalid + "_i3", "t3,t4", evalid.toString, "1231415", "378462511"))

  val selectedU2iActions = List(
    ("4", evalid + "_u0", evalid + "_i1", "1234500", "5"),
    ("3", evalid + "_u3", evalid + "_i0", "1234505", "1"),
    ("4", evalid + "_u1", evalid + "_i3", "1234501", "3"),
    ("4", evalid + "_u1", evalid + "_i2", "1234506", "4"),
    ("2", evalid + "_u1", evalid + "_i0", "1234507", "5"),
    ("3", evalid + "_u2", evalid + "_i3", "1234502", "2"),
    ("4", evalid + "_u0", evalid + "_i2", "1234508", "3"),
    ("4", evalid + "_u2", evalid + "_i0", "1234509", "1"),
    ("4", evalid + "_u0", evalid + "_i1", "1234503", "2"),
    ("4", evalid + "_u3", evalid + "_i3", "1234504", "3"),
    ("4", evalid + "_u3", evalid + "_i3", "1234503", "3"),
    ("4", evalid + "_u2", evalid + "_i3", "1234504", "3"),
    ("4", evalid + "_u1", evalid + "_i3", "1234505", "3"),
    ("4", evalid + "_u0", evalid + "_i3", "1234509", "3"),
    ("view", evalid + "_u0", evalid + "_i0", "1234509", "PIO_NONE"),
    ("like", evalid + "_u1", evalid + "_i2", "1234509", "PIO_NONE"))

  "U2ITrainingTestSplitTimeTest with timeorder=true" should {
    test(List(""), 0.4, 0.3, 0.2, true, appid, evalid,
      items,
      users,
      u2iActions,
      selectedItemsAll,
      selectedUsers,
      selectedU2iActions
    )

  }

  "U2ITrainingTestSplitTimeTest with timeorder=false" should {
    test(List(""), 0.3, 0.2, 0.3, false, appid, evalid,
      items,
      users,
      u2iActions,
      selectedItemsAll,
      selectedUsers,
      selectedU2iActions
    )
  }

  "U2ITrainingTestSplitTimeTest with timeorder=true and validation=0" should {
    test(List(""), 0.6, 0, 0.1, true, appid, evalid,
      items,
      users,
      u2iActions,
      selectedItemsAll,
      selectedUsers,
      selectedU2iActions
    )
  }

  "U2ITrainingTestSplitTimeTest with timeorder=false and validation=0" should {
    test(List(""), 0.6, 0, 0.4, false, appid, evalid,
      items,
      users,
      u2iActions,
      selectedItemsAll,
      selectedUsers,
      selectedU2iActions
    )
  }

}

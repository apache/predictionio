package io.prediction.evaluations.scalding.itemrec.trainingtestsplit

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{Users, Items, U2iActions}
import io.prediction.commons.filepath.TrainingTestSplitFile
import io.prediction.commons.appdata.{User, Item}


class TrainingTestSplitTimeTest extends Specification with TupleConversions {

  def test(itypes: List[String], trainingsize: Int, testsize: Int, timeorder: Boolean, appid: Int, evalid: Int,
      items: List[(String, String, String, String, String)],
      users: List[(String, String, String)],
      u2iActions: List[(String, String, String, String, String)],
      selectedItems: List[(String, String, String, String, String)],
      selectedUsers: List[(String, String, String)],
      selectedU2iActions: List[(String, String, String, String, String)],
      selectedU2iActionsTrainig: List[(String, String, String, String, String)],
      selectedU2iActionsTest: List[(String, String, String, String, String)]
      ) = {

    val dbType = "file"
    val dbName = "testpath/"
    val dbHost = None
    val dbPort = None

    val training_dbType = "file"
    val training_dbName = "trainingsetpath/"
    val training_dbHost = None
    val training_dbPort = None
    
    val test_dbType = "file"
    val test_dbName = "testsetpath/"
    val test_dbHost = None
    val test_dbPort = None

    val hdfsRoot = "testroot/"
    
    val engineid = 4

    // NOTE: since it's random selection and the actual selected size is approximate only,
    //       the actual selected size may not be necessarily equal to the total size
    // add +/- 20% for testing purpse
    val totalSize = (trainingsize + testsize)
    val maxCount = ((scala.math.min((totalSize + 2), 10).toDouble) / 10 * selectedU2iActions.size).toInt
    val minCount = ((totalSize.toDouble - 2) / 10 * selectedU2iActions.size).toInt

    val countExpected = Range(minCount, maxCount+1).toList
    
    JobTest("io.prediction.evaluations.scalding.itemrec.trainingtestsplit.TrainingTestSplitTimePrep")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      .arg("training_dbType", training_dbType)
      .arg("training_dbName", training_dbName)
      .arg("test_dbType", test_dbType)
      .arg("test_dbName", test_dbName)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("evalid", evalid.toString)
      .arg("trainingsize", trainingsize.toString)
      .arg("testsize", testsize.toString)
      .arg("timeorder", timeorder.toString)
      .source(Users(appId=appid, dbType=dbType, dbName=dbName, dbHost=dbHost, dbPort=dbPort).getSource, users)
      .source(Items(appId=appid, itypes=Some(itypes), dbType=dbType, dbName=dbName, dbHost=dbHost, dbPort=dbPort).getSource, items)
      .source(U2iActions(appId=appid, dbType=dbType, dbName=dbName, dbHost=dbHost, dbPort=dbPort).getSource, u2iActions)
      .sink[(String, String, String)](Users(appId=evalid, dbType=training_dbType, dbName=training_dbName, dbHost=training_dbHost, dbPort=training_dbPort).getSource) { outputBuffer =>
        "correctly write trainingUsers" in {
          outputBuffer must containTheSameElementsAs(selectedUsers)
        }
      }
      .sink[(String, String, String, String, String)](Items(appId=evalid, itypes=None, dbType=training_dbType, dbName=training_dbName, dbHost=training_dbHost, dbPort=training_dbPort).getSource) { outputBuffer =>
        "correctly write trainingItems" in {
          outputBuffer must containTheSameElementsAs(selectedItems)
        }
      }
      .sink[(String, String, String, String, String)](U2iActions(appId=evalid,
        dbType="file", dbName=TrainingTestSplitFile(hdfsRoot, appid, engineid, evalid, ""), dbHost=None, dbPort=None).getSource) { outputBuffer =>
        "select some of the u2i actions" in {
          // NOTE: approx only
          selectedU2iActions must containAllOf(outputBuffer.toList) and 
            (outputBuffer.toList.size must be_<=(maxCount)) and
            (outputBuffer.toList.size must be_>=(minCount))
        }
      }
      .sink[(Int)](Tsv(TrainingTestSplitFile(hdfsRoot, appid, engineid, evalid, "u2iCount.tsv"))) { outputBuffer =>
        "write u2i count within the approximate range" in {
          // NOTE: approx only
          outputBuffer must containAnyOf(countExpected)
        }
      }
      .run
      .finish


    JobTest("io.prediction.evaluations.scalding.itemrec.trainingtestsplit.TrainingTestSplitTime")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      .arg("training_dbType", training_dbType)
      .arg("training_dbName", training_dbName)
      .arg("test_dbType", test_dbType)
      .arg("test_dbName", test_dbName)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("evalid", evalid.toString)
      .arg("trainingsize", trainingsize.toString)
      .arg("testsize", testsize.toString)
      .arg("timeorder", timeorder.toString)
      .arg("totalCount", selectedU2iActions.size.toString)
      .source(U2iActions(appId=evalid,
        dbType="file", dbName=TrainingTestSplitFile(hdfsRoot, appid, engineid, evalid, ""), dbHost=None, dbPort=None).getSource, selectedU2iActions)
      .sink[(String, String, String, String, String)](U2iActions(appId=evalid,
        dbType=training_dbType, dbName=training_dbName, dbHost=training_dbHost, dbPort=training_dbPort).getSource) { outputBuffer =>
        "generate training set" in {
          outputBuffer must containTheSameElementsAs(selectedU2iActionsTrainig)
        }
      }
      .sink[(String, String, String, String, String)](U2iActions(appId=evalid,
        dbType=test_dbType, dbName=test_dbName, dbHost=test_dbHost, dbPort=test_dbPort).getSource) { outputBuffer =>
        "generate test set" in {
          outputBuffer must containTheSameElementsAs(selectedU2iActionsTest)
        }
      }
      .run
      .finish

  }

  val appid = 2
  val evalid = 101
  val users = List(
    (appid+"_u0", appid.toString, "123456"), 
    (appid+"_u1", appid.toString, "23456"), 
    (appid+"_u2", appid.toString, "455677"), 
    (appid+"_u3", appid.toString, "876563111"))

  val items = List(
    (appid+"_i0", "t1,t2,t3", appid.toString, "2293300", "1266673"), 
    (appid+"_i1", "t2,t3", appid.toString, "14526361", "12345135"), 
    (appid+"_i2", "t4", appid.toString, "14526361", "23423424"), 
    (appid+"_i3", "t3,t4", appid.toString, "1231415", "378462511"))

  val u2iActions = List(
    ("4", appid+"_u0", appid+"_i1", "1234500", "5"),
    ("3", appid+"_u3", appid+"_i0", "1234505", "1"),
    ("4", appid+"_u1", appid+"_i3", "1234501", "3"),
    ("4", appid+"_u1", appid+"_i2", "1234506", "4"),
    ("2", appid+"_u1", appid+"_i0", "1234507", "5"),
    ("3", appid+"_u2", appid+"_i3", "1234502", "2"),
    ("4", appid+"_u0", appid+"_i2", "1234508", "3"),
    ("4", appid+"_u2", appid+"_i0", "1234509", "1"),
    ("4", appid+"_u0", appid+"_i1", "1234503", "2"),
    ("4", appid+"_u3", appid+"_i3", "1234504", "3"))

  val selectedUsers = List(
    (evalid+"_u0", evalid.toString, "123456"), 
    (evalid+"_u1", evalid.toString, "23456"), 
    (evalid+"_u2", evalid.toString, "455677"), 
    (evalid+"_u3", evalid.toString, "876563111"))

  val selectedItemsAll = List(
    (evalid+"_i0", "t1,t2,t3", evalid.toString, "2293300", "1266673"), 
    (evalid+"_i1", "t2,t3", evalid.toString, "14526361", "12345135"), 
    (evalid+"_i2", "t4", evalid.toString, "14526361", "23423424"), 
    (evalid+"_i3", "t3,t4", evalid.toString, "1231415", "378462511"))

  val selectedU2iActions = List(
    ("4", evalid+"_u0", evalid+"_i1", "1234500", "5"),
    ("3", evalid+"_u3", evalid+"_i0", "1234505", "1"),
    ("4", evalid+"_u1", evalid+"_i3", "1234501", "3"),
    ("4", evalid+"_u1", evalid+"_i2", "1234506", "4"),
    ("2", evalid+"_u1", evalid+"_i0", "1234507", "5"),
    ("3", evalid+"_u2", evalid+"_i3", "1234502", "2"),
    ("4", evalid+"_u0", evalid+"_i2", "1234508", "3"),
    ("4", evalid+"_u2", evalid+"_i0", "1234509", "1"),
    ("4", evalid+"_u0", evalid+"_i1", "1234503", "2"),
    ("4", evalid+"_u3", evalid+"_i3", "1234504", "3"))

  // ceil(6/8 * 10) = 8
  val selectedU2iActionsTrainig = List(
    ("4", evalid+"_u0", evalid+"_i1", "1234500", "5"),
    ("3", evalid+"_u3", evalid+"_i0", "1234505", "1"),
    ("4", evalid+"_u1", evalid+"_i3", "1234501", "3"),
    ("4", evalid+"_u1", evalid+"_i2", "1234506", "4"),
    ("2", evalid+"_u1", evalid+"_i0", "1234507", "5"),
    ("3", evalid+"_u2", evalid+"_i3", "1234502", "2"),
    ("4", evalid+"_u0", evalid+"_i1", "1234503", "2"),
    ("4", evalid+"_u3", evalid+"_i3", "1234504", "3"))

  val selectedU2iActionsTest = List(
    ("4", evalid+"_u0", evalid+"_i2", "1234508", "3"),
    ("4", evalid+"_u2", evalid+"_i0", "1234509", "1"))

  "TrainingTestSplitTimeTest" should {
      test(List(""), 6, 2, true, appid, evalid,
        items,
        users,
        u2iActions,
        selectedItemsAll,
        selectedUsers,
        selectedU2iActions,
        selectedU2iActionsTrainig,
        selectedU2iActionsTest
      ) 
    
  }

}
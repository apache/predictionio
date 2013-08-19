package io.prediction.algorithms.scalding.itemsim.latestrank

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.Items
import io.prediction.commons.scalding.modeldata.{ItemSimScores}
import io.prediction.commons.filepath.{AlgoFile}

class LatestRankTest extends Specification with TupleConversions {
  def test(
    algoid: Int,
    modelSet: Boolean,
    itypes: List[String],
    numSimilarItems: Int,
    items: List[(String, String, String, String)],
    itemSimScores: List[(String, String, Double, String, Int, Boolean)]) = {
    val training_dbType = "file"
    val training_dbName = "testpath/"

    val modeldata_dbType = "file"
    val modeldata_dbName = "testpath/"

    val hdfsRoot = "testpath/"

    val appid = 7
    val engineid = 10
    val evalid = None

    JobTest("io.prediction.algorithms.scalding.itemsim.latestrank.LatestRank")
      .arg("training_dbType", training_dbType)
      .arg("training_dbName", training_dbName)
      .arg("modeldata_dbType", modeldata_dbType)
      .arg("modeldata_dbName", modeldata_dbName)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("algoid", algoid.toString)
      .arg("itypes", itypes)
      .arg("numSimilarItems", numSimilarItems.toString)
      .arg("modelSet", modelSet.toString)
      .source(Items(appId=appid, itypes=Some(itypes), dbType=training_dbType, dbName=training_dbName, dbHost=None, dbPort=None).getSource, items)
      .sink[(String, String, Double, String, Int, Boolean)](ItemSimScores(dbType=modeldata_dbType, dbName=modeldata_dbName, dbHost=None, dbPort=None).getSource) { outputBuffer =>
        "correctly write ItemSimScores" in {
          outputBuffer.toList must containTheSameElementsAs(itemSimScores)
        }
      }
      .run
      .finish
  }

  val algoid = 12
  val modelSet = false
  val itypesT1T2 = List("t1", "t2")
  val itypesAll = List("t1", "t2", "t3", "t4")
  val items = List(("i0", "t1,t2,t3", "19", "123456"), ("i1", "t2,t3", "19", "123457"), ("i2", "t4", "19", "21"), ("i3", "t3,t4", "19", "9876543210"))
  val itemSimScoresT1T2 = List(
    ("i3", "i1", 123457.0, "t2,t3", algoid, modelSet),
    ("i3", "i0", 123456.0, "t1,t2,t3", algoid, modelSet),
    ("i2", "i1", 123457.0, "t2,t3", algoid, modelSet),
    ("i2", "i0", 123456.0, "t1,t2,t3", algoid, modelSet),
    ("i1", "i0", 123456.0, "t1,t2,t3", algoid, modelSet),
    ("i0", "i1", 123457.0, "t2,t3", algoid, modelSet))

  val itemSimScoresAll = List(
    ("i3", "i1", 123457.0, "t2,t3", algoid, modelSet),
    ("i3", "i0", 123456.0, "t1,t2,t3", algoid, modelSet),
    ("i3", "i2", 21.0, "t4", algoid, modelSet),
    ("i2", "i3", 9876543210.0, "t3,t4", algoid, modelSet),
    ("i2", "i1", 123457.0, "t2,t3", algoid, modelSet),
    ("i2", "i0", 123456.0, "t1,t2,t3", algoid, modelSet),
    ("i1", "i3", 9876543210.0, "t3,t4", algoid, modelSet),
    ("i1", "i0", 123456.0, "t1,t2,t3", algoid, modelSet),
    ("i1", "i2", 21.0, "t4", algoid, modelSet),
    ("i0", "i3", 9876543210.0, "t3,t4", algoid, modelSet),
    ("i0", "i1", 123457.0, "t2,t3", algoid, modelSet),
    ("i0", "i2", 21.0, "t4", algoid, modelSet))

  val itemSimScoresAllTop2 = List(
    ("i3", "i1", 123457.0, "t2,t3", algoid, modelSet),
    ("i3", "i0", 123456.0, "t1,t2,t3", algoid, modelSet),
    ("i2", "i3", 9876543210.0, "t3,t4", algoid, modelSet),
    ("i2", "i1", 123457.0, "t2,t3", algoid, modelSet),
    ("i1", "i3", 9876543210.0, "t3,t4", algoid, modelSet),
    ("i1", "i0", 123456.0, "t1,t2,t3", algoid, modelSet),
    ("i0", "i3", 9876543210.0, "t3,t4", algoid, modelSet),
    ("i0", "i1", 123457.0, "t2,t3", algoid, modelSet))

  "latestrank.LatestRank with some itypes and numSimilarItems larger than number of items" should {
    test(algoid, modelSet, itypesT1T2, 500, items, itemSimScoresT1T2)
  }

  "latestrank.LatestRank with all itypes and numSimilarItems larger than number of items" should {
    test(algoid, modelSet, itypesAll, 500, items, itemSimScoresAll)
  }

  "latestrank.LatestRank with all itypes numSimilarItems smaller than number of items" should {
    test(algoid, modelSet, itypesAll, 2, items, itemSimScoresAllTop2)
  }
}

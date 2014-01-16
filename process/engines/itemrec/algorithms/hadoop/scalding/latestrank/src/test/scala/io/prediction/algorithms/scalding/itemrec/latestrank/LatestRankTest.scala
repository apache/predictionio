package io.prediction.algorithms.scalding.itemrec.latestrank

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{ Items, Users }
import io.prediction.commons.scalding.modeldata.{ ItemRecScores }
import io.prediction.commons.filepath.{ AlgoFile }

class LatestRankTest extends Specification with TupleConversions {

  def test(algoid: Int, modelSet: Boolean,
    itypes: List[String],
    numRecommendations: Int,
    items: List[(String, String, String, String)],
    users: List[(String, String)],
    itemRecScores: List[(String, String, String, String, Int, Boolean)]) = {

    val training_dbType = "file"
    val training_dbName = "testpath/"

    val modeldata_dbType = "file"
    val modeldata_dbName = "testpath/"

    val hdfsRoot = "testpath/"

    val appid = 7
    val engineid = 10
    val evalid = None

    JobTest("io.prediction.algorithms.scalding.itemrec.latestrank.LatestRank")
      .arg("training_dbType", training_dbType)
      .arg("training_dbName", training_dbName)
      .arg("modeldata_dbType", modeldata_dbType)
      .arg("modeldata_dbName", modeldata_dbName)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("algoid", algoid.toString)
      .arg("itypes", itypes)
      .arg("numRecommendations", numRecommendations.toString)
      .arg("modelSet", modelSet.toString)
      .source(Items(appId = appid, itypes = Some(itypes),
        dbType = training_dbType, dbName = training_dbName, dbHost = None, dbPort = None).getSource, items)
      .source(Users(appId = appid,
        dbType = training_dbType, dbName = training_dbName, dbHost = None, dbPort = None).getSource, users)
      .sink[(String, String, String, String, Int, Boolean)](ItemRecScores(dbType = modeldata_dbType, dbName = modeldata_dbName, dbHost = None, dbPort = None, algoid = algoid, modelset = modelSet).getSource) { outputBuffer =>

        "correctly write ItemRecScores" in {
          val outputList = outputBuffer.toList

          // convert score to double and compare
          // because double have different string representation
          // eg 9.87654321E9 vs 9876543210.0
          val outputListDouble = outputList.map { x => (x._1, x._2, x._3.split(",").toList.map(_.toDouble), x._4, x._5, x._6) }
          val expectedDouble = itemRecScores.map { x => (x._1, x._2, x._3.split(",").toList.map(_.toDouble), x._4, x._5, x._6) }

          outputListDouble must containTheSameElementsAs(expectedDouble)
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
  val users = List(("u0", "3"), ("u1", "3"), ("u2", "3"), ("u3", "3"))
  val itemRecScoresT1T2 = List(
    ("u0", "i1,i0", "123457.0,123456.0", "[t2,t3],[t1,t2,t3]", algoid, modelSet),
    ("u1", "i1,i0", "123457.0,123456.0", "[t2,t3],[t1,t2,t3]", algoid, modelSet),
    ("u2", "i1,i0", "123457.0,123456.0", "[t2,t3],[t1,t2,t3]", algoid, modelSet),
    ("u3", "i1,i0", "123457.0,123456.0", "[t2,t3],[t1,t2,t3]", algoid, modelSet))

  val itemRecScoresAll = List(
    ("u0", "i3,i1,i0,i2", "9876543210.0,123457.0,123456.0,21.0", "[t3,t4],[t2,t3],[t1,t2,t3],[t4]", algoid, modelSet),
    ("u1", "i3,i1,i0,i2", "9876543210.0,123457.0,123456.0,21.0", "[t3,t4],[t2,t3],[t1,t2,t3],[t4]", algoid, modelSet),
    ("u2", "i3,i1,i0,i2", "9876543210.0,123457.0,123456.0,21.0", "[t3,t4],[t2,t3],[t1,t2,t3],[t4]", algoid, modelSet),
    ("u3", "i3,i1,i0,i2", "9876543210.0,123457.0,123456.0,21.0", "[t3,t4],[t2,t3],[t1,t2,t3],[t4]", algoid, modelSet))

  val itemRecScoresAllTop2 = List(
    ("u0", "i3,i1", "9876543210.0,123457.0", "[t3,t4],[t2,t3]", algoid, modelSet),
    ("u1", "i3,i1", "9876543210.0,123457.0", "[t3,t4],[t2,t3]", algoid, modelSet),
    ("u2", "i3,i1", "9876543210.0,123457.0", "[t3,t4],[t2,t3]", algoid, modelSet),
    ("u3", "i3,i1", "9876543210.0,123457.0", "[t3,t4],[t2,t3]", algoid, modelSet))

  "latestrank.LatestRank with some itypes and numRecommendations larger than number of items" should {

    test(algoid, modelSet, itypesT1T2, 500, items, users, itemRecScoresT1T2)

  }

  "latestrank.LatestRank with all itypes and numRecommendations larger than number of items" should {

    test(algoid, modelSet, itypesAll, 500, items, users, itemRecScoresAll)

  }

  "latestrank.LatestRank with all itypes numRecommendations smaller than number of items" should {

    test(algoid, modelSet, itypesAll, 2, items, users, itemRecScoresAllTop2)

  }

}

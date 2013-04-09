package io.prediction.algorithms.scalding.itemrec.latestrank

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{Items, Users}
import io.prediction.commons.scalding.modeldata.{ItemRecScores}
import io.prediction.commons.filepath.{AlgoFile}

class LatestRankTest extends Specification with TupleConversions {

  def test(algoid: Int, modelSet: Boolean,
    itypes: List[String], 
    items: List[(String, String, String, String)],
    users: List[(String, String)],
    itemRecScores: List[(String, String, Double, String, Int, Boolean)]
    ) = {

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
      .arg("modelSet", modelSet.toString)
      .source(Items(appId=appid, itypes=Some(itypes), 
        dbType=training_dbType, dbName=training_dbName, dbHost=None, dbPort=None).getSource, items)
      .source(Users(appId=appid,
        dbType=training_dbType, dbName=training_dbName, dbHost=None, dbPort=None).getSource, users)
      .sink[(String, String, Double, String, Int, Boolean)](ItemRecScores(dbType=modeldata_dbType, dbName=modeldata_dbName, dbHost=None, dbPort=None).getSource) { outputBuffer =>

        "correctly write ItemRecScores" in {
          outputBuffer.toList must containTheSameElementsAs(itemRecScores)
        }

      }
      .run
      .finish
  }

  "latestrank.LatestRank" should {

    val algoid = 12
    val modelSet = false
    val itypes = List("t1", "t2")
    val items = List(("i0", "t1,t2,t3", "19", "123456"), ("i1", "t2,t3", "19", "123457"), ("i2", "t4", "19", "21"), ("i3", "t3,t4", "19", "9876543210"))
    val users = List(("u0", "3"), ("u1", "3"), ("u2", "3"), ("u3", "3"))
    val itemRecScores = List(
      ("u0", "i0", 123456.0, "t1,t2,t3", algoid, modelSet),
      ("u0", "i1", 123457.0, "t2,t3", algoid, modelSet),
      ("u1", "i0", 123456.0, "t1,t2,t3", algoid, modelSet),
      ("u1", "i1", 123457.0, "t2,t3", algoid, modelSet),
      ("u2", "i0", 123456.0, "t1,t2,t3", algoid, modelSet),
      ("u2", "i1", 123457.0, "t2,t3", algoid, modelSet),
      ("u3", "i0", 123456.0, "t1,t2,t3", algoid, modelSet),
      ("u3", "i1", 123457.0, "t2,t3", algoid, modelSet))

    test(algoid, modelSet, itypes, items, users, itemRecScores)

  }

}

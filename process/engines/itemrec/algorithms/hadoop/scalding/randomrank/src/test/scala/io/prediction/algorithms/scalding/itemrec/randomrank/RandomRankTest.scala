package io.prediction.algorithms.scalding.itemrec.randomrank

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{ Items, Users }
import io.prediction.commons.scalding.modeldata.{ ItemRecScores }
import io.prediction.commons.filepath.{ AlgoFile }

class RandomRankTest extends Specification with TupleConversions {

  def test(algoid: Int, modelSet: Boolean,
    itypes: List[String],
    numRecommendations: Int,
    items: List[(String, String)],
    users: List[(String, String)],
    itemRecScores: List[(String, String, Double, String, Int, Boolean)]) = {

    val training_dbType = "file"
    val training_dbName = "testpath/"

    val modeldata_dbType = "file"
    val modeldata_dbName = "testpath/"

    val hdfsRoot = "testpath/"

    val appid = 7
    val engineid = 10
    val evalid = None

    JobTest("io.prediction.algorithms.scalding.itemrec.randomrank.RandomRank")
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
      /*
      .sink[(String, String)](Tsv(AlgoFile(hdfsRoot, appid, engineid, algoid, evalid, "itemRecScores.tsv"))) { outputBuffer =>

        def takeOutScoresAndItypes(d: List[(String, String, Double, String, Int, Boolean)]) = {
          d map {x => (x._1, x._2) }
        }

        "generate correct user and item pairs in AlgoFile" in {
          val result = outputBuffer.toList
          val expected = takeOutScoresAndItypes(itemRecScores)

          result must containTheSameElementsAs(expected)
        }

      }*/
      .sink[(String, String, Double, String, Int, Boolean)](ItemRecScores(dbType = modeldata_dbType, dbName = modeldata_dbName, dbHost = None, dbPort = None).getSource) { outputBuffer =>

        def takeOutScores(d: List[(String, String, Double, String, Int, Boolean)]) = {
          d map { x => (x._1, x._2, x._4, x._5, x._6) }
        }

        def getScoresOnly(d: List[(String, String, Double, String, Int, Boolean)]) = {
          d map { x => x._3 }
        }

        "generate correct user and item pairs in modeldata" in {

          // don't check scores since they are random
          val result = takeOutScores(outputBuffer.toList)
          val expected = takeOutScores(itemRecScores)

          result must containTheSameElementsAs(expected)

        }

        "generate different scores for each pair in modeldata" in {
          // very simple way to check if the scores are random
          // (just to check if they are different)

          val scoresList: List[Double] = getScoresOnly(outputBuffer.toList)
          val scoresSet = scoresList.toSet

          scoresSet.size must be_==(scoresList.size)

        }
      }
      .run
      .finish
  }

  "randomrank.RandomRank with selected itypes" should {

    val algoid = 12
    val modelSet = false
    val itypes = List("t1", "t2")
    val items = List(("i0", "t1,t2,t3"), ("i1", "t2,t3"), ("i2", "t4"), ("i3", "t3,t4"))
    val users = List(("u0", "3"), ("u1", "3"), ("u2", "3"), ("u3", "3"))
    val itemRecScores = List(
      ("u0", "i0", 0.0, "t1,t2,t3", algoid, modelSet),
      ("u0", "i1", 0.0, "t2,t3", algoid, modelSet),
      ("u1", "i0", 0.0, "t1,t2,t3", algoid, modelSet),
      ("u1", "i1", 0.0, "t2,t3", algoid, modelSet),
      ("u2", "i0", 0.0, "t1,t2,t3", algoid, modelSet),
      ("u2", "i1", 0.0, "t2,t3", algoid, modelSet),
      ("u3", "i0", 0.0, "t1,t2,t3", algoid, modelSet),
      ("u3", "i1", 0.0, "t2,t3", algoid, modelSet))

    test(algoid, modelSet, itypes, 500, items, users, itemRecScores)

  }

  "randomrank.RandomRank with all itypes" should {

    val algoid = 12
    val modelSet = false
    val itypes = List("")
    val items = List(("i0", "t1,t2,t3"), ("i1", "t2,t3"), ("i2", "t4"), ("i3", "t3,t4"))
    val users = List(("u0", "3"), ("u1", "3"), ("u2", "3"), ("u3", "3"))
    val itemRecScores = List(
      ("u0", "i0", 0.0, "t1,t2,t3", algoid, modelSet),
      ("u0", "i1", 0.0, "t2,t3", algoid, modelSet),
      ("u0", "i2", 0.0, "t4", algoid, modelSet),
      ("u0", "i3", 0.0, "t3,t4", algoid, modelSet),
      ("u1", "i0", 0.0, "t1,t2,t3", algoid, modelSet),
      ("u1", "i1", 0.0, "t2,t3", algoid, modelSet),
      ("u1", "i2", 0.0, "t4", algoid, modelSet),
      ("u1", "i3", 0.0, "t3,t4", algoid, modelSet),
      ("u2", "i0", 0.0, "t1,t2,t3", algoid, modelSet),
      ("u2", "i1", 0.0, "t2,t3", algoid, modelSet),
      ("u2", "i2", 0.0, "t4", algoid, modelSet),
      ("u2", "i3", 0.0, "t3,t4", algoid, modelSet),
      ("u3", "i0", 0.0, "t1,t2,t3", algoid, modelSet),
      ("u3", "i1", 0.0, "t2,t3", algoid, modelSet),
      ("u3", "i2", 0.0, "t4", algoid, modelSet),
      ("u3", "i3", 0.0, "t3,t4", algoid, modelSet))

    test(algoid, modelSet, itypes, 500, items, users, itemRecScores)

  }

  // TODO: test with smaller number of numRecommendations (but can't know expected result beacause the score is random...)

}

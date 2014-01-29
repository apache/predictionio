package io.prediction.algorithms.scalding.mahout.itemrec

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.filepath.{ AlgoFile, DataFile }
import io.prediction.commons.scalding.modeldata.ItemRecScores
import cascading.tuple.{ Tuple, TupleEntry, TupleEntryIterator, Fields }

class ModelConstructorTest extends Specification with TupleConversions {

  def test(unseenOnly: Boolean, numRecommendations: Int,
    items: List[(String, String, String)],
    users: List[(String, String)],
    predicted: List[(String, String)],
    ratings: List[(String, String, String)],
    output: List[(String, String, String, String)]) = {

    val appid = 3
    val engineid = 4
    val algoid = 7
    val evalid = None
    val modelSet = true

    val dbType = "file"
    val dbName = "testpath/"
    val dbHost = None
    val dbPort = None
    val hdfsRoot = "testroot/"

    val itemRecScores = output map { case (uid, iid, score, itypes) => (uid, iid, score, itypes, algoid, modelSet) }

    JobTest("io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("algoid", algoid.toString)
      .arg("modelSet", modelSet.toString)
      .arg("unseenOnly", unseenOnly.toString)
      .arg("numRecommendations", numRecommendations.toString)
      .source(Tsv(AlgoFile(hdfsRoot, appid, engineid, algoid, evalid, "predicted.tsv"), new Fields("uindex", "predicted")), predicted)
      .source(Csv(DataFile(hdfsRoot, appid, engineid, algoid, evalid, "ratings.csv"), ",", new Fields("uindexR", "iindexR", "ratingR")), ratings)
      .source(Tsv(DataFile(hdfsRoot, appid, engineid, algoid, evalid, "itemsIndex.tsv")), items)
      .source(Tsv(DataFile(hdfsRoot, appid, engineid, algoid, evalid, "usersIndex.tsv")), users)
      .sink[(String, String, String, String, Int, Boolean)](ItemRecScores(dbType = dbType, dbName = dbName, dbHost = dbHost, dbPort = dbPort, algoid = algoid, modelset = modelSet).getSource) { outputBuffer =>
        "correctly write model data to a file" in {
          outputBuffer.toList must containTheSameElementsAs(itemRecScores)
        }
      }
      .run
      .finish

  }

  val test1Items = List(("0", "i0", "t1,t2,t3"), ("1", "i1", "t1,t2"), ("2", "i2", "t2,t3"), ("3", "i3", "t2"))

  val test1Users = List(("0", "u0"), ("1", "u1"), ("2", "u2"), ("3", "u3"))

  val test1Predicted = List(("0", "[1:0.123,2:0.456]"), ("1", "[0:1.2]"))
  val test1PredictedWithSeenItems = List(("0", "[1:0.123,2:0.456,0:4.321,3:1.234]"), ("1", "[0:1.2]"))

  val test1Ratings = List(("0", "0", "2.3"), ("0", "3", "4.56"))

  val test1Output = List(
    ("u0", "i3,i0,i2,i1", "4.56,2.3,0.456,0.123", "[t2],[t1,t2,t3],[t2,t3],[t1,t2]"),
    ("u1", "i0", "1.2", "[t1,t2,t3]"))

  // only output 2 recommendations
  val test1Output2 = List(
    ("u0", "i3,i0", "4.56,2.3", "[t2],[t1,t2,t3]"),
    ("u1", "i0", "1.2", "[t1,t2,t3]"))

  val test1OutputUnseenOnly = List(
    ("u0", "i2,i1", "0.456,0.123", "[t2,t3],[t1,t2]"),
    ("u1", "i0", "1.2", "[t1,t2,t3]"))

  // only output 1 recommendation
  val test1OutputUnseenOnly1 = List(
    ("u0", "i2", "0.456", "[t2,t3]"),
    ("u1", "i0", "1.2", "[t1,t2,t3]"))

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=false and numRecommendations=100" should {

    test(false, 100, test1Items, test1Users, test1Predicted, test1Ratings, test1Output)

  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=false and numRecommendations=2" should {

    test(false, 2, test1Items, test1Users, test1Predicted, test1Ratings, test1Output2)

  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=true and numRecommendations=100" should {

    test(true, 100, test1Items, test1Users, test1Predicted, test1Ratings, test1OutputUnseenOnly)

  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=true and numRecommendations=1" should {

    test(true, 1, test1Items, test1Users, test1Predicted, test1Ratings, test1OutputUnseenOnly1)

  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=false, numRecommendations=100 and seen items in predicted results" should {

    test(false, 100, test1Items, test1Users, test1PredictedWithSeenItems, test1Ratings, test1Output)

  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=true, numRecommendations=100 and seen items in predicted results" should {

    test(true, 100, test1Items, test1Users, test1PredictedWithSeenItems, test1Ratings, test1OutputUnseenOnly)

  }

  val test2Items = List(("0", "i0", "t1,t2,t3"), ("1", "i1", "t1,t2"), ("2", "i2", "t2,t3"), ("3", "i3", "t2"))

  val test2Users = List(("0", "u0"), ("1", "u1"), ("2", "u2"), ("3", "u3"))

  val test2Predicted = List(("0", "[1:123,2:9]"), ("1", "[0:1]"))

  val test2Ratings = List(("0", "0", "2"), ("0", "3", "88"))

  val test2Output = List(
    ("u0", "i1,i3,i2,i0", "123.0,88.0,9.0,2.0", "[t1,t2],[t2],[t2,t3],[t1,t2,t3]"),
    ("u1", "i0", "1.0", "[t1,t2,t3]"))

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=false and numRecommendations=100 (score should not be compared as string)" should {

    test(false, 100, test2Items, test2Users, test2Predicted, test2Ratings, test2Output)

  }

}

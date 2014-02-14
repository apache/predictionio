package io.prediction.algorithms.scalding.mahout.itemrec

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.filepath.{ AlgoFile, DataFile }
import io.prediction.commons.scalding.modeldata.ItemRecScores
import cascading.tuple.{ Tuple, TupleEntry, TupleEntryIterator, Fields }

class ModelConstructorTest extends Specification with TupleConversions {

  val appid = 3

  def test(unseenOnly: Boolean, numRecommendations: Int, recommendationTime: Long,
    items: List[(String, String, String, String, String)], //(iindex, iid, itypes, starttime, endtime)
    users: List[(String, String)],
    predicted: List[(String, String)],
    ratings: List[(String, String, String)],
    output: List[(String, String, String, String)]) = {

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
      .arg("recommendationTime", recommendationTime.toString)
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

  def testWithBooleanData(unseenOnly: Boolean, numRecommendations: Int, recommendationTime: Long,
    items: List[(String, String, String, String, String)], //(iindex, iid, itypes, starttime, endtime)
    users: List[(String, String)],
    predicted: List[(String, String)],
    ratings: List[(String, String, String)],
    output: List[(String, String, String, String)],
    booleanData: Boolean) = {

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
      .arg("recommendationTime", recommendationTime.toString)
      .arg("booleanData", booleanData.toString)
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

  def testWithImplicitFeedback(unseenOnly: Boolean, numRecommendations: Int, recommendationTime: Long,
    items: List[(String, String, String, String, String)], //(iindex, iid, itypes, starttime, endtime)
    users: List[(String, String)],
    predicted: List[(String, String)],
    ratings: List[(String, String, String)],
    output: List[(String, String, String, String)],
    implicitFeedback: Boolean) = {

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
      .arg("recommendationTime", recommendationTime.toString)
      .arg("implicitFeedback", implicitFeedback.toString)
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

  val noEndtime = "PIO_NONE"

  /* test 1*/
  val test1Items = List(
    ("0", "i0", "t1,t2,t3", "12346", noEndtime),
    ("1", "i1", "t1,t2", "12347", noEndtime),
    ("2", "i2", "t2,t3", "12348", noEndtime),
    ("3", "i3", "t2", "12349", noEndtime))

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
    test(false, 100, 1234567890, test1Items, test1Users, test1Predicted, test1Ratings, test1Output)
  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=false and numRecommendations=2" should {
    test(false, 2, 1234567890, test1Items, test1Users, test1Predicted, test1Ratings, test1Output2)
  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=true and numRecommendations=100" should {
    test(true, 100, 1234567890, test1Items, test1Users, test1Predicted, test1Ratings, test1OutputUnseenOnly)
  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=true and numRecommendations=1" should {
    test(true, 1, 1234567890, test1Items, test1Users, test1Predicted, test1Ratings, test1OutputUnseenOnly1)
  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=false, numRecommendations=100 and seen items in predicted results" should {
    test(false, 100, 1234567890, test1Items, test1Users, test1PredictedWithSeenItems, test1Ratings, test1Output)
  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=true, numRecommendations=100 and seen items in predicted results" should {
    test(true, 100, 1234567890, test1Items, test1Users, test1PredictedWithSeenItems, test1Ratings, test1OutputUnseenOnly)
  }

  /* booleanData */
  "mahout.itemrec.itembased ModelConstructor with unseenOnly=true and numRecommendations=100 and booleanData=true" should {
    testWithBooleanData(true, 100, 1234567890, test1Items, test1Users, test1Predicted, test1Ratings, test1OutputUnseenOnly, true)
  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=true and numRecommendations=100 and booleanData=false" should {
    testWithBooleanData(true, 100, 1234567890, test1Items, test1Users, test1Predicted, test1Ratings, test1OutputUnseenOnly, false)
  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=false and numRecommendations=100 and booleanData=true" should {
    // should only generate unseen data if booleanData=true although unseenOnly=false
    testWithBooleanData(false, 100, 1234567890, test1Items, test1Users, test1Predicted, test1Ratings, test1OutputUnseenOnly, true)
  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=false and numRecommendations=100 and booleanData=false" should {
    testWithBooleanData(false, 100, 1234567890, test1Items, test1Users, test1Predicted, test1Ratings, test1Output, false)
  }

  /* implicitFeedback */
  "mahout.itemrec.itembased ModelConstructor with unseenOnly=true and numRecommendations=100 and implicitFeedback=true" should {
    testWithImplicitFeedback(true, 100, 1234567890, test1Items, test1Users, test1Predicted, test1Ratings, test1OutputUnseenOnly, true)
  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=true and numRecommendations=100 and implicitFeedback=false" should {
    testWithImplicitFeedback(true, 100, 1234567890, test1Items, test1Users, test1Predicted, test1Ratings, test1OutputUnseenOnly, false)
  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=false and numRecommendations=100 and implicitFeedback=true" should {
    // should only generate unseen data if testWithImplicitFeedback=true although unseenOnly=false
    testWithImplicitFeedback(false, 100, 1234567890, test1Items, test1Users, test1Predicted, test1Ratings, test1OutputUnseenOnly, true)
  }

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=false and numRecommendations=100 and implicitFeedback=false" should {
    testWithImplicitFeedback(false, 100, 1234567890, test1Items, test1Users, test1Predicted, test1Ratings, test1Output, false)
  }

  /* test 2: test double comparision */
  val test2Items = List(
    ("0", "i0", "t1,t2,t3", "12346", noEndtime),
    ("1", "i1", "t1,t2", "12347", noEndtime),
    ("2", "i2", "t2,t3", "12348", noEndtime),
    ("3", "i3", "t2", "12349", noEndtime))

  val test2Users = List(("0", "u0"), ("1", "u1"), ("2", "u2"), ("3", "u3"))

  val test2Predicted = List(("0", "[1:123,2:9]"), ("1", "[0:1]"))

  val test2Ratings = List(("0", "0", "2"), ("0", "3", "88"))

  val test2Output = List(
    ("u0", "i1,i3,i2,i0", "123.0,88.0,9.0,2.0", "[t1,t2],[t2],[t2,t3],[t1,t2,t3]"),
    ("u1", "i0", "1.0", "[t1,t2,t3]"))

  "mahout.itemrec.itembased ModelConstructor with unseenOnly=false and numRecommendations=100 (score should not be compared as string)" should {

    test(false, 100, 1234567890, test2Items, test2Users, test2Predicted, test2Ratings, test2Output)

  }

  /* test3: test starttime and endtime */

  // starttime, endtime
  // i0  A |---------|
  // i1    B |---------|E
  // i2       C|---------|
  // i3           |---------|
  //               D        F G  

  val tA = 123122
  val tB = 123123
  val tC = 123457
  val tD = 123679
  val tE = 543322
  val tF = 543654
  val tG = 543655

  val test3Items = List(
    ("0", "i0", "t1,t2,t3", "123123", "543210"),
    ("1", "i1", "t1,t2", "123456", "543321"),
    ("2", "i2", "t2,t3", "123567", "543432"),
    ("3", "i3", "t2", "123678", "543654"))

  val test3Users = List(("0", "u0"), ("1", "u1"), ("2", "u2"), ("3", "u3"))

  val test3Predicted = List(("0", "[1:123,2:9]"), ("1", "[0:1]"))

  val test3Ratings = List(
    ("0", "0", "2"), ("0", "3", "88"),
    ("1", "2", "3"),
    ("2", "3", "4"))

  val test3Output = List(
    ("u0", "i1,i3,i2,i0", "123.0,88.0,9.0,2.0", "[t1,t2],[t2],[t2,t3],[t1,t2,t3]"),
    ("u1", "i2,i0", "3.0,1.0", "[t2,t3],[t1,t2,t3]"),
    ("u2", "i3", "4.0", "[t2]"))

  val test3OutputEmpty = List()

  val test3Outputi0 = List(
    ("u0", "i0", "2.0", "[t1,t2,t3]"),
    ("u1", "i0", "1.0", "[t1,t2,t3]"))

  val test3Outputi0i1 = List(
    ("u0", "i1,i0", "123.0,2.0", "[t1,t2],[t1,t2,t3]"),
    ("u1", "i0", "1.0", "[t1,t2,t3]"))

  val test3Outputi2i3 = List(
    ("u0", "i3,i2", "88.0,9.0", "[t2],[t2,t3]"),
    ("u1", "i2", "3.0", "[t2,t3]"),
    ("u2", "i3", "4.0", "[t2]"))

  "unseenOnly=false, numRecommendations=100 and recommendationTime < all item starttime" should {
    test(false, 100, tA, test3Items, test3Users, test3Predicted, test3Ratings, test3OutputEmpty)
  }

  "unseenOnly=false, numRecommendations=100 and recommendationTime == earliest starttime" should {
    test(false, 100, tB, test3Items, test3Users, test3Predicted, test3Ratings, test3Outputi0)
  }

  "unseenOnly=false, numRecommendations=100 and recommendationTime > some items starttime" should {
    test(false, 100, tC, test3Items, test3Users, test3Predicted, test3Ratings, test3Outputi0i1)
  }

  "unseenOnly=false, numRecommendations=100 and recommendationTime > all item starttime and < all item endtime" should {
    test(false, 100, tD, test3Items, test3Users, test3Predicted, test3Ratings, test3Output)
  }

  "unseenOnly=false, numRecommendations=100 and recommendationTime > some item endtime" should {
    test(false, 100, tE, test3Items, test3Users, test3Predicted, test3Ratings, test3Outputi2i3)
  }

  "unseenOnly=false, numRecommendations=100 and recommendationTime == last item endtime" should {
    test(false, 100, tF, test3Items, test3Users, test3Predicted, test3Ratings, test3OutputEmpty)
  }

  "unseenOnly=false, numRecommendations=100 and recommendationTime > last item endtime" should {
    test(false, 100, tG, test3Items, test3Users, test3Predicted, test3Ratings, test3OutputEmpty)
  }

}

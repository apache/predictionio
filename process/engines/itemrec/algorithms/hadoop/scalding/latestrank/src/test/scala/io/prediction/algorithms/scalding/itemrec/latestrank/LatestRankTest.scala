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
    recommendationTime: Long,
    items: List[(String, String, String, String, String, String)], // id, itypes, appid, starttime, ct, endtime
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
      .arg("recommendationTime", recommendationTime.toString)
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

  val largeNumber: Long = scala.Long.MaxValue // larger than any item starttime
  val noEndtime = "PIO_NONE"

  /* test 1 */
  val test1Algoid = 12
  val test1ModelSet = false
  val test1ItypesT1T2 = List("t1", "t2")
  val test1ItypesAll = List("t1", "t2", "t3", "t4")
  val test1Items = List(
    ("i0", "t1,t2,t3", "19", "123456", "345678", noEndtime),
    ("i1", "t2,t3", "19", "123457", "567890", noEndtime),
    ("i2", "t4", "19", "21", "88", noEndtime),
    ("i3", "t3,t4", "19", "9876543210", "67890", noEndtime))
  val test1Users = List(("u0", "3"), ("u1", "3"), ("u2", "3"), ("u3", "3"))
  val test1ItemRecScoresT1T2 = List(
    ("u0", "i1,i0", "123457.0,123456.0", "[t2,t3],[t1,t2,t3]", test1Algoid, test1ModelSet),
    ("u1", "i1,i0", "123457.0,123456.0", "[t2,t3],[t1,t2,t3]", test1Algoid, test1ModelSet),
    ("u2", "i1,i0", "123457.0,123456.0", "[t2,t3],[t1,t2,t3]", test1Algoid, test1ModelSet),
    ("u3", "i1,i0", "123457.0,123456.0", "[t2,t3],[t1,t2,t3]", test1Algoid, test1ModelSet))

  val test1ItemRecScoresAll = List(
    ("u0", "i3,i1,i0,i2", "9876543210.0,123457.0,123456.0,21.0", "[t3,t4],[t2,t3],[t1,t2,t3],[t4]", test1Algoid, test1ModelSet),
    ("u1", "i3,i1,i0,i2", "9876543210.0,123457.0,123456.0,21.0", "[t3,t4],[t2,t3],[t1,t2,t3],[t4]", test1Algoid, test1ModelSet),
    ("u2", "i3,i1,i0,i2", "9876543210.0,123457.0,123456.0,21.0", "[t3,t4],[t2,t3],[t1,t2,t3],[t4]", test1Algoid, test1ModelSet),
    ("u3", "i3,i1,i0,i2", "9876543210.0,123457.0,123456.0,21.0", "[t3,t4],[t2,t3],[t1,t2,t3],[t4]", test1Algoid, test1ModelSet))

  val test1ItemRecScoresAllTop2 = List(
    ("u0", "i3,i1", "9876543210.0,123457.0", "[t3,t4],[t2,t3]", test1Algoid, test1ModelSet),
    ("u1", "i3,i1", "9876543210.0,123457.0", "[t3,t4],[t2,t3]", test1Algoid, test1ModelSet),
    ("u2", "i3,i1", "9876543210.0,123457.0", "[t3,t4],[t2,t3]", test1Algoid, test1ModelSet),
    ("u3", "i3,i1", "9876543210.0,123457.0", "[t3,t4],[t2,t3]", test1Algoid, test1ModelSet))

  "latestrank.LatestRank with some itypes and numRecommendations larger than number of items" should {

    test(test1Algoid, test1ModelSet, test1ItypesT1T2, 500, largeNumber, test1Items, test1Users, test1ItemRecScoresT1T2)

  }

  "latestrank.LatestRank with all itypes and numRecommendations larger than number of items" should {

    test(test1Algoid, test1ModelSet, test1ItypesAll, 500, largeNumber, test1Items, test1Users, test1ItemRecScoresAll)

  }

  "latestrank.LatestRank with all itypes numRecommendations smaller than number of items" should {

    test(test1Algoid, test1ModelSet, test1ItypesAll, 2, largeNumber, test1Items, test1Users, test1ItemRecScoresAllTop2)

  }

  /* test 2: test starttime and endtime */
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

  val test2Algoid = 12
  val test2ModelSet = false

  val test2ItypesAll = List("t1", "t2", "t3", "t4")
  val test2Items = List(
    ("i0", "t1,t2,t3", "19", "123123", "4", "543210"),
    ("i1", "t2,t3", "19", "123456", "5", "543321"),
    ("i2", "t4", "19", "123567", "6", "543432"),
    ("i3", "t3,t4", "19", "123678", "7", "543654"))

  val test2Users = List(("u0", "3"), ("u1", "3"), ("u2", "3"), ("u3", "3"))

  val test2ItemRecScoresAll = List(
    ("u0", "i3,i2,i1,i0", "123678.0,123567.0,123456.0,123123.0", "[t3,t4],[t4],[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u1", "i3,i2,i1,i0", "123678.0,123567.0,123456.0,123123.0", "[t3,t4],[t4],[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u2", "i3,i2,i1,i0", "123678.0,123567.0,123456.0,123123.0", "[t3,t4],[t4],[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u3", "i3,i2,i1,i0", "123678.0,123567.0,123456.0,123123.0", "[t3,t4],[t4],[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet))

  val test2ItemRecScoresEmpty = List()

  val test2ItemRecScoresi0 = List(
    ("u0", "i0", "123123.0", "[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u1", "i0", "123123.0", "[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u2", "i0", "123123.0", "[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u3", "i0", "123123.0", "[t1,t2,t3]", test2Algoid, test2ModelSet))

  val test2ItemRecScoresi0i1 = List(
    ("u0", "i1,i0", "123456.0,123123.0", "[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u1", "i1,i0", "123456.0,123123.0", "[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u2", "i1,i0", "123456.0,123123.0", "[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u3", "i1,i0", "123456.0,123123.0", "[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet))

  val test2ItemRecScoresi2i3 = List(
    ("u0", "i3,i2", "123678.0,123567.0", "[t3,t4],[t4]", test2Algoid, test2ModelSet),
    ("u1", "i3,i2", "123678.0,123567.0", "[t3,t4],[t4]", test2Algoid, test2ModelSet),
    ("u2", "i3,i2", "123678.0,123567.0", "[t3,t4],[t4]", test2Algoid, test2ModelSet),
    ("u3", "i3,i2", "123678.0,123567.0", "[t3,t4],[t4]", test2Algoid, test2ModelSet))

  "recommendationTime < all item starttime" should {
    test(test2Algoid, test2ModelSet, test2ItypesAll, 500, tA, test2Items, test2Users, test2ItemRecScoresEmpty)
  }

  "recommendationTime == earliest starttime" should {
    test(test2Algoid, test2ModelSet, test2ItypesAll, 500, tB, test2Items, test2Users, test2ItemRecScoresi0)
  }

  "recommendationTime > some items starttime" should {
    test(test2Algoid, test2ModelSet, test2ItypesAll, 500, tC, test2Items, test2Users, test2ItemRecScoresi0i1)
  }

  "recommendationTime > all item starttime and < all item endtime" should {
    test(test2Algoid, test2ModelSet, test2ItypesAll, 500, tD, test2Items, test2Users, test2ItemRecScoresAll)
  }

  "recommendationTime > some item endtime" should {
    test(test2Algoid, test2ModelSet, test2ItypesAll, 500, tE, test2Items, test2Users, test2ItemRecScoresi2i3)
  }

  "recommendationTime == last item endtime" should {
    test(test2Algoid, test2ModelSet, test2ItypesAll, 500, tA, test2Items, test2Users, test2ItemRecScoresEmpty)
  }

  "recommendationTime > last item endtime" should {
    test(test2Algoid, test2ModelSet, test2ItypesAll, 500, tA, test2Items, test2Users, test2ItemRecScoresEmpty)
  }

}

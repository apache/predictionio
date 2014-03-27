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
      .arg("recommendationTime", recommendationTime.toString)
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
      .sink[(String, String, String, String, Int, Boolean)](ItemRecScores(dbType = modeldata_dbType, dbName = modeldata_dbName, dbHost = None, dbPort = None, algoid = algoid, modelset = modelSet).getSource) { outputBuffer =>

        def takeOutScores(d: List[(String, String, String, String, Int, Boolean)]) = {
          // don't check score and itypes.
          // for iids, don't check order. convert to set
          d map { x => (x._1, x._2.split(",").toSet, x._5, x._6) }
        }

        def getScoresOnly(d: List[(String, String, String, String, Int, Boolean)]) = {
          d flatMap { x => x._3.split(",").toList.map(_.toDouble) }
        }

        def getIids(d: List[(String, String, String, String, Int, Boolean)]) = {
          // List(List("i0", "i1"), List("i1", "i0"), ...)

          d map { x => x._2.split(",").toList }
        }

        def getItypes(d: List[(String, String, String, String, Int, Boolean)]) = {
          //("u0", "i0,i1", "0.0,0.0", "[t1,t2,t3],[t2,t3]", algoid, modelSet),
          // => List( List( (i0, List(t1,t2,t3)) , (i1, List(t2,t3)) )
          d.map { x =>
            val itypesList = x._4.split("],").toList.map(x => x.stripPrefix("[").stripSuffix("]").split(",").toList)
            val iidList = x._2.split(",").toList

            iidList zip itypesList
          }
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

        if (getIids(itemRecScores).flatMap { x => x }.toSet.size > 3) {
          // only check this if the iids in itemRecScores are more than 1
          "not generate same order of iid for all uid group" in {
            if (!(getIids(outputBuffer.toList).toSet.size > 1)) {
              println(outputBuffer)
              println(getIids(outputBuffer.toList).toSet)
            }
            getIids(outputBuffer.toList).toSet.size must be_>(1)

          }
        }

        "itypes order match the iids order" in {

          // extract (iid, itypes) from the output
          val itypesList = getItypes(outputBuffer.toList)
          val itemsMap = items.map(x =>
            (x._1, x)).toMap

          // use the iid only and contruct the (iid, itypes)
          val expected = getIids(outputBuffer.toList).map(x =>
            // x is List of iid
            // create the List of item types using the iid
            x.map(x => (x, itemsMap(x)._2.split(",").toList))
          )

          itypesList must be_==(expected)

        }

      }
      .run
      .finish
  }

  val largeNumber: Long = scala.Long.MaxValue // larger than any item starttime
  val noEndtime = "PIO_NONE"

  "randomrank.RandomRank with selected itypes" should {

    val algoid = 12
    val modelSet = false
    val itypes = List("t1", "t2")
    val items = List(
      ("i0", "t1,t2,t3", "19", "123456", "345678", noEndtime),
      ("i1", "t2,t3", "19", "123457", "567890", noEndtime),
      ("i2", "t4", "19", "21", "88", noEndtime),
      ("i3", "t3,t4", "19", "9876543210", "67890", noEndtime))

    val users = List(("u0", "3"), ("u1", "3"), ("u2", "3"), ("u3", "3"))
    val itemRecScores = List(
      ("u0", "i0,i1", "0.0,0.0", "[t1,t2,t3],[t2,t3]", algoid, modelSet),
      ("u1", "i0,i1", "0.0,0.0", "[t1,t2,t3],[t2,t3]", algoid, modelSet),
      ("u2", "i0,i1", "0.0,0.0", "[t1,t2,t3],[t2,t3]", algoid, modelSet),
      ("u3", "i0,i1", "0.0,0.0", "[t1,t2,t3],[t2,t3]", algoid, modelSet))

    test(algoid, modelSet, itypes, 500, largeNumber, items, users, itemRecScores)

  }

  "randomrank.RandomRank with all itypes" should {

    val algoid = 12
    val modelSet = false
    val itypes = List("")
    val items = List(
      ("i0", "t1,t2,t3", "19", "123456", "345678", noEndtime),
      ("i1", "t2,t3", "19", "123457", "567890", noEndtime),
      ("i2", "t4", "19", "21", "88", noEndtime),
      ("i3", "t3,t4", "19", "9876543210", "67890", noEndtime))
    val users = List(("u0", "3"), ("u1", "3"), ("u2", "3"), ("u3", "3"))
    val itemRecScores = List(
      ("u0", "i0,i1,i2,i3", "0.0,0.0,0.0,0.0", "[t1,t2,t3],[t2,t3],[t4],[t3,t4]", algoid, modelSet),
      ("u1", "i0,i1,i2,i3", "0.0,0.0,0.0,0.0", "[t1,t2,t3],[t2,t3],[t4],[t3,t4]", algoid, modelSet),
      ("u2", "i0,i1,i2,i3", "0.0,0.0,0.0,0.0", "[t1,t2,t3],[t2,t3],[t4],[t3,t4]", algoid, modelSet),
      ("u3", "i0,i1,i2,i3", "0.0,0.0,0.0,0.0", "[t1,t2,t3],[t2,t3],[t4],[t3,t4]", algoid, modelSet))

    test(algoid, modelSet, itypes, 500, largeNumber, items, users, itemRecScores)

  }

  // TODO: test with smaller number of numRecommendations (but can't know expected result beacause the score is random...)

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
    ("u0", "i3,i2,i1,i0", "0.0,0.0,0.0,0.0", "[t3,t4],[t4],[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u1", "i3,i2,i1,i0", "0.0,0.0,0.0,0.0", "[t3,t4],[t4],[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u2", "i3,i2,i1,i0", "0.0,0.0,0.0,0.0", "[t3,t4],[t4],[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u3", "i3,i2,i1,i0", "0.0,0.0,0.0,0.0", "[t3,t4],[t4],[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet))

  val test2ItemRecScoresEmpty = List()

  val test2ItemRecScoresi0 = List(
    ("u0", "i0", "0.0", "[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u1", "i0", "0.0", "[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u2", "i0", "0.0", "[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u3", "i0", "0.0", "[t1,t2,t3]", test2Algoid, test2ModelSet))

  val test2ItemRecScoresi0i1 = List(
    ("u0", "i1,i0", "0.0,0.0", "[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u1", "i1,i0", "0.0,0.0", "[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u2", "i1,i0", "0.0,0.0", "[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet),
    ("u3", "i1,i0", "0.0,0.0", "[t2,t3],[t1,t2,t3]", test2Algoid, test2ModelSet))

  val test2ItemRecScoresi2i3 = List(
    ("u0", "i3,i2", "0.0,0.0", "[t3,t4],[t4]", test2Algoid, test2ModelSet),
    ("u1", "i3,i2", "0.0,0.0", "[t3,t4],[t4]", test2Algoid, test2ModelSet),
    ("u2", "i3,i2", "0.0,0.0", "[t3,t4],[t4]", test2Algoid, test2ModelSet),
    ("u3", "i3,i2", "0.0,0.0", "[t3,t4],[t4]", test2Algoid, test2ModelSet))

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

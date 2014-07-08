package io.prediction.algorithms.scalding.mahout.itemrec

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.filepath.{ AlgoFile, DataFile }
import io.prediction.commons.scalding.modeldata.ItemRecScores
import cascading.tuple.{ Tuple, TupleEntry, TupleEntryIterator, Fields }

class ModelConstructorTest extends Specification with TupleConversions {

  val appid = 3

  def test(numRecommendations: Int,
    //(iindex, iid, itypes, starttime, endtime)
    items: List[(String, String, String, String, String)],
    users: List[(String, String)],
    predicted: List[(String, String)],
    output: List[(String, String, String, String)]) = {

    val engineid = 4
    val algoid = 7
    val evalid = None
    val modelSet = true

    val dbType = "file"
    val dbName = "testpath/"
    val dbHost = Seq()
    val dbPort = Seq()
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
      .arg("numRecommendations", numRecommendations.toString)
      .source(Tsv(AlgoFile(hdfsRoot, appid, engineid, algoid, evalid, "predicted.tsv"), new Fields("uindex", "predicted")), predicted)
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
    ("3", "i3", "t2", "12349", noEndtime),
    ("4", "i4", "t1", "12349", noEndtime))

  val test1Users = List(("0", "u0"), ("1", "u1"), ("2", "u2"), ("3", "u3"))

  val test1Predicted = List(("0", "[1:0.123,2:0.456,4:1.2]"), ("1", "[0:1.2]"))

  val test1Output = List(
    ("u0", "i4,i2,i1", "1.2,0.456,0.123", "[t1],[t2,t3],[t1,t2]"),
    ("u1", "i0", "1.2", "[t1,t2,t3]"))

  // only output 2 recommendations
  val test1Output2 = List(
    ("u0", "i4,i2", "1.2,0.456", "[t1],[t2,t3]"),
    ("u1", "i0", "1.2", "[t1,t2,t3]"))

  "mahout.itemrec.itembased ModelConstructor with numRecommendations=100" should {
    test(100, test1Items, test1Users, test1Predicted, test1Output)
  }

  "mahout.itemrec.itembased ModelConstructor with numRecommendations=2" should {
    test(2, test1Items, test1Users, test1Predicted, test1Output2)
  }

  /* test 2: test double comparision */
  val test2Items = List(
    ("0", "i0", "t1,t2,t3", "12346", noEndtime),
    ("1", "i1", "t1,t2", "12347", noEndtime),
    ("2", "i2", "t2,t3", "12348", noEndtime),
    ("3", "i3", "t2", "12349", noEndtime))

  val test2Users = List(("0", "u0"), ("1", "u1"), ("2", "u2"), ("3", "u3"))

  val test2Predicted = List(("0", "[0:2,1:123,2:9,3:88]"), ("1", "[0:1]"))

  val test2Output = List(
    ("u0", "i1,i3,i2,i0", "123.0,88.0,9.0,2.0", "[t1,t2],[t2],[t2,t3],[t1,t2,t3]"),
    ("u1", "i0", "1.0", "[t1,t2,t3]"))

  "mahout.itemrec.itembased ModelConstructor with numRecommendations=100 (score should not be compared as string)" should {

    test(100, test2Items, test2Users, test2Predicted, test2Output)
  }

}

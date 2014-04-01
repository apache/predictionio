package io.prediction.algorithms.scalding.mahout.itemsim

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.filepath.{ AlgoFile, DataFile }
import io.prediction.commons.scalding.modeldata.ItemSimScores
import cascading.tuple.{ Tuple, TupleEntry, TupleEntryIterator, Fields }

class ModelConstructorTest extends Specification with TupleConversions {

  val appid = 3

  def test(numSimilarItems: Int, recommendationTime: Long,
    items: List[(String, String, String, String, String)], //(iindex, iid, itypes, starttime, endtime)
    similarities: List[(String, String, String)],
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

    val itemSimScores = output map { case (iid, simiid, score, simitypes) => (iid, simiid, score, simitypes, algoid, modelSet) }

    JobTest("io.prediction.algorithms.scalding.mahout.itemsim.ModelConstructor")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("algoid", algoid.toString)
      .arg("modelSet", modelSet.toString)
      .arg("numSimilarItems", numSimilarItems.toString)
      .arg("recommendationTime", recommendationTime.toString)
      .source(Tsv(AlgoFile(hdfsRoot, appid, engineid, algoid, evalid, "similarities.tsv"), new Fields("iindex", "simiindex", "score")), similarities)
      .source(Tsv(DataFile(hdfsRoot, appid, engineid, algoid, evalid, "itemsIndex.tsv")), items)
      .sink[(String, String, String, String, Int, Boolean)](ItemSimScores(dbType = dbType, dbName = dbName, dbHost = dbHost, dbPort = dbPort, algoid = algoid, modelset = modelSet).getSource) { outputBuffer =>
        "correctly write model data to a file" in {
          outputBuffer.toList must containTheSameElementsAs(itemSimScores)
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

  val test1Similarities = List(
    ("0", "1", "0.83"),
    ("0", "2", "0.25"),
    ("0", "3", "0.49"),
    ("1", "2", "0.51"),
    ("1", "3", "0.68"),
    ("2", "3", "0.32"))

  val test1Output = List(
    ("i0", "i1,i3,i2", "0.83,0.49,0.25", "[t1,t2],[t2],[t2,t3]"),
    ("i1", "i0,i3,i2", "0.83,0.68,0.51", "[t1,t2,t3],[t2],[t2,t3]"),
    ("i2", "i1,i3,i0", "0.51,0.32,0.25", "[t1,t2],[t2],[t1,t2,t3]"),
    ("i3", "i1,i0,i2", "0.68,0.49,0.32", "[t1,t2],[t1,t2,t3],[t2,t3]"))

  val test1Output1 = List(
    ("i0", "i1", "0.83", "[t1,t2]"),
    ("i1", "i0", "0.83", "[t1,t2,t3]"),
    ("i2", "i1", "0.51", "[t1,t2]"),
    ("i3", "i1", "0.68", "[t1,t2]"))

  val test1Output2 = List(
    ("i0", "i1,i3", "0.83,0.49", "[t1,t2],[t2]"),
    ("i1", "i0,i3", "0.83,0.68", "[t1,t2,t3],[t2]"),
    ("i2", "i1,i3", "0.51,0.32", "[t1,t2],[t2]"),
    ("i3", "i1,i0", "0.68,0.49", "[t1,t2],[t1,t2,t3]"))

  "mahout.itemsim ModelConstructor" should {

    test(100, 1234567890, test1Items, test1Similarities, test1Output)

  }

  "mahout.itemsim ModelConstructor with numSimilarItems=1" should {

    test(1, 1234567890, test1Items, test1Similarities, test1Output1)

  }

  "mahout.itemsim ModelConstructor with numSimilarItems=2" should {

    test(2, 1234567890, test1Items, test1Similarities, test1Output2)

  }

  /* test 2: score sorting */

  val test2Items = List(
    ("0", "i0", "t1,t2,t3", "12346", noEndtime),
    ("1", "i1", "t1,t2", "12347", noEndtime),
    ("2", "i2", "t2,t3", "12348", noEndtime),
    ("3", "i3", "t2", "12349", noEndtime))

  val test2Similarities = List(
    ("0", "1", "83"),
    ("0", "2", "200"),
    ("0", "3", "4"),
    ("1", "2", "9"),
    ("1", "3", "68"),
    ("2", "3", "1000"))

  val test2Output = List(
    ("i0", "i2,i1,i3", "200.0,83.0,4.0", "[t2,t3],[t1,t2],[t2]"),
    ("i1", "i0,i3,i2", "83.0,68.0,9.0", "[t1,t2,t3],[t2],[t2,t3]"),
    ("i2", "i3,i0,i1", "1000.0,200.0,9.0", "[t2],[t1,t2,t3],[t1,t2]"),
    ("i3", "i2,i1,i0", "1000.0,68.0,4.0", "[t2,t3],[t1,t2],[t1,t2,t3]"))

  "mahout.itemsim ModelConstructor (score should not be compared as string)" should {

    test(100, 1234567890, test2Items, test2Similarities, test2Output)

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

  val test3Similarities = List(
    ("0", "1", "83"),
    ("0", "2", "200"),
    ("0", "3", "4"),
    ("1", "2", "9"),
    ("1", "3", "68"),
    ("2", "3", "1000"))

  val test3Output = List(
    ("i0", "i2,i1,i3", "200.0,83.0,4.0", "[t2,t3],[t1,t2],[t2]"),
    ("i1", "i0,i3,i2", "83.0,68.0,9.0", "[t1,t2,t3],[t2],[t2,t3]"),
    ("i2", "i3,i0,i1", "1000.0,200.0,9.0", "[t2],[t1,t2,t3],[t1,t2]"),
    ("i3", "i2,i1,i0", "1000.0,68.0,4.0", "[t2,t3],[t1,t2],[t1,t2,t3]"))

  val test3OutputEmpty = List()

  val test3Outputi0 = List(
    ("i1", "i0", "83.0", "[t1,t2,t3]"),
    ("i2", "i0", "200.0", "[t1,t2,t3]"),
    ("i3", "i0", "4.0", "[t1,t2,t3]"))

  val test3Outputi0i1 = List(
    ("i0", "i1", "83.0", "[t1,t2]"),
    ("i1", "i0", "83.0", "[t1,t2,t3]"),
    ("i2", "i0,i1", "200.0,9.0", "[t1,t2,t3],[t1,t2]"),
    ("i3", "i1,i0", "68.0,4.0", "[t1,t2],[t1,t2,t3]"))

  val test3Outputi2i3 = List(
    ("i0", "i2,i3", "200.0,4.0", "[t2,t3],[t2]"),
    ("i1", "i3,i2", "68.0,9.0", "[t2],[t2,t3]"),
    ("i2", "i3", "1000.0", "[t2]"),
    ("i3", "i2", "1000.0", "[t2,t3]"))

  "numSimilarItems=100 and recommendationTime < all item starttime" should {
    test(100, tA, test3Items, test3Similarities, test3OutputEmpty)
  }

  "numSimilarItems=100 and recommendationTime == earliest starttime" should {
    test(100, tB, test3Items, test3Similarities, test3Outputi0)
  }

  "numSimilarItems=100 and recommendationTime > some items starttime" should {
    test(100, tC, test3Items, test3Similarities, test3Outputi0i1)
  }

  "numSimilarItems=100 and recommendationTime > all item starttime and < all item endtime" should {
    test(100, tD, test3Items, test3Similarities, test3Output)
  }

  "numSimilarItems=100 and recommendationTime > some item endtime" should {
    test(100, tE, test3Items, test3Similarities, test3Outputi2i3)
  }

  "numSimilarItems=100 and recommendationTime == last item endtime" should {
    test(100, tF, test3Items, test3Similarities, test3OutputEmpty)
  }

  "numSimilarItems=100 and recommendationTime > last item endtime" should {
    test(100, tG, test3Items, test3Similarities, test3OutputEmpty)
  }

}

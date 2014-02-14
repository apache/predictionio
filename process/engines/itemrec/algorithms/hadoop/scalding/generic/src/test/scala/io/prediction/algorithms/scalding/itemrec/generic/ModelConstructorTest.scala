package io.prediction.algorithms.scalding.itemrec.generic

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.filepath.{ AlgoFile, DataFile }
import io.prediction.commons.scalding.modeldata.ItemRecScores

class ModelConstructorTest extends Specification with TupleConversions {

  def test(recommendationTime: Long,
    items: List[(String, String, String, String)], //iid, itypes, starttime, endtime
    itemRecScores: List[(String, String, String)],
    output: List[(String, String, String, String)]) = {

    val appid = 3
    val engineid = 4
    val algoid = 7
    val modelSet = true

    val dbType = "file"
    val dbName = "testpath/"
    val dbHost = None
    val dbPort = None
    val hdfsRoot = "testroot/"

    val outputItemRecScores = output map { case (uid, iid, score, itypes) => (uid, iid, score, itypes, algoid, modelSet) }

    JobTest("io.prediction.algorithms.scalding.itemrec.generic.ModelConstructor")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("algoid", algoid.toString)
      .arg("modelSet", modelSet.toString)
      .arg("recommendationTime", recommendationTime.toString)
      //.arg("debug", "test") // NOTE: test mode
      .source(Tsv(AlgoFile(hdfsRoot, appid, engineid, algoid, None, "itemRecScores.tsv")), itemRecScores)
      .source(Tsv(DataFile(hdfsRoot, appid, engineid, algoid, None, "selectedItems.tsv")), items)
      .sink[(String, String, String, String, Int, Boolean)](ItemRecScores(dbType = dbType, dbName = dbName, dbHost = dbHost, dbPort = dbPort, algoid = algoid, modelset = modelSet).getSource) { outputBuffer =>
        "correctly write model data to a file" in {
          outputBuffer.toList must containTheSameElementsAs(outputItemRecScores)
        }
      }
      .run
      .finish
  }

  val largeNumber = 1234567890 // larger than any item starttime
  val noEndtime = "PIO_NONE"

  /* test 1 */
  val test1ItemRecScores = List(("u0", "i1", "0.123"), ("u0", "i2", "0.456"), ("u1", "i0", "1.23"))
  val test1Items = List(
    ("i0", "t1,t2,t3", "12346", noEndtime),
    ("i1", "t1,t2", "12347", noEndtime),
    ("i2", "t2,t3", "12348", noEndtime))

  val test1Output = List(
    ("u0", "i2,i1", "0.456,0.123", "[t2,t3],[t1,t2]"),
    ("u1", "i0", "1.23", "[t1,t2,t3]"))

  "itemrec.generic ModelConstructor" should {
    test(largeNumber, test1Items, test1ItemRecScores, test1Output)
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

  val test2ItemRecScores = List(
    ("u0", "i1", "0.123"),
    ("u0", "i2", "0.456"),
    ("u0", "i3", "0.2"),
    ("u1", "i0", "12"),
    ("u1", "i2", "2"))

  val test2Items = List(
    ("i0", "t1,t2,t3", "123123", "543210"),
    ("i1", "t1,t2", "123456", "543321"),
    ("i2", "t2,t3", "123567", "543432"),
    ("i3", "t2", "123678", "543654"))

  val test2Output = List(
    ("u0", "i2,i3,i1", "0.456,0.2,0.123", "[t2,t3],[t2],[t1,t2]"),
    ("u1", "i0,i2", "12.0,2.0", "[t1,t2,t3],[t2,t3]"))

  val test2OutputEmpty = List()

  val test2Outputi0 = List(
    ("u1", "i0", "12.0", "[t1,t2,t3]"))

  val test2Outputi0i1 = List(
    ("u0", "i1", "0.123", "[t1,t2]"),
    ("u1", "i0", "12.0", "[t1,t2,t3]"))

  val test2Outputi2i3 = List(
    ("u0", "i2,i3", "0.456,0.2", "[t2,t3],[t2]"),
    ("u1", "i2", "2.0", "[t2,t3]"))

  "recommendationTime < all item starttime" should {
    test(tA, test2Items, test2ItemRecScores, test2OutputEmpty)
  }

  "recommendationTime == earliest starttime" should {
    test(tB, test2Items, test2ItemRecScores, test2Outputi0)
  }

  "recommendationTime > some items starttime" should {
    test(tC, test2Items, test2ItemRecScores, test2Outputi0i1)
  }

  "recommendationTime > all item starttime and < all item endtime" should {
    test(tD, test2Items, test2ItemRecScores, test2Output)
  }

  "recommendationTime > some item endtime" should {
    test(tE, test2Items, test2ItemRecScores, test2Outputi2i3)
  }

  "recommendationTime == last item endtime" should {
    test(tF, test2Items, test2ItemRecScores, test2OutputEmpty)
  }

  "recommendationTime > last item endtime" should {
    test(tG, test2Items, test2ItemRecScores, test2OutputEmpty)
  }

}

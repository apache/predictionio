package io.prediction.metrics.scalding.itemrec.map

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.filepath.OfflineMetricFile
import io.prediction.commons.scalding.appdata.U2iActions

class MAPAtKDataPreparatorTest extends Specification with TupleConversions {

  val Rate = "rate"
  val Like = "like"
  val Dislike = "dislike"
  val View = "view"
  //val ViewDetails = "viewDetails"
  val Conversion = "conversion"

  def test(params: Map[String, String],
    testU2i: List[(String, String, String, String, String)],
    relevantItems: List[(String, String)] // List(("u0", "i0,i1,i2"), ("u1", "i0,i1,i2"))
    ) = {

    val test_dbType = "file"
    val test_dbName = "testsetpath/"
    val test_dbHost = None
    val test_dbPort = None

    val training_dbType = "file"
    val training_dbName = "trainingsetpath/"
    val training_dbHost = None
    val training_dbPort = None

    val modeldata_dbType = "file"
    val modeldata_dbName = "modeldatapath/"
    val modeldata_dbHost = None
    val modeldata_dbPort = None

    val hdfsRoot = "testroot/"

    JobTest("io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator")
      .arg("test_dbType", test_dbType)
      .arg("test_dbName", test_dbName)
      .arg("training_dbType", training_dbType)
      .arg("training_dbName", training_dbName)
      .arg("modeldata_dbType", modeldata_dbType)
      .arg("modeldata_dbName", modeldata_dbName)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", "2")
      .arg("engineid", "4")
      .arg("evalid", "5")
      .arg("metricid", "6")
      .arg("algoid", "8")
      .arg("goalParam", params("goalParam"))
      .arg("kParam", params("kParam"))
      .source(U2iActions(appId = 5, dbType = test_dbType, dbName = test_dbName, dbHost = test_dbHost, dbPort = test_dbPort).getSource, testU2i)
      .sink[(String, String)](Tsv(OfflineMetricFile(hdfsRoot, 2, 4, 5, 6, 8, "relevantItems.tsv"))) { outputBuffer =>

        def sortItems(t: List[(String, String)]): List[(String, List[String])] = {
          t map (x => (x._1, x._2.split(",").toList.sorted))
        }

        "correctly generates relevantItems for each user" in {
          // since DataPrepator may generate relevantItems list in any order
          // and the order is not important,
          // sort the list first so we can compare it with expected result.
          val output = sortItems(outputBuffer.toList)
          val expected = sortItems(relevantItems)

          println(outputBuffer.toList)
          //println(output)
          println(expected)

          output must containTheSameElementsAs(expected)

        }
      }
      .run
      .finish
  }

  val testU2i = List(
    // u0
    (Rate, "u0", "i0", "123450", "4"),
    (View, "u0", "i1", "123457", "PIO_NONE"),
    (Dislike, "u0", "i2", "123458", "PIO_NONE"),
    (View, "u0", "i3", "123459", "PIO_NONE"),
    (View, "u0", "i7", "123460", "PIO_NONE"),
    (Rate, "u0", "i8", "123450", "5"),

    // u1
    (View, "u1", "i0", "123457", "PIO_NONE"),
    (Conversion, "u1", "i1", "123458", "PIO_NONE"),
    (Conversion, "u1", "i4", "123457", "PIO_NONE"),
    (Conversion, "u1", "i5", "123456", "PIO_NONE"),
    (Rate, "u1", "i7", "123456", "3"),
    (Rate, "u1", "i8", "123454", "3"),
    (Rate, "u1", "i9", "123453", "4"),

    // u2
    (View, "u2", "i3", "123458", "PIO_NONE"),
    (Conversion, "u2", "i4", "123451", "PIO_NONE"),
    (Conversion, "u2", "i5", "123452", "PIO_NONE"),
    (Rate, "u2", "i6", "123452", "5"))

  "itemrec.map MAPAtKDataPreparator with goal = view" should {
    val params = Map("goalParam" -> "view", "kParam" -> "4")
    val relevantItems = List(
      ("u0", "i1,i3,i7"),
      ("u1", "i0"),
      ("u2", "i3"))

    test(params, testU2i, relevantItems)
  }

  "itemrec.map MAPAtKDataPreparator with goal = conversion" should {
    val params = Map("goalParam" -> "conversion", "kParam" -> "8")
    val relevantItems = List(
      ("u1", "i1,i4,i5"),
      ("u2", "i4,i5"))

    test(params, testU2i, relevantItems)
  }

  "itemrec.map MAPAtKDataPreparator with goal = rate >= 3" should {
    val params = Map("goalParam" -> "rate3", "kParam" -> "8")
    val relevantItems = List(
      ("u0", "i0,i8"),
      ("u1", "i7,i8,i9"),
      ("u2", "i6"))

    test(params, testU2i, relevantItems)
  }

  "itemrec.map MAPAtKDataPreparator with goal = rate >= 4" should {
    val params = Map("goalParam" -> "rate4", "kParam" -> "8")
    val relevantItems = List(
      ("u0", "i0,i8"),
      ("u1", "i9"),
      ("u2", "i6"))

    test(params, testU2i, relevantItems)
  }

  "itemrec.map MAPAtKDataPreparator with goal = rate >= 5" should {
    val params = Map("goalParam" -> "rate5", "kParam" -> "8")
    val relevantItems = List(
      ("u0", "i8"),
      ("u2", "i6"))

    test(params, testU2i, relevantItems)
  }

}
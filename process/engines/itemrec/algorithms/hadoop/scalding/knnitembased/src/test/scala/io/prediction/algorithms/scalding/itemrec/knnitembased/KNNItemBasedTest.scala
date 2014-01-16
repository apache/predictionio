package io.prediction.algorithms.scalding.itemrec.knnitembased

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.filepath.{ DataFile, AlgoFile }

class KNNItemBasedTest extends Specification with TupleConversions {

  // helper function
  // only compare double up to 9 decimal places
  def roundingData(orgList: List[(String, String, Double)]) = {
    orgList map { x =>
      val (t1, t2, t3) = x

      // NOTE: use HALF_UP mode to avoid error caused by rounding when compare data
      // (eg. 3.5 vs 3.499999999999).
      // (eg. 0.6666666666 vs 0.666666667)

      (t1, t2, BigDecimal(t3).setScale(9, BigDecimal.RoundingMode.HALF_UP).toDouble)
    }
  }

  def testWithMerge(testArgs: Map[String, String],
    testInput: List[(String, String, Int)],
    testOutput: List[(String, String, Double)]) = {

    val appid = 1
    val engineid = 2
    val algoid = 3
    val hdfsRoot = "testroot/"

    JobTest("io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased")
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("algoid", algoid.toString)
      .arg("hdfsRoot", hdfsRoot)
      .arg("measureParam", testArgs("measureParam"))
      .arg("priorCountParam", testArgs("priorCountParam"))
      .arg("priorCorrelParam", testArgs("priorCorrelParam"))
      .arg("minNumRatersParam", testArgs("minNumRatersParam"))
      .arg("maxNumRatersParam", testArgs("maxNumRatersParam"))
      .arg("minIntersectionParam", testArgs("minIntersectionParam"))
      .arg("minNumRatedSimParam", testArgs("minNumRatedSimParam"))
      .arg("mergeRatingParam", "")
      .arg("unseenOnly", testArgs("unseenOnly"))
      .arg("numRecommendations", testArgs("numRecommendations"))
      .source(Tsv(DataFile(hdfsRoot, appid, engineid, algoid, None, "ratings.tsv")), testInput)
      .sink[(String, String, Double)](Tsv(AlgoFile(hdfsRoot, appid, engineid, algoid, None, "itemRecScores.tsv"))) { outputBuffer =>
        "correctly calculate itemRecScores" in {
          roundingData(outputBuffer.toList) must containTheSameElementsAs(roundingData(testOutput))
        }
      }
      .run
      .finish
  }

  def testWithoutMerge(testArgs: Map[String, String],
    testInput: List[(String, String, Int)],
    testOutput: List[(String, String, Double)]) = {

    val appid = 1
    val engineid = 2
    val algoid = 3
    val hdfsRoot = "testroot/"

    JobTest("io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased")
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("algoid", algoid.toString)
      .arg("hdfsRoot", hdfsRoot)
      .arg("measureParam", testArgs("measureParam"))
      .arg("priorCountParam", testArgs("priorCountParam"))
      .arg("priorCorrelParam", testArgs("priorCorrelParam"))
      .arg("minNumRatersParam", testArgs("minNumRatersParam"))
      .arg("maxNumRatersParam", testArgs("maxNumRatersParam"))
      .arg("minIntersectionParam", testArgs("minIntersectionParam"))
      .arg("minNumRatedSimParam", testArgs("minNumRatedSimParam"))
      .arg("unseenOnly", testArgs("unseenOnly"))
      .arg("numRecommendations", testArgs("numRecommendations"))
      .source(Tsv(DataFile(hdfsRoot, appid, engineid, algoid, None, "ratings.tsv")), testInput)
      .sink[(String, String, Double)](Tsv(AlgoFile(hdfsRoot, appid, engineid, algoid, None, "itemRecScores.tsv"))) { outputBuffer =>
        "correctly calculate itemRecScores" in {
          roundingData(outputBuffer.toList) must containTheSameElementsAs(roundingData(testOutput))
        }
      }
      .run
      .finish
  }

  // test1
  val test1args = Map[String, String]("measureParam" -> "correl",
    "priorCountParam" -> "10",
    "priorCorrelParam" -> "0",
    "minNumRatersParam" -> "1",
    "maxNumRatersParam" -> "100000",
    "minIntersectionParam" -> "1",
    "minNumRatedSimParam" -> "1",
    "unseenOnly" -> "false",
    "numRecommendations" -> "500"
  )

  val test1Input = List(
    ("u0", "i0", 1),
    ("u0", "i1", 2),
    ("u0", "i2", 3),
    ("u1", "i1", 4),
    ("u1", "i2", 4),
    ("u1", "i3", 2),
    ("u2", "i0", 3),
    ("u2", "i1", 2),
    ("u2", "i3", 1),
    ("u3", "i0", 2),
    ("u3", "i2", 1),
    ("u3", "i3", 5))

  val test1ItemSimScore = List(
    ("i0", "i1", 0.0),
    ("i1", "i0", 0.0),
    ("i0", "i2", -0.16666666666666666),
    ("i2", "i0", -0.16666666666666666),
    ("i0", "i3", -0.16666666666666666),
    ("i3", "i0", -0.16666666666666666),
    ("i1", "i2", 0.16666666666666666),
    ("i2", "i1", 0.16666666666666666),
    ("i1", "i3", 0.16666666666666666),
    ("i3", "i1", 0.16666666666666666),
    ("i2", "i3", -0.16666666666666666),
    ("i3", "i2", -0.16666666666666666))

  val test1Output = List[(String, String, Double)](
    ("u0", "i0", 1),
    ("u0", "i1", 2),
    ("u0", "i2", 3),
    ("u0", "i3", -0.666666666666667),
    ("u1", "i0", -3.0),
    ("u1", "i1", 4),
    ("u1", "i2", 4),
    ("u1", "i3", 2),
    ("u2", "i0", 3),
    ("u2", "i1", 2),
    ("u2", "i2", -0.666666666666667),
    ("u2", "i3", 1),
    ("u3", "i0", 2),
    ("u3", "i1", 3.0),
    ("u3", "i2", 1),
    ("u3", "i3", 5))

  "KNNItemBasedTest minNumRatedSimParam=1 and mergeRatingParam defined" should {
    testWithMerge(test1args, test1Input, test1Output)
  }

  // test2
  val test2args = Map[String, String]("measureParam" -> "correl",
    "priorCountParam" -> "10",
    "priorCorrelParam" -> "0",
    "minNumRatersParam" -> "1",
    "maxNumRatersParam" -> "100000",
    "minIntersectionParam" -> "1",
    "minNumRatedSimParam" -> "3",
    "unseenOnly" -> "false",
    "numRecommendations" -> "500"
  )

  val test2Input = List(
    ("u0", "i0", 1),
    ("u0", "i1", 2),
    ("u0", "i2", 3),
    ("u1", "i1", 4),
    ("u1", "i2", 4),
    ("u1", "i3", 2),
    ("u2", "i0", 3),
    ("u2", "i1", 2),
    ("u2", "i3", 1),
    ("u3", "i0", 2),
    ("u3", "i2", 1),
    ("u3", "i3", 5))

  val test2ItemSimScore = List(
    ("i0", "i1", 0.0),
    ("i1", "i0", 0.0),
    ("i0", "i2", -0.16666666666666666),
    ("i2", "i0", -0.16666666666666666),
    ("i0", "i3", -0.16666666666666666),
    ("i3", "i0", -0.16666666666666666),
    ("i1", "i2", 0.16666666666666666),
    ("i2", "i1", 0.16666666666666666),
    ("i1", "i3", 0.16666666666666666),
    ("i3", "i1", 0.16666666666666666),
    ("i2", "i3", -0.16666666666666666),
    ("i3", "i2", -0.16666666666666666))

  val test2Output = List[(String, String, Double)](
    ("u0", "i0", 1),
    ("u0", "i1", 2),
    ("u0", "i2", 3),
    ("u0", "i3", -0.666666666666667),
    // u1, i0, won't be predicted because num of similar items is < minNumRatedSimParam
    ("u1", "i1", 4),
    ("u1", "i2", 4),
    ("u1", "i3", 2),
    ("u2", "i0", 3),
    ("u2", "i1", 2),
    ("u2", "i2", -0.666666666666667),
    ("u2", "i3", 1),
    ("u3", "i0", 2),
    // u3, i1, won't be predicted because num of similar items is < minNumRatedSimParam
    ("u3", "i2", 1),
    ("u3", "i3", 5))

  "KNNItemBasedTest minNumRatedSimParam=3 and mergeRatingParam defined" should {
    testWithMerge(test2args, test2Input, test2Output)
  }

  // test3
  val test3args = Map[String, String]("measureParam" -> "correl",
    "priorCountParam" -> "10",
    "priorCorrelParam" -> "0",
    "minNumRatersParam" -> "1",
    "maxNumRatersParam" -> "100000",
    "minIntersectionParam" -> "1",
    "minNumRatedSimParam" -> "1",
    "unseenOnly" -> "false",
    "numRecommendations" -> "500"
  )

  val test3Input = List(
    ("u0", "i0", 1),
    ("u0", "i1", 2),
    ("u0", "i2", 3),
    ("u1", "i1", 4),
    ("u1", "i2", 4),
    ("u1", "i3", 2),
    ("u2", "i0", 3),
    ("u2", "i1", 2),
    ("u2", "i3", 1),
    ("u3", "i0", 2),
    ("u3", "i2", 1),
    ("u3", "i3", 5))

  val test3ItemSimScore = List(
    ("i0", "i1", 0.0),
    ("i1", "i0", 0.0),
    ("i0", "i2", -0.16666666666666666),
    ("i2", "i0", -0.16666666666666666),
    ("i0", "i3", -0.16666666666666666),
    ("i3", "i0", -0.16666666666666666),
    ("i1", "i2", 0.16666666666666666),
    ("i2", "i1", 0.16666666666666666),
    ("i1", "i3", 0.16666666666666666),
    ("i3", "i1", 0.16666666666666666),
    ("i2", "i3", -0.16666666666666666),
    ("i3", "i2", -0.16666666666666666))

  val test3Output = List[(String, String, Double)](
    ("u0", "i0", -3.0),
    ("u0", "i1", 3.0),
    ("u0", "i2", 0.5),
    ("u0", "i3", -0.666666666666667),
    ("u1", "i0", -3.0),
    ("u1", "i1", 3.0),
    ("u1", "i2", 1.0),
    ("u1", "i3", 0.0),
    ("u2", "i0", -1.0),
    ("u2", "i1", 1.0),
    ("u2", "i2", -0.666666666666667),
    ("u2", "i3", -0.5),
    ("u3", "i0", -3.0),
    ("u3", "i1", 3.0),
    ("u3", "i2", -3.5),
    ("u3", "i3", -1.5))

  "KNNItemBasedTest with large numRecommendations and mergeRatingParam undefined" should {
    testWithoutMerge(test3args, test3Input, test3Output)
  }

  val test3OutputTop3 = List[(String, String, Double)](
    ("u0", "i1", 3.0),
    ("u0", "i2", 0.5),
    ("u0", "i3", -0.666666666666667),
    ("u1", "i1", 3.0),
    ("u1", "i2", 1.0),
    ("u1", "i3", 0.0),
    ("u2", "i1", 1.0),
    ("u2", "i2", -0.666666666666667),
    ("u2", "i3", -0.5),
    ("u3", "i0", -3.0),
    ("u3", "i1", 3.0),
    ("u3", "i3", -1.5))

  "KNNItemBasedTest with small numRecommendations and mergeRatingParam undefined" should {
    testWithoutMerge(test3args ++ Map("numRecommendations" -> "3"), test3Input, test3OutputTop3)
  }

  val test3OutputUnseenOnly = List[(String, String, Double)](
    ("u0", "i3", -0.666666666666667),
    ("u1", "i0", -3.0),
    ("u2", "i2", -0.666666666666667),
    ("u3", "i1", 3.0))

  "KNNItemBasedTest with unseenOnly=true and mergeRatingParam undefined" should {
    testWithoutMerge(test3args ++ Map("unseenOnly" -> "true"), test3Input, test3OutputUnseenOnly)
  }

}
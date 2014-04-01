package io.prediction.algorithms.scalding.itemsim.itemsimcf

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.filepath.{ DataFile, AlgoFile }

class ItemSimilarityTest extends Specification with TupleConversions {

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

  def test(testArgs: Map[String, String],
    testInput: List[(String, String, Int)],
    testOutput: List[(String, String, Double)]) = {

    val hdfsRoot = "testroot/"

    JobTest("io.prediction.algorithms.scalding.itemsim.itemsimcf.ItemSimilarity").
      arg("hdfsRoot", hdfsRoot).
      arg("appid", "8").
      arg("engineid", "2").
      arg("algoid", "3").
      arg("measureParam", testArgs("measureParam")).
      arg("priorCountParam", testArgs("priorCountParam")).
      arg("priorCorrelParam", testArgs("priorCorrelParam")).
      arg("minNumRatersParam", testArgs("minNumRatersParam")).
      arg("maxNumRatersParam", testArgs("maxNumRatersParam")).
      arg("minIntersectionParam", testArgs("minIntersectionParam")).
      arg("numSimilarItems", testArgs("numSimilarItems")).
      source(Tsv(DataFile(hdfsRoot, 8, 2, 3, None, "ratings.tsv")), testInput).
      sink[(String, String, Double)](Tsv(AlgoFile(hdfsRoot, 8, 2, 3, None, "itemSimScores.tsv"))) { outputBuffer =>
        "correctly calculate similarity score" in {
          roundingData(outputBuffer.toList) must containTheSameElementsAs(roundingData(testOutput))
        }
      }
      .run
      .finish
  }

  // simple test1
  val test1args = Map[String, String]("measureParam" -> "correl",
    "priorCountParam" -> "10",
    "priorCorrelParam" -> "0",
    "minNumRatersParam" -> "1",
    "maxNumRatersParam" -> "10000",
    "minIntersectionParam" -> "1",
    "numSimilarItems" -> "500"
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

  val test1Output = List(
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

  val hdfsRoot = "testroot/"

  "ItemSimilarity Correlation" should {
    test(test1args, test1Input, test1Output)
  }

  // simple test2
  val test2args = Map[String, String]("measureParam" -> "correl",
    "priorCountParam" -> "20",
    "priorCorrelParam" -> "0.5",
    "minNumRatersParam" -> "1",
    "maxNumRatersParam" -> "10000",
    "minIntersectionParam" -> "1",
    "numSimilarItems" -> "500"
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

  val test2Output = List(
    ("i0", "i1", 0.454545454545454),
    ("i1", "i0", 0.454545454545454),
    ("i0", "i2", 0.363636363636364),
    ("i2", "i0", 0.363636363636364),
    ("i0", "i3", 0.363636363636364),
    ("i3", "i0", 0.363636363636364),
    ("i1", "i2", 0.545454545454545),
    ("i2", "i1", 0.545454545454545),
    ("i1", "i3", 0.545454545454545),
    ("i3", "i1", 0.545454545454545),
    ("i2", "i3", 0.363636363636364),
    ("i3", "i2", 0.363636363636364))

  "ItemSimilarity Correlation with different regularization" should {
    test(test2args, test2Input, test2Output)
  }

  // simple test3
  val test3args = Map[String, String]("measureParam" -> "cosine",
    "priorCountParam" -> "0",
    "priorCorrelParam" -> "0",
    "minNumRatersParam" -> "1",
    "maxNumRatersParam" -> "10000",
    "minIntersectionParam" -> "1",
    "numSimilarItems" -> "500"
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

  val test3Output = List[(String, String, Double)](
    ("i0", "i1", 0.894427190999916),
    ("i1", "i0", 0.894427190999916),
    ("i0", "i2", 0.707106781186548),
    ("i2", "i0", 0.707106781186548),
    ("i0", "i3", 0.707106781186548),
    ("i3", "i0", 0.707106781186548),
    ("i1", "i2", 0.983869910099907),
    ("i2", "i1", 0.983869910099907),
    ("i1", "i3", 1.0), // NOTE: (use HALF_UP to work around 1.0 vs 0.999999999)
    ("i3", "i1", 1.0),
    ("i2", "i3", 0.585490553844358),
    ("i3", "i2", 0.585490553844358))

  "ItemSimilarity Cosine" should {
    test(test3args, test3Input, test3Output)
  }

  // test4 - test numSimilarItems smaller than existing
  val test4args = Map[String, String]("measureParam" -> "cosine",
    "priorCountParam" -> "0",
    "priorCorrelParam" -> "0",
    "minNumRatersParam" -> "1",
    "maxNumRatersParam" -> "10000",
    "minIntersectionParam" -> "1",
    "numSimilarItems" -> "1"
  )

  val test4Input = List(
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

  val test4Output = List[(String, String, Double)](
    ("i0", "i1", 0.894427190999916),
    ("i2", "i1", 0.983869910099907),
    ("i1", "i3", 1.0), // NOTE: (use HALF_UP to work around 1.0 vs 0.999999999)
    ("i3", "i1", 1.0))

  "ItemSimilarity Cosine with smaller numSimilarItems" should {
    test(test4args, test4Input, test4Output)
  }

}
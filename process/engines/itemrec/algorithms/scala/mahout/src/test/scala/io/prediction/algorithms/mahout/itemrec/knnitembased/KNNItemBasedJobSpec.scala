package io.prediction.algorithms.mahout.itemrec.knnitembased

import org.specs2.mutable._
import com.github.nscala_time.time.Imports._
import scala.io.Source
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

import io.prediction.algorithms.mahout.itemrec.MahoutJob
import io.prediction.algorithms.mahout.itemrec.TestUtils

class KNNItemBasedJobSpec extends Specification {

  val ratingsCSV = List(
    "1,1,3",
    "4,1,5",
    "1,2,3",
    "3,2,2",
    "4,2,4",
    "1,3,4",
    "2,3,4",
    "3,3,2",
    "2,4,2",
    "3,4,3",
    "4,4,2"
  )

  val appid = 25
  val engineid = 31
  val algoid = 32

  val jobName =
    "io.prediction.algorithms.mahout.itemrec.knnitembased.KNNItemBasedJob"

  "KNNItemBasedJob with unseenOnly=false" should {
    val testDir = "/tmp/pio_test/KNNItemBasedJob/unseenOnlyfalse/"
    val inputFile = s"${testDir}ratings.csv"
    val outputFile = s"${testDir}predicted.tsv"
    val outputSim = s"${testDir}sim.csv"

    val testDirFile = new File(testDir)
    testDirFile.mkdirs()

    val jobArgs = Map(
      "input" -> inputFile,
      "output" -> outputFile,
      "appid" -> appid,
      "engineid" -> engineid,
      "algoid" -> algoid,
      "booleanData" -> false,
      "numRecommendations" -> 5,
      "itemSimilarity" -> "LogLikelihoodSimilarity",
      "weighted" -> false,
      "nearestN" -> 10,
      "threshold" -> 4.9E-324,
      "outputSim" -> outputSim,
      "preComputeItemSim" -> false,
      "unseenOnly" -> false,
      "recommendationTime" -> DateTime.now.millis
    )

    TestUtils.writeToFile(ratingsCSV, inputFile)

    val predictedExpected = List(
      "1\t[3:3.4408236,1:3.2995765,4:3.2805154,2:3.2180138]",
      "2\t[3:3.338186,1:3.0,2:3.0,4:2.661814]",
      "3\t[4:2.5027347,1:2.3333333,2:2.2486327,3:2.2486327]",
      "4\t[2:3.905135,3:3.8779385,1:3.8016937,4:3.4595158]"
    )

    MahoutJob.main(Array(jobName) ++ TestUtils.argMapToArray(jobArgs))

    "generate prediction output correctly" in {
      val predicted = Source.fromFile(outputFile)
        .getLines().toList

      predicted must containTheSameElementsAs(predictedExpected)
    }

  }

  "KNNItemBasedJob with unseenOnly=true" should {
    val testDir = "/tmp/pio_test/KNNItemBasedJob/unseenOnlytrue/"
    val inputFile = s"${testDir}ratings.csv"
    val outputFile = s"${testDir}predicted.tsv"
    val outputSim = s"${testDir}sim.csv"

    val testDirFile = new File(testDir)
    testDirFile.mkdirs()

    val jobArgs = Map(
      "input" -> inputFile,
      "output" -> outputFile,
      "appid" -> appid,
      "engineid" -> engineid,
      "algoid" -> algoid,
      "booleanData" -> false,
      "numRecommendations" -> 5,
      "itemSimilarity" -> "LogLikelihoodSimilarity",
      "weighted" -> false,
      "nearestN" -> 10,
      "threshold" -> 4.9E-324,
      "outputSim" -> outputSim,
      "preComputeItemSim" -> false,
      "unseenOnly" -> true,
      "recommendationTime" -> DateTime.now.millis
    )

    TestUtils.writeToFile(ratingsCSV, inputFile)

    val predictedExpected = List(
      "1\t[4:3.2805154]",
      "2\t[1:3.0,2:3.0]",
      "3\t[1:2.3333333]",
      "4\t[3:3.8779385]"
    )

    MahoutJob.main(Array(jobName) ++ TestUtils.argMapToArray(jobArgs))

    "generate prediction output correctly" in {
      val predicted = Source.fromFile(outputFile)
        .getLines().toList

      predicted must containTheSameElementsAs(predictedExpected)
    }
  }

  "KNNItemBasedJob with unseenOnly=true and seenFile" should {
    val testDir = "/tmp/pio_test/KNNItemBasedJob/unseenOnlytrueSeenFile/"
    val inputFile = s"${testDir}ratings.csv"
    val outputFile = s"${testDir}predicted.tsv"
    val outputSim = s"${testDir}sim.csv"
    val seenFile = s"${testDir}seen.csv"

    val testDirFile = new File(testDir)
    testDirFile.mkdirs()

    val seenCSV = List(
      "1,1",
      "4,1",
      "1,2"
    )

    val jobArgs = Map(
      "input" -> inputFile,
      "output" -> outputFile,
      "appid" -> appid,
      "engineid" -> engineid,
      "algoid" -> algoid,
      "booleanData" -> false,
      "numRecommendations" -> 5,
      "itemSimilarity" -> "LogLikelihoodSimilarity",
      "weighted" -> false,
      "nearestN" -> 10,
      "threshold" -> 4.9E-324,
      "outputSim" -> outputSim,
      "preComputeItemSim" -> false,
      "unseenOnly" -> true,
      "seenFile" -> seenFile,
      "recommendationTime" -> DateTime.now.millis
    )

    TestUtils.writeToFile(ratingsCSV, inputFile)
    TestUtils.writeToFile(seenCSV, seenFile)

    val predictedExpected = List(
      "1\t[3:3.4408236,4:3.2805154]",
      "2\t[3:3.338186,1:3.0,2:3.0,4:2.661814]",
      "3\t[4:2.5027347,1:2.3333333,2:2.2486327,3:2.2486327]",
      "4\t[2:3.905135,3:3.8779385,4:3.4595158]"
    )

    MahoutJob.main(Array(jobName) ++ TestUtils.argMapToArray(jobArgs))

    "generate prediction output correctly" in {
      val predicted = Source.fromFile(outputFile)
        .getLines().toList

      predicted must containTheSameElementsAs(predictedExpected)
    }
  }

  "KNNItemBasedJob with unseenOnly=true and empty seenFile" should {
    val testDir = "/tmp/pio_test/KNNItemBasedJob/unseenOnlytrueEmptySeenFile/"
    val inputFile = s"${testDir}ratings.csv"
    val outputFile = s"${testDir}predicted.tsv"
    val outputSim = s"${testDir}sim.csv"
    val seenFile = s"${testDir}seen.csv"

    val testDirFile = new File(testDir)
    testDirFile.mkdirs()

    val seenCSV = List()

    val jobArgs = Map(
      "input" -> inputFile,
      "output" -> outputFile,
      "appid" -> appid,
      "engineid" -> engineid,
      "algoid" -> algoid,
      "booleanData" -> false,
      "numRecommendations" -> 5,
      "itemSimilarity" -> "LogLikelihoodSimilarity",
      "weighted" -> false,
      "nearestN" -> 10,
      "threshold" -> 4.9E-324,
      "outputSim" -> outputSim,
      "preComputeItemSim" -> false,
      "unseenOnly" -> true,
      "seenFile" -> seenFile,
      "recommendationTime" -> DateTime.now.millis
    )

    TestUtils.writeToFile(ratingsCSV, inputFile)
    TestUtils.writeToFile(seenCSV, seenFile)

    val predictedExpected = List(
      "1\t[3:3.4408236,1:3.2995765,4:3.2805154,2:3.2180138]",
      "2\t[3:3.338186,1:3.0,2:3.0,4:2.661814]",
      "3\t[4:2.5027347,1:2.3333333,2:2.2486327,3:2.2486327]",
      "4\t[2:3.905135,3:3.8779385,1:3.8016937,4:3.4595158]"
    )

    MahoutJob.main(Array(jobName) ++ TestUtils.argMapToArray(jobArgs))

    "generate prediction output correctly" in {
      val predicted = Source.fromFile(outputFile)
        .getLines().toList

      predicted must containTheSameElementsAs(predictedExpected)
    }
  }

  // TODO: add more tests...

}

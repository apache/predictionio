package io.prediction.algorithms.mahout.itemrec.knnuserbased

import org.specs2.mutable._
import com.github.nscala_time.time.Imports._
import scala.io.Source
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

import io.prediction.algorithms.mahout.itemrec.MahoutJob
import io.prediction.algorithms.mahout.itemrec.TestUtils

class KNNUserBasedJobSpec extends Specification {

  val ratingsCSV = List(
    "1,1,3",
    "4,1,5",
    "1,2,3",
    "3,2,2",
    "4,2,4",
    "1,3,5",
    "2,3,1",
    "3,3,2",
    "2,4,2",
    "3,4,3",
    "4,4,2"
  )

  val appid = 25
  val engineid = 31
  val algoid = 32

  val jobName =
    "io.prediction.algorithms.mahout.itemrec.knnuserbased.KNNUserBasedJob"

  "KNNUserBasedJob with unseenOnly=false" should {
    val testDir = "/tmp/pio_test/KNNUserBasedJob/unseenOnlyfalse/"
    val inputFile = s"${testDir}ratings.csv"
    val outputFile = s"${testDir}predicted.tsv"

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
      "nearestN" -> 10,
      "userSimilarity" -> "LogLikelihoodSimilarity",
      "weighted" -> false,
      "minSimilarity" -> 5e-324,
      "samplingRate" -> 1,
      "unseenOnly" -> false,
      "recommendationTime" -> 0
    )

    TestUtils.writeToFile(ratingsCSV, inputFile)

    val predictedExpected = List(
      "1\t[2:3.0,4:2.2805154,3:1.3898838]",
      "2\t[1:4.0,3:3.5,2:3.0,4:2.5]",
      "3\t[1:4.0,2:3.5,3:2.559535,4:2.0]",
      "4\t[2:2.5,3:2.402577,4:2.3898838]"
    )

    MahoutJob.main(Array(jobName) ++ TestUtils.argMapToArray(jobArgs))

    "generate prediction output correctly" in {
      val predicted = Source.fromFile(outputFile)
        .getLines().toList

      predicted must containTheSameElementsAs(predictedExpected)
    }

  }

  "KNNUserBasedJob with unseenOnly=true" should {
    val testDir = "/tmp/pio_test/KNNUserBasedJob/unseenOnlytrue/"
    val inputFile = s"${testDir}ratings.csv"
    val outputFile = s"${testDir}predicted.tsv"

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
      "nearestN" -> 10,
      "userSimilarity" -> "LogLikelihoodSimilarity",
      "weighted" -> false,
      "minSimilarity" -> 5e-324,
      "samplingRate" -> 1,
      "unseenOnly" -> true,
      "recommendationTime" -> 0
    )

    TestUtils.writeToFile(ratingsCSV, inputFile)

    val predictedExpected = List(
      "1\t[4:2.2805154]",
      "2\t[1:4.0,2:3.0]",
      "3\t[1:4.0]",
      "4\t[3:2.402577]"
    )

    MahoutJob.main(Array(jobName) ++ TestUtils.argMapToArray(jobArgs))

    "generate prediction output correctly" in {
      val predicted = Source.fromFile(outputFile)
        .getLines().toList

      predicted must containTheSameElementsAs(predictedExpected)
    }
  }

  "KNNUserBasedJob with unseenOnly=true and seenFile" should {
    val testDir = "/tmp/pio_test/KNNUserBasedJob/unseenOnlytrueSeenFile/"
    val inputFile = s"${testDir}ratings.csv"
    val outputFile = s"${testDir}predicted.tsv"
    val seenFile = s"${testDir}seen.csv"

    val testDirFile = new File(testDir)
    testDirFile.mkdirs()

    val seenCSV = List(
      "1,1",
      "4,1",
      "1,2",
      "2,1",
      "3,1"
    )

    val jobArgs = Map(
      "input" -> inputFile,
      "output" -> outputFile,
      "appid" -> appid,
      "engineid" -> engineid,
      "algoid" -> algoid,
      "booleanData" -> false,
      "numRecommendations" -> 5,
      "nearestN" -> 10,
      "userSimilarity" -> "LogLikelihoodSimilarity",
      "weighted" -> false,
      "minSimilarity" -> 5e-324,
      "samplingRate" -> 1,
      "unseenOnly" -> true,
      "seenFile" -> seenFile,
      "recommendationTime" -> 0
    )

    TestUtils.writeToFile(ratingsCSV, inputFile)
    TestUtils.writeToFile(seenCSV, seenFile)

    val predictedExpected = List(
      "1\t[4:2.2805154,3:1.3898838]",
      "2\t[3:3.5,2:3.0,4:2.5]",
      "3\t[2:3.5,3:2.559535,4:2.0]",
      "4\t[2:2.5,3:2.402577,4:2.3898838]"
    )

    MahoutJob.main(Array(jobName) ++ TestUtils.argMapToArray(jobArgs))

    "generate prediction output correctly" in {
      val predicted = Source.fromFile(outputFile)
        .getLines().toList

      predicted must containTheSameElementsAs(predictedExpected)
    }
  }

  "KNNUserBasedJob with unseenOnly=true and empty seenFile" should {
    val testDir = "/tmp/pio_test/KNNUserBasedJob/unseenOnlytrueEmptySeenFile/"
    val inputFile = s"${testDir}ratings.csv"
    val outputFile = s"${testDir}predicted.tsv"
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
      "nearestN" -> 10,
      "userSimilarity" -> "LogLikelihoodSimilarity",
      "weighted" -> false,
      "minSimilarity" -> 5e-324,
      "samplingRate" -> 1,
      "unseenOnly" -> true,
      "seenFile" -> seenFile,
      "recommendationTime" -> 0
    )

    TestUtils.writeToFile(ratingsCSV, inputFile)
    TestUtils.writeToFile(seenCSV, seenFile)

    val predictedExpected = List(
      "1\t[2:3.0,4:2.2805154,3:1.3898838]",
      "2\t[1:4.0,3:3.5,2:3.0,4:2.5]",
      "3\t[1:4.0,2:3.5,3:2.559535,4:2.0]",
      "4\t[2:2.5,3:2.402577,4:2.3898838]"
    )

    MahoutJob.main(Array(jobName) ++ TestUtils.argMapToArray(jobArgs))

    "generate prediction output correctly" in {
      val predicted = Source.fromFile(outputFile)
        .getLines().toList

      predicted must containTheSameElementsAs(predictedExpected)
    }
  }

}

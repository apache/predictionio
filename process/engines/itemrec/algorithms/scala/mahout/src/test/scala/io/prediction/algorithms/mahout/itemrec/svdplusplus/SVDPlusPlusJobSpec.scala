package io.prediction.algorithms.mahout.itemrec.svdplusplus

import org.specs2.mutable._
import com.github.nscala_time.time.Imports._
import scala.io.Source
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

import io.prediction.algorithms.mahout.itemrec.MahoutJob
import io.prediction.algorithms.mahout.itemrec.TestUtils

class SVDPlusPlusJobSpec extends Specification {
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
    "io.prediction.algorithms.mahout.itemrec.svdplusplus.SVDPlusPlusJob"

  def convertToIDSet(rec: String): (String, Set[String]) = {
    val field = rec.split("\t")
    val uid = field(0)
    val data = field(1)
    val dataLen = data.length
    val iids = data.take(dataLen - 1).tail.split(",").toList
      .map { ratingData =>
        val ratingDataArray = ratingData.split(":")
        ratingDataArray(0)
      }.toSet
    (uid, iids)
  }

  "SVDPlusPlusJob with unseenOnly=false" should {
    val testDir = "/tmp/pio_test/SVDPlusPlusJob/unseenOnlyfalse/"
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
      "numRecommendations" -> 5,
      "numFeatures" -> 3,
      "learningRate" -> 0.01,
      "preventOverfitting" -> 0.1,
      "randomNoise" -> 0.01,
      "numIterations" -> 3,
      "learningRateDecay" -> 1,
      "unseenOnly" -> false,
      "recommendationTime" -> DateTime.now.millis
    )

    TestUtils.writeToFile(ratingsCSV, inputFile)

    MahoutJob.main(Array(jobName) ++ TestUtils.argMapToArray(jobArgs))

    // NOTE: don't check predicted score
    val predictedExp = List(
      ("1", Set("1", "2", "3", "4")),
      ("2", Set("1", "2", "3", "4")),
      ("3", Set("1", "2", "3", "4")),
      ("4", Set("1", "2", "3", "4"))
    )

    "generate prediction output correctly" in {
      val predicted = Source.fromFile(outputFile).getLines().toList
      predicted.map(convertToIDSet(_)) must
        containTheSameElementsAs(predictedExp)
    }
  }

  "SVDPlusPlusJob with unseenOnly=true" should {
    val testDir = "/tmp/pio_test/SVDPlusPlusJob/unseenOnlytrue/"
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
      "numRecommendations" -> 5,
      "numFeatures" -> 3,
      "learningRate" -> 0.01,
      "preventOverfitting" -> 0.1,
      "randomNoise" -> 0.01,
      "numIterations" -> 3,
      "learningRateDecay" -> 1,
      "unseenOnly" -> true,
      "recommendationTime" -> DateTime.now.millis
    )

    TestUtils.writeToFile(ratingsCSV, inputFile)

    MahoutJob.main(Array(jobName) ++ TestUtils.argMapToArray(jobArgs))

    // NOTE: don't check predicted score
    val predictedExp = List(
      ("1", Set("4")),
      ("2", Set("1", "2")),
      ("3", Set("1")),
      ("4", Set("3"))
    )

    "generate prediction output correctly" in {
      val predicted = Source.fromFile(outputFile).getLines().toList
      predicted.map(convertToIDSet(_)) must
        containTheSameElementsAs(predictedExp)
    }

  }

  "SVDPlusPlusJob with unseenOnly=true and seenFile" should {
    val testDir = "/tmp/pio_test/SVDPlusPlusJob/unseenOnlytrueSeenFile/"
    val inputFile = s"${testDir}ratings.csv"
    val outputFile = s"${testDir}predicted.tsv"
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
      "numRecommendations" -> 5,
      "numFeatures" -> 3,
      "learningRate" -> 0.01,
      "preventOverfitting" -> 0.1,
      "randomNoise" -> 0.01,
      "numIterations" -> 3,
      "learningRateDecay" -> 1,
      "unseenOnly" -> true,
      "seenFile" -> seenFile,
      "recommendationTime" -> DateTime.now.millis
    )

    TestUtils.writeToFile(ratingsCSV, inputFile)
    TestUtils.writeToFile(seenCSV, seenFile)

    MahoutJob.main(Array(jobName) ++ TestUtils.argMapToArray(jobArgs))

    // NOTE: don't check predicted score
    val predictedExp = List(
      ("1", Set("3", "4")),
      ("2", Set("1", "2", "3", "4")),
      ("3", Set("1", "2", "3", "4")),
      ("4", Set("2", "3", "4"))
    )

    "generate prediction output correctly" in {
      val predicted = Source.fromFile(outputFile).getLines().toList
      predicted.map(convertToIDSet(_)) must
        containTheSameElementsAs(predictedExp)
    }

  }

  // TODO: add more tests...

}

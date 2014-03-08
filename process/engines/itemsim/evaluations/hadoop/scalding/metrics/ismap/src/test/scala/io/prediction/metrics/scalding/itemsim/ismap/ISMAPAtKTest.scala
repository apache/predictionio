package io.prediction.metrics.scalding.itemsim.ismap

import org.specs2.mutable._

import com.twitter.scalding._
import com.twitter.scalding.Dsl._

import io.prediction.commons.filepath.{ OfflineMetricFile }
import io.prediction.commons.scalding.settings.OfflineEvalResults

class ISMAPAtKTest extends Specification {
  def test(
    evalid: Int,
    metricid: Int,
    algoid: Int,
    iteration: Int,
    splitset: String,
    params: Map[String, String],
    relevantUsers: List[(String, String)],
    relevantItems: List[(String, String)],
    topKItems: List[(String, String, Double)],
    averagePrecision: List[(String, Double)],
    meanAveragePrecision: Double) = {
    val dbType = "file"
    val dbName = "testpath/"
    val dbHost = None
    val dbPort = None
    val hdfsRoot = "testroot/"

    val appid = 20
    val engineid = 1

    val offlineEvalResults = List((evalid, metricid, algoid, meanAveragePrecision, iteration, splitset))

    val relevantUsersSource = TypedTsv[(String, String)](OfflineMetricFile(hdfsRoot, appid, engineid, evalid, metricid, algoid, "relevantUsers.tsv"), ('ruiid, 'ruuid))
    val relevantItemsSource = TypedTsv[(String, String)](OfflineMetricFile(hdfsRoot, appid, engineid, evalid, metricid, algoid, "relevantItems.tsv"), ('riuid, 'riiid))
    val topKItemsSource = TypedTsv[(String, String, Double)](OfflineMetricFile(hdfsRoot, appid, engineid, evalid, metricid, algoid, "topKItems.tsv"), ('iid, 'simiid, 'score))

    JobTest("io.prediction.metrics.scalding.itemsim.ismap.ISMAPAtK")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("evalid", evalid.toString)
      .arg("metricid", metricid.toString)
      .arg("algoid", algoid.toString)
      .arg("iteration", iteration.toString)
      .arg("splitset", splitset)
      .arg("kParam", params("kParam"))
      .source(relevantUsersSource, relevantUsers)
      .source(relevantItemsSource, relevantItems)
      .source(topKItemsSource, topKItems)
      .sink[(String, Double)](Tsv(OfflineMetricFile(hdfsRoot, appid, engineid, evalid, metricid, algoid, "averagePrecision.tsv"))) { outputBuffer =>
        "correctly calculate Average Precision for each user" in {
          // only compare double up to 6 decimal places
          def roundingData(orgList: List[(String, Double)]) = {
            orgList map { x =>
              val (t1, t2) = x
              // NOTE: use HALF_UP mode to avoid error caused by rounding when compare data
              // (eg. 3.5 vs 3.499999999999, 0.6666666666 vs 0.666666667)
              (t1, BigDecimal(t2).setScale(6, BigDecimal.RoundingMode.HALF_UP).toDouble)
            }
          }

          roundingData(outputBuffer.toList) must containTheSameElementsAs(roundingData(averagePrecision))
        }
      }
      .sink[(Int, Int, Int, Double, Int, String)](OfflineEvalResults(dbType = dbType, dbName = dbName, dbHost = dbHost, dbPort = dbPort).getSource) { outputBuffer =>
        def roundingData(orgList: List[(Int, Int, Int, Double, Int, String)]) = {
          orgList map { x =>
            val (t1, t2, t3, t4, t5, t6) = x
            // NOTE: use HALF_UP mode to avoid error caused by rounding when compare data
            // (eg. 3.5 vs 3.499999999999, 0.6666666666 vs 0.666666667)
            (t1, t2, t3, BigDecimal(t4).setScale(6, BigDecimal.RoundingMode.HALF_UP).toDouble, t5, t6)
          }
        }
        "correctly write MAP@k score into a file" in {
          roundingData(outputBuffer.toList) must containTheSameElementsAs(roundingData(offlineEvalResults))
        }
      }
      .run
      .finish
  }

  "itemsim.ismap" should {
    val relevantUsers = List(
      ("i0", "u1"),
      ("i1", "u1"),
      ("i3", "u0"),
      ("i4", "u0"),
      ("i5", "u0"))
    val relevantItems = List(
      ("u0", "i3"),
      ("u0", "i4"),
      ("u0", "i5"),
      ("u1", "i0"),
      ("u1", "i1"),
      ("u1", "i5"),
      ("u3", "i0"))
    val topKItems = List(
      ("i0", "i6", 5.0),
      ("i0", "i4", 4.0),
      ("i0", "i3", 3.0),
      ("i0", "i5", 2.0),
      ("i1", "i4", 3.0),
      ("i1", "i5", 2.0),
      ("i1", "i0", 1.0))
    val averagePrecision = List(("i0", 0.0625), ("i1", 0.38888888888888884))
    val meanAveragePrecision = 0.225694

    val params = Map("kParam" -> "5")

    test(12, 2, 54, 9, "validation",
      params, relevantUsers, relevantItems, topKItems, averagePrecision, meanAveragePrecision)
  }
}

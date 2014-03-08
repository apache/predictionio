package io.prediction.metrics.scalding.itemrec.map

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.filepath.{ OfflineMetricFile }
import io.prediction.commons.scalding.settings.OfflineEvalResults

class MAPAtKTest extends Specification with TupleConversions {

  def test(
    evalid: Int, metricid: Int, algoid: Int, iteration: Int, splitset: String,
    params: Map[String, String],
    relevantItems: List[(String, String)],
    topKItems: List[(String, String)],
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

    JobTest("io.prediction.metrics.scalding.itemrec.map.MAPAtK")
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
      .source(Tsv(OfflineMetricFile(hdfsRoot, appid, engineid, evalid, metricid, algoid, "relevantItems.tsv")), relevantItems)
      .source(Tsv(OfflineMetricFile(hdfsRoot, appid, engineid, evalid, metricid, algoid, "topKItems.tsv")), topKItems)
      .sink[(String, Double)](Tsv(OfflineMetricFile(hdfsRoot, appid, engineid, evalid, metricid, algoid, "averagePrecision.tsv"))) { outputBuffer =>
        // only compare double up to 6 decimal places
        def roundingData(orgList: List[(String, Double)]) = {
          orgList map { x =>
            val (t1, t2) = x
            // NOTE: use HALF_UP mode to avoid error caused by rounding when compare data
            // (eg. 3.5 vs 3.499999999999, 0.6666666666 vs 0.666666667)
            (t1, BigDecimal(t2).setScale(6, BigDecimal.RoundingMode.HALF_UP).toDouble)
          }
        }

        "correctly calculate Average Precision for each user" in {
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

  "itemrec.map" should {
    val relevantItems = List(("u0", "i3,i4,i5"), ("u1", "i0,i1"), ("u3", "i0"))
    val topKItems = List(("u0", "i6,i4,i3,i5,i0"), ("u1", "i1,i4,i5,i0"))
    val averagePrecision = List(("u0", 0.638888888), ("u1", 0.75), ("u3", 0.0))
    val meanAveragePrecision = 0.4629629333333

    val params = Map("kParam" -> "5")

    test(2, 3, 4, 11, "",
      params, relevantItems, topKItems, averagePrecision, meanAveragePrecision)
  }

  "itemrec.map with different ids" should {
    val relevantItems = List(("u0", "i3,i4,i5"), ("u1", "i0,i1"), ("u3", "i0"))
    val topKItems = List(("u0", "i6,i4,i3,i5,i0"), ("u1", "i1,i4,i5,i0"))
    val averagePrecision = List(("u0", 0.638888888), ("u1", 0.75), ("u3", 0.0))
    val meanAveragePrecision = 0.4629629333333

    val params = Map("kParam" -> "5")

    test(12, 2, 54, 9, "validation",
      params, relevantItems, topKItems, averagePrecision, meanAveragePrecision)
  }

}
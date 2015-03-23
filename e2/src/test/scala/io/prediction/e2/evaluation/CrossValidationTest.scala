package io.prediction.e2.evaluation

import io.prediction.e2.fixture.{CrossValidationFixture, SharedSparkContext}
import org.scalatest.{Matchers, FlatSpec}

import io.prediction.e2.engine.LabeledPoint
import org.apache.spark.rdd.RDD


import scala.language.reflectiveCalls

class CrossValidationTest extends FlatSpec with Matchers
with SharedSparkContext with CrossValidationFixture {
  val labeledPoints = dataset.labeledPoints
  val dataCount = labeledPoints.length
  val evalKs = (1 to dataCount)
  type DataSplit = (RDD[LabeledPoint], RDD[LabeledPoint])

  "Fold count" should "equal evalK" in {
    evalKs.map {k =>
      val labeledPointsRdd = sc.parallelize(labeledPoints)
      val splits = CommonHelperFunctions.splitData(k, labeledPointsRdd)
      splits.length should be (k)
    }
  }

  "Testing data size" should  "equal total / evalK" in {
    evalKs.map {k =>
      val labeledPointsRdd = sc.parallelize(labeledPoints)
      val splits = CommonHelperFunctions.splitData(k, labeledPointsRdd)
      splits.map {split: DataSplit =>
        val foldSize = dataCount / k
        if (dataCount % k == 0) {
          split._2.count() should be(foldSize)
        }
        else {
          val diff = split._2.count().toInt - foldSize
          diff should be <= 1
        }
      }
    }
  }

  "Training + testing" should "equal original dataset" in {
    evalKs.map {k =>
      val labeledPointsRdd = sc.parallelize(labeledPoints)
      val splits = CommonHelperFunctions.splitData(k, labeledPointsRdd)
      splits.map {split: DataSplit =>
        val collected = split._1.union(split._2).collect()
        collected.toSet should equal (labeledPoints.toSet)
      }
    }
  }
}

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
  type D = LabeledPoint
  type TD = TrainingData
  type EI = Null
  type Q = Query
  type A = ActualResult
  type DataSplit = (TD, EI, RDD[(Q, A)])

  "Fold count" should "equal evalK" in {
    evalKs.map {k =>
      val labeledPointsRDD = sc.parallelize(labeledPoints)
      val splits = CommonHelperFunctions.splitData[D, TD, EI, Q, A](
        k,
        labeledPointsRDD,
        null,
        toTrainingData,
        toQuery,
        toActualResult)
      splits.length should be (k)
    }
  }


  "Testing data size" should  "equal total / evalK" in {
    val labeledPointsRDD = sc.parallelize(labeledPoints)
    evalKs.map {k =>
      val splits = CommonHelperFunctions.splitData[D, TD, EI, Q, A](
        k,
        labeledPointsRDD,
        null,
        toTrainingData,
        toQuery,
        toActualResult)
      splits.map {split: DataSplit =>
        val foldSize = dataCount / k
        if (dataCount % k == 0) {
          split._3.count() should be(foldSize)
        }
        else {
          val diff = split._3.count() - foldSize
          diff should be <= 1L
        }
      }
    }
  }

  "Training + testing" should "equal original dataset" in {
    val labeledPointsRDD = sc.parallelize(labeledPoints)
    evalKs.map {k =>
      val splits = CommonHelperFunctions.splitData[D, TD, EI, Q, A](
        k,
        labeledPointsRDD,
        null,
        toTrainingData,
        toQuery,
        toActualResult)
      splits.map {split: DataSplit =>
        val trainginDataRDD = sc.parallelize(split._1.labeledPoints)
        val collected = trainginDataRDD.union(split._2).collect()
        collected.toSet should equal (labeledPoints.toSet)
      }
    }
  }
}

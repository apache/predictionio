package io.prediction.e2.evaluation

import io.prediction.e2.fixture.SharedSparkContext
import org.scalatest.{Matchers, FlatSpec}

import io.prediction.e2.engine.LabeledPoint
import org.apache.spark.rdd.RDD


import scala.language.reflectiveCalls

object CrossValidationTest {
  case class TrainingData(labeledPoints: Seq[LabeledPoint])
  case class Query(features: Array[String])
  case class ActualResult(label: String)

  def toTrainingData(labeledPoints: RDD[LabeledPoint]) = TrainingData(labeledPoints.collect().toSeq)
  def toQuery(labeledPoint: LabeledPoint) = Query(labeledPoint.features)
  def toActualResult(labeledPoint: LabeledPoint) = ActualResult(labeledPoint.label)

}

class CrossValidationTest extends FlatSpec with Matchers
with SharedSparkContext{


  val Label1 = "l1"
  val Label2 = "l2"
  val Label3 = "l3"
  val Attribute1 = "a1"
  val NotAttribute1 = "na1"
  val Attribute2 = "a2"
  val NotAttribute2 = "na2"

  val labeledPoints = Seq(
    LabeledPoint(Label1, Array(Attribute1, Attribute2)),
    LabeledPoint(Label2, Array(NotAttribute1, Attribute2)),
    LabeledPoint(Label3, Array(Attribute1, NotAttribute2))
  )

  val dataCount = labeledPoints.size
  val evalKs = (1 to dataCount)
  type D = LabeledPoint
  type TD = CrossValidationTest.TrainingData
  type EI = Null
  type Q = CrossValidationTest.Query
  type A = CrossValidationTest.ActualResult
  type DataSplit = (TD, EI, RDD[(Q, A)])

  "Fold count" should "equal evalK" in {
      val labeledPointsRDD = sc.parallelize(labeledPoints)
      val splits = CommonHelperFunctions.splitData[D, TD, EI, Q, A](
        dataCount,
        labeledPointsRDD,
        null,
        CrossValidationTest.toTrainingData,
        CrossValidationTest.toQuery,
        CrossValidationTest.toActualResult)
      splits.size shouldEqual dataCount
  }


  "Testing data size" should  "equal total / evalK" in {
    val labeledPointsRDD = sc.parallelize(labeledPoints)
    evalKs.map {k =>
      val splits = CommonHelperFunctions.splitData[D, TD, EI, Q, A](
        k,
        labeledPointsRDD,
        null,
        CrossValidationTest.toTrainingData,
        CrossValidationTest.toQuery,
        CrossValidationTest.toActualResult)
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
        CrossValidationTest.toTrainingData,
        CrossValidationTest.toQuery,
        CrossValidationTest.toActualResult)
      splits.map {split: DataSplit =>
        val trainginDataRDD = sc.parallelize(split._1.labeledPoints)
        val collected = trainginDataRDD.union(split._2).collect()
        collected.toSet should equal (labeledPoints.toSet)
      }
    }
  }
}

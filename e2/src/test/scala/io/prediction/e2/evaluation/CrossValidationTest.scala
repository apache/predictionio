package io.prediction.e2.evaluation

import io.prediction.e2.evaluation.CrossValidationTest.EmptyEvaluationParams
import org.scalatest.{Matchers, FlatSpec}
import org.apache.spark.rdd.RDD
import io.prediction.e2.fixture.SharedSparkContext
import io.prediction.e2.engine.LabeledPoint



import scala.language.reflectiveCalls

object CrossValidationTest {
  case class TrainingData(labeledPoints: Seq[LabeledPoint])
  case class Query(features: Array[String])
  case class ActualResult(label: String)

  case class EmptyEvaluationParams()

  def toTrainingData(labeledPoints: RDD[LabeledPoint]) = TrainingData(labeledPoints.collect().toSeq)
  def toQuery(labeledPoint: LabeledPoint) = Query(labeledPoint.features)
  def toActualResult(labeledPoint: LabeledPoint) = ActualResult(labeledPoint.label)

}


class CrossValidationTest extends FlatSpec with Matchers
with SharedSparkContext{


  val Label1 = "l1"
  val Label2 = "l2"
  val Label3 = "l3"
  val Label4 = "l4"
  val Attribute1 = "a1"
  val NotAttribute1 = "na1"
  val Attribute2 = "a2"
  val NotAttribute2 = "na2"

  val labeledPoints = Seq(
    LabeledPoint(Label1, Array(Attribute1, Attribute2)),
    LabeledPoint(Label2, Array(NotAttribute1, Attribute2)),
    LabeledPoint(Label3, Array(Attribute1, NotAttribute2)),
    LabeledPoint(Label4, Array(NotAttribute1, NotAttribute2))
  )

  val dataCount = labeledPoints.size
  val evalKs = (1 to dataCount)
  val emptyParams = new CrossValidationTest.EmptyEvaluationParams()
  type Fold = (
    CrossValidationTest.TrainingData,
    CrossValidationTest.EmptyEvaluationParams,
    RDD[(CrossValidationTest.Query, CrossValidationTest.ActualResult)])

  def toTestTrain(dataSplit: Fold): (Seq[LabeledPoint], Seq[LabeledPoint]) = {
    val trainingData = dataSplit._1.labeledPoints
    val queryActual = dataSplit._3
    val testingData = queryActual.map { case (query, actual) =>
      LabeledPoint(actual.label, query.features)
    }
    (trainingData, testingData.collect().toSeq)
  }

  def splitData(k: Int, labeledPointsRDD: RDD[LabeledPoint]): Seq[Fold] = {
    CommonHelperFunctions.splitData[
      LabeledPoint,
      CrossValidationTest.TrainingData,
      CrossValidationTest.EmptyEvaluationParams,
      CrossValidationTest.Query,
      CrossValidationTest.ActualResult](
        k,
        labeledPointsRDD,
        emptyParams,
        CrossValidationTest.toTrainingData,
        CrossValidationTest.toQuery,
        CrossValidationTest.toActualResult)
  }



  "Fold count" should "equal evalK" in {
    val labeledPointsRDD = sc.parallelize(labeledPoints)
    val lengths = evalKs.map(k => splitData(k, labeledPointsRDD).length)
    lengths should be(evalKs)
  }


  "Testing data size" should  "be within 1 of total / evalK" in {
    val labeledPointsRDD = sc.parallelize(labeledPoints)
    val splits = evalKs.map(k => k -> splitData(k, labeledPointsRDD))
    val diffs = splits.map { case (k, folds) =>
      folds.map(fold => fold._3.count() - dataCount / k)
    }
    diffs.flatMap(folds => if (folds.exists(_ > 1)) Some(true) else None) should be('empty)
    diffs.map(folds => folds.sum) should be(evalKs.map(k => dataCount % k))
  }



  "Training + testing" should "equal original dataset" in {
    val labeledPointsRDD = sc.parallelize(labeledPoints)
    val splits = evalKs.map(k => splitData(k, labeledPointsRDD))
    val reJoined = splits.flatMap {folds =>
      folds.map {fold =>
        val (training, testing) = toTestTrain(fold)
        (training ++ testing).toSet
      }
    }
    reJoined should be(reJoined.map(k => labeledPoints.toSet))

  }

  "Training and testing" should "be disjoint" in {
    val labeledPointsRDD = sc.parallelize(labeledPoints)
    val splits = evalKs.map(k => k -> splitData(k, labeledPointsRDD))
    val intersections = splits.flatMap { case (k, folds) =>
      folds.flatMap { fold =>
        val (training, testing) = toTestTrain(fold)
        training.toSet.intersect(testing.toSet)
      }
    }
    intersections should be('empty)
  }
}

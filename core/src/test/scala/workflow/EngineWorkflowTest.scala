package io.prediction.workflow


import org.scalatest.FunSuite
import org.scalatest.Inside
import org.scalatest.Matchers._
import org.scalatest.Inspectors._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

import io.prediction.controller._
import io.prediction.core._
import grizzled.slf4j.{ Logger, Logging }

import java.lang.Thread

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite

class EngineWorkflowTrainSuite extends FunSuite with SharedSparkContext {
  import io.prediction.controller.Engine0._
  test("Parallel DS/P/Algos") {
    val models = EngineWorkflow.train(
      sc,
      new PDataSource0(0),
      new PPreparator0(1),
      Seq(
        new PAlgo0(2),
        new PAlgo1(3),
        new PAlgo0(4)))

    val pd = ProcessedData(1, TrainingData(0))

    models should contain theSameElementsAs Seq(
      PAlgo0.Model(2, pd), PAlgo1.Model(3, pd), PAlgo0.Model(4, pd))
  }

  test("Local DS/P/Algos") {
    val models = EngineWorkflow.train(
      sc,
      new LDataSource0(0),
      new LPreparator0(1),
      Seq(
        new LAlgo0(2),
        new LAlgo1(3),
        new LAlgo0(4)))
    
    val pd = ProcessedData(1, TrainingData(0))

    val expectedResults = Seq(
      LAlgo0.Model(2, pd),
      LAlgo1.Model(3, pd),
      LAlgo0.Model(4, pd))

    forAll(models.zip(expectedResults)) { case (model, expected) => 
      model shouldBe a [RDD[_]]
      val localModel = model.asInstanceOf[RDD[_]].collect
      localModel should contain theSameElementsAs Seq(expected)
    }
  }

  test("P2L DS/P/Algos") {
    val models = EngineWorkflow.train(
      sc,
      new PDataSource0(0),
      new PPreparator0(1),
      Seq(
        new NAlgo0(2),
        new NAlgo1(3),
        new NAlgo0(4)))

    val pd = ProcessedData(1, TrainingData(0))
    
    models should contain theSameElementsAs Seq(
      NAlgo0.Model(2, pd), NAlgo1.Model(3, pd), NAlgo0.Model(4, pd))
  }
}


class EngineWorkflowEvalSuite
extends FunSuite with Inside with SharedSparkContext {
  import io.prediction.controller.Engine0._

  @transient lazy val logger = Logger[this.type] 
  
  test("Simple Parallel DS/P/A/S") {
    val en = 2
    val qn = 5

    val evalDataSet: Seq[(EvalInfo, RDD[(Query, Prediction, Actual)])] = 
    EngineWorkflow.eval(
      sc,
      new PDataSource1(id = 1, en = en, qn = qn),
      new PPreparator0(id = 2),
      Seq(new PAlgo0(id = 3)),
      new LServing0(id = 10))

    val pd = ProcessedData(2, TrainingData(1))
    val model0 = PAlgo0.Model(3, pd)

    forAll(evalDataSet.zipWithIndex) { case (evalData, ex) => {
      val (evalInfo, qpaRDD) = evalData
      evalInfo shouldBe EvalInfo(1)

      val qpaSeq: Seq[(Query, Prediction, Actual)] = qpaRDD.collect
      forAll (qpaSeq) { case (q, p, a) => 
        val Query(qId, qEx, qQx) = q
        val Actual(aId, aEx, aQx) = a
        qId shouldBe aId
        qEx shouldBe ex
        aEx shouldBe ex
        qQx shouldBe aQx

        inside (p) { case Prediction(pId, pQ, pModels, pPs) => {
          pId shouldBe 10
          pQ shouldBe q
          pModels shouldBe None
          pPs should have size 1
          pPs shouldBe Seq(
            Prediction(id = 3, q = q, models = Some(model0)))
        }}
      }

    }}

  }

  test("Parallel DS/P/A/S") {
    val en = 2
    val qn = 5

    val evalDataSet: Seq[(EvalInfo, RDD[(Query, Prediction, Actual)])] = 
    EngineWorkflow.eval(
      sc,
      new PDataSource1(id = 1, en = en, qn = qn),
      new PPreparator0(id = 2),
      Seq(
        new PAlgo0(id = 3), 
        new PAlgo1(id = 4),
        new NAlgo1(id = 5)),
      new LServing0(id = 10))

    val pd = ProcessedData(2, TrainingData(1))
    val model0 = PAlgo0.Model(3, pd)
    val model1 = PAlgo1.Model(4, pd)
    val model2 = NAlgo1.Model(5, pd)

    forAll(evalDataSet.zipWithIndex) { case (evalData, ex) => {
      val (evalInfo, qpaRDD) = evalData
      evalInfo shouldBe EvalInfo(1)

      val qpaSeq: Seq[(Query, Prediction, Actual)] = qpaRDD.collect
      forAll (qpaSeq) { case (q, p, a) => 
        val Query(qId, qEx, qQx) = q
        val Actual(aId, aEx, aQx) = a
        qId shouldBe aId
        qEx shouldBe ex
        aEx shouldBe ex
        qQx shouldBe aQx

        inside (p) { case Prediction(pId, pQ, pModels, pPs) => {
          pId shouldBe 10
          pQ shouldBe q
          pModels shouldBe None
          pPs should have size 3
          pPs shouldBe Seq(
            Prediction(id = 3, q = q, models = Some(model0)),
            Prediction(id = 4, q = q, models = Some(model1)),
            Prediction(id = 5, q = q, models = Some(model2))
          )
        }}
      }
    }}
  }
  
  test("Local DS/P/A/S") {
    val en = 2
    val qn = 5

    val evalDataSet: Seq[(EvalInfo, RDD[(Query, Prediction, Actual)])] = 
    EngineWorkflow.eval(
      sc,
      new LDataSource0(id = 1, en = en, qn = qn),
      new LPreparator0(id = 2),
      Seq(
        new LAlgo0(id = 3), 
        new LAlgo1(id = 4),
        new LAlgo1(id = 5)),
      new LServing0(id = 10))

    val pd = ProcessedData(2, TrainingData(1))
    val model0 = LAlgo0.Model(3, pd)
    val model1 = LAlgo1.Model(4, pd)
    val model2 = LAlgo1.Model(5, pd)

    forAll(evalDataSet.zipWithIndex) { case (evalData, ex) => {
      val (evalInfo, qpaRDD) = evalData
      evalInfo shouldBe EvalInfo(1)

      val qpaSeq: Seq[(Query, Prediction, Actual)] = qpaRDD.collect
      forAll (qpaSeq) { case (q, p, a) => 
        val Query(qId, qEx, qQx) = q
        val Actual(aId, aEx, aQx) = a
        qId shouldBe aId
        qEx shouldBe ex
        aEx shouldBe ex
        qQx shouldBe aQx

        inside (p) { case Prediction(pId, pQ, pModels, pPs) => {
          pId shouldBe 10
          pQ shouldBe q
          pModels shouldBe None
          pPs should have size 3
          pPs shouldBe Seq(
            Prediction(id = 3, q = q, models = Some(model0)),
            Prediction(id = 4, q = q, models = Some(model1)),
            Prediction(id = 5, q = q, models = Some(model2))
          )
        }}
      }

    }}

  }
}


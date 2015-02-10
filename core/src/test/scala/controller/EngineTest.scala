package io.prediction.controller

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
import io.prediction.workflow.SharedSparkContext
import io.prediction.workflow.PersistentModelManifest
import grizzled.slf4j.{ Logger, Logging }

import _root_.java.lang.Thread

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite

class EngineDevSuite
extends FunSuite with Inside with SharedSparkContext {
  import io.prediction.controller.Engine0._
  @transient lazy val logger = Logger[this.type] 

  test("Engine.train") {
    val engine = new Engine(
      classOf[PDataSource2],
      classOf[PPreparator1],
      Map("" -> classOf[PAlgo2]),
      classOf[LServing1])

    val engineParams = EngineParams(
      dataSourceParams = PDataSource2.Params(0),
      preparatorParams = PPreparator1.Params(1),
      algorithmParamsList = Seq(("", PAlgo2.Params(2))),
      servingParams = LServing1.Params(3))

    val models = engine.train(sc, engineParams)
    
    val pd = ProcessedData(1, TrainingData(0))

    // PAlgo2.Model doesn't have IPersistentModel trait implemented. Hence the
    // model extract after train is Unit.
    models should contain theSameElementsAs Seq(Unit)
  }

  test("Engine.train persisting PAlgo.Model") {
    val engine = new Engine(
      classOf[PDataSource2],
      classOf[PPreparator1],
      Map(
        "PAlgo2" -> classOf[PAlgo2],
        "PAlgo3" -> classOf[PAlgo3]
      ),
      classOf[LServing1])

    val engineParams = EngineParams(
      dataSourceParams = PDataSource2.Params(0),
      preparatorParams = PPreparator1.Params(1),
      algorithmParamsList = Seq(
        ("PAlgo2", PAlgo2.Params(2)),
        ("PAlgo3", PAlgo3.Params(21)),
        ("PAlgo3", PAlgo3.Params(22))
      ),
      servingParams = LServing1.Params(3))

    val pd = ProcessedData(1, TrainingData(0))
    val model21 = PAlgo3.Model(21, pd)
    val model22 = PAlgo3.Model(22, pd)

    val models = engine.train(sc, engineParams)

    val pModel21 = PersistentModelManifest(model21.getClass.getName)
    val pModel22 = PersistentModelManifest(model22.getClass.getName)
    
    models should contain theSameElementsAs Seq(Unit, pModel21, pModel22)
  }

  test("Engine.train persisting LAlgo.Model") {
    val engine = Engine(
      classOf[LDataSource1],
      classOf[LPreparator1],
      Map(
        "LAlgo1" -> classOf[LAlgo1],
        "LAlgo2" -> classOf[LAlgo2],
        "LAlgo3" -> classOf[LAlgo3]
      ),
      classOf[LServing1])

    val engineParams = EngineParams(
      dataSourceParams = LDataSource1.Params(0),
      preparatorParams = LPreparator1.Params(1),
      algorithmParamsList = Seq(
        ("LAlgo2", LAlgo2.Params(20)),
        ("LAlgo2", LAlgo2.Params(21)),
        ("LAlgo3", LAlgo3.Params(22))),
      servingParams = LServing1.Params(3))

    val pd = ProcessedData(1, TrainingData(0))
    val model20 = LAlgo2.Model(20, pd)
    val model21 = LAlgo2.Model(21, pd)
    val model22 = LAlgo3.Model(22, pd)

    val models = engine.train(sc, engineParams)

    val pModel20 = PersistentModelManifest(model20.getClass.getName)
    val pModel21 = PersistentModelManifest(model21.getClass.getName)
    
    models should contain theSameElementsAs Seq(pModel20, pModel21, model22)
  }
  
  test("Engine.train persisting P&NAlgo.Model") {
    val engine = new Engine(
      classOf[PDataSource2],
      classOf[PPreparator1],
      Map(
        "PAlgo2" -> classOf[PAlgo2],
        "PAlgo3" -> classOf[PAlgo3],
        "NAlgo2" -> classOf[NAlgo2],
        "NAlgo3" -> classOf[NAlgo3]
      ),
      classOf[LServing1])

    val engineParams = EngineParams(
      dataSourceParams = PDataSource2.Params(0),
      preparatorParams = PPreparator1.Params(1),
      algorithmParamsList = Seq(
        ("PAlgo2", PAlgo2.Params(20)),
        ("PAlgo3", PAlgo3.Params(21)),
        ("PAlgo3", PAlgo3.Params(22)),
        ("NAlgo2", NAlgo2.Params(23)),
        ("NAlgo3", NAlgo3.Params(24)),
        ("NAlgo3", NAlgo3.Params(25))
      ),
      servingParams = LServing1.Params(3))

    val pd = ProcessedData(1, TrainingData(0))
    val model21 = PAlgo3.Model(21, pd)
    val model22 = PAlgo3.Model(22, pd)
    val model23 = NAlgo2.Model(23, pd)
    val model24 = NAlgo3.Model(24, pd)
    val model25 = NAlgo3.Model(25, pd)

    val models = engine.train(sc, engineParams)

    val pModel21 = PersistentModelManifest(model21.getClass.getName)
    val pModel22 = PersistentModelManifest(model22.getClass.getName)
    val pModel23 = PersistentModelManifest(model23.getClass.getName)
    
    models should contain theSameElementsAs Seq(
      Unit, pModel21, pModel22, pModel23, model24, model25)
  }

  test("Engine.eval") {
    val engine = new Engine(
      classOf[PDataSource2],
      classOf[PPreparator1],
      Map("" -> classOf[PAlgo2]),
      classOf[LServing1])

    val qn = 10
    val en = 3

    val engineParams = EngineParams(
      dataSourceParams = PDataSource2.Params(id = 0, en = en, qn = qn),
      preparatorParams = PPreparator1.Params(1),
      algorithmParamsList = Seq(("", PAlgo2.Params(2))),
      servingParams = LServing1.Params(3))

    val algoCount = engineParams.algorithmParamsList.size
    val pd = ProcessedData(1, TrainingData(0))
    val model0 = PAlgo2.Model(2, pd)

    val evalDataSet = engine.eval(sc, engineParams)

    evalDataSet should have size en

    forAll(evalDataSet.zipWithIndex) { case (evalData, ex) => {
      val (evalInfo, qpaRDD) = evalData
      evalInfo shouldBe EvalInfo(0)

      val qpaSeq: Seq[(Query, Prediction, Actual)] = qpaRDD.collect

      qpaSeq should have size qn

      forAll (qpaSeq) { case (q, p, a) => 
        val Query(qId, qEx, qQx) = q
        val Actual(aId, aEx, aQx) = a
        qId shouldBe aId
        qEx shouldBe ex
        aEx shouldBe ex
        qQx shouldBe aQx

        inside (p) { case Prediction(pId, pQ, pModels, pPs) => {
          pId shouldBe 3
          pQ shouldBe q
          pModels shouldBe None
          pPs should have size algoCount
          pPs shouldBe Seq(
            Prediction(id = 2, q = q, models = Some(model0)))
        }}
      }
    }}
  }
}

package io.prediction.workflow

import _root_.java.lang.Thread

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


import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite

class EvaluationWorkflowDevSuite extends FunSuite with SharedSparkContext {
  import io.prediction.controller.Engine1._
  test("Evaluation return best engine params, simple result type: Double") {
    val engine = new Engine1()
    val ep0 = EngineParams(dataSourceParams = Engine1.DSP(0.2))
    val ep1 = EngineParams(dataSourceParams = Engine1.DSP(0.3))
    val ep2 = EngineParams(dataSourceParams = Engine1.DSP(0.3))
    val ep3 = EngineParams(dataSourceParams = Engine1.DSP(-0.2))
    val engineParamsList = Seq(ep0, ep1, ep2, ep3)

    val metric = new Metric0()

    val (bestEngineParams, bestScore) = EvaluationWorkflow.runEvaluation(
      sc,
      engine,
      engineParamsList,
      metric) 
  
    bestEngineParams shouldBe ep1
    bestScore shouldBe 0.3
  }

  test("Evaluation return best engine params, complex result type") {
    val engine = new Engine1()
    val ep0 = EngineParams(dataSourceParams = Engine1.DSP(0.2))
    val ep1 = EngineParams(dataSourceParams = Engine1.DSP(0.3))
    val ep2 = EngineParams(dataSourceParams = Engine1.DSP(0.3))
    val ep3 = EngineParams(dataSourceParams = Engine1.DSP(-0.2))
    val engineParamsList = Seq(ep0, ep1, ep2, ep3)

    val metric = new Metric1()

    val (bestEngineParams, bestScore) = EvaluationWorkflow.runEvaluation(
      sc,
      engine,
      engineParamsList,
      metric) 
  
    bestEngineParams shouldBe ep1
    bestScore shouldBe Metric1.Result(0, 0.3)
  }
}

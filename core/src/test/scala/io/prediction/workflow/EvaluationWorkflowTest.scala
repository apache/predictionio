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

class EvaluationWorkflowSuite extends FunSuite with SharedSparkContext {
  import io.prediction.controller.Engine1._
  test("Evaluation return best engine params, simple result type: Double") {
    val engine = new Engine1()
    val ep0 = EngineParams(dataSourceParams = Engine1.DSP(0.2))
    val ep1 = EngineParams(dataSourceParams = Engine1.DSP(0.3))
    val ep2 = EngineParams(dataSourceParams = Engine1.DSP(0.3))
    val ep3 = EngineParams(dataSourceParams = Engine1.DSP(-0.2))
    val engineParamsList = Seq(ep0, ep1, ep2, ep3)

    val evaluator = new MetricEvaluator(new Metric0())

    val result = EvaluationWorkflow.runEvaluation(
      sc,
      engine,
      engineParamsList,
      evaluator,
      WorkflowParams())

    result.bestScore.score shouldBe 0.3
    result.bestEngineParams shouldBe ep1
  }

  test("Evaluation return best engine params, complex result type") {
    val engine = new Engine1()
    val ep0 = EngineParams(dataSourceParams = Engine1.DSP(0.2))
    val ep1 = EngineParams(dataSourceParams = Engine1.DSP(0.3))
    val ep2 = EngineParams(dataSourceParams = Engine1.DSP(0.3))
    val ep3 = EngineParams(dataSourceParams = Engine1.DSP(-0.2))
    val engineParamsList = Seq(ep0, ep1, ep2, ep3)

    val evaluator = new MetricEvaluator(new Metric1())

    val result = EvaluationWorkflow.runEvaluation(
      sc,
      engine,
      engineParamsList,
      evaluator,
      WorkflowParams())
  
    result.bestScore.score shouldBe Metric1.Result(0, 0.3)
    result.bestEngineParams shouldBe ep1
  }
}

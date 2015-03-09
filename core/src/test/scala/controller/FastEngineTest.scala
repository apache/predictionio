package io.prediction.controller.experimental

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
import io.prediction.workflow.StopAfterReadInterruption
import io.prediction.workflow.StopAfterPrepareInterruption
import grizzled.slf4j.{ Logger, Logging }

import _root_.java.lang.Thread

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite
import scala.util.Random

class FastEngineDevSuite
extends FunSuite with Inside with SharedSparkContext {
  import io.prediction.controller.Engine0._

  test("Build Prefix Tree") {
    val engine = new Engine(
      classOf[PDataSource2],
      classOf[PPreparator1],
      Map(
        "algo2" -> classOf[PAlgo2],
        "algo3" -> classOf[PAlgo3]
      ),
      classOf[LServing1])

    val ep0 = EngineParams(
      dataSourceParams = PDataSource2.Params(0),
      preparatorParams = PPreparator1.Params(1),
      algorithmParamsList = Seq(("algo2", PAlgo2.Params(2))),
      servingParams = LServing1.Params(3))
    val ep1 = ep0.copy(
      servingParams = ("", LServing1.Params(4)))
    val ep2 = ep0.copy(
      servingParams = ("", LServing1.Params(3)))
    val ep3 = ep0.copy(
      preparatorParams = ("", PPreparator1.Params(2)))
    val ep4 = ep0.copy(
      algorithmParamsList = Seq(
        ("algo2", PAlgo2.Params(3)),
        ("algo2", PAlgo2.Params(3))))
    val ep5 = ep0.copy(
      algorithmParamsList = Seq(
        ("algo2", PAlgo2.Params(3)),
        ("algo2", PAlgo2.Params(3))),
      servingParams = ("", LServing1.Params(3)))
  
    FastEvalEngineWorkflow.start(
      engine = engine,
      engineParamsList = Seq(ep0, ep1, ep2, ep3, ep4, ep5))

  }
}


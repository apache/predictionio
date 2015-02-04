package n.io.prediction.controller

import org.scalatest.FunSuite
import org.scalatest.Inside
import org.scalatest.Matchers._
import org.scalatest.Inspectors._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

import n.io.prediction.controller._
import n.io.prediction.core._
import n.io.prediction.workflow.SharedSparkContext
import io.prediction.controller.EngineParams
import grizzled.slf4j.{ Logger, Logging }

import java.lang.Thread

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite

class EngineDevSuite extends FunSuite with SharedSparkContext {
  import n.io.prediction.controller.Engine0._
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

    models should contain theSameElementsAs Seq(PAlgo2.Model(2, pd))
  }

}


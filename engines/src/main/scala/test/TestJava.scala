package io.prediction.engines.test

import io.prediction.First

import io.prediction.engines.stock._
import com.github.nscala_time.time.Imports._
import java.lang.Integer
import scala.collection.JavaConversions._

import io.prediction.FirstAlgo
import io.prediction.EmptyParams
//import io.prediction.SecondAlgo

import io.prediction.engines.java.test.SimpleDataPreparator
import io.prediction.engines.java.test.DataPreparator
import io.prediction.engines.java.test.EvaluationDataParams

import io.prediction.engines.java.regression
import io.prediction.DefaultServer

import io.prediction.workflow.DebugWorkflow;

abstract class AlgoClass[M] {
  def f(e: Int): Int = {
    return g(e) + 10
  }

  def g(e: Int): Int

  def get(e: Int): M

  def getBactch(e: Int): M = get(e + 20)

  def s(): String = "Algo"
}

/*
trait AlgoTrait extends AlgoClass {
  def g(e: Int): Int
}
*/

object RunJava {
  val tickerList = Seq("GOOG", "AAPL", "FB", "GOOGL", "MSFT")

  def t() {
    val f = new First()
    println("start")
    println(f.get())
    f.inc(10)
    println(f.get())
    f.inc(-4)
    println(f.get())
    println("done")

    val trainingDataParams = new TrainingDataParams(
      baseDate = new DateTime(2010, 1, 1, 0, 0),
      untilIdx = 30,
      windowSize = 30,
      marketTicker = "SPY",
      tickerList = Seq("GOOG", "AAPL", "MSFT"))

    val dataPrep = new StockDataPreparator

    val trainingData = dataPrep.prepareTraining(trainingDataParams)

    println(trainingData)
 
    f.train(trainingData)

    val x = (1,3)
    f.t(x)
    
    val i = Int.box(10)
    f.s(i)

    val qq: Integer = f.qq()
    println("f.q" + qq)

    val m = f.p()
    m.foreach { println }
  }

  def tt() {
    val algo = new FirstAlgo()
    println(algo.s)

    println(algo.f(100))

    /*
    val algo2 = new SecondAlgo()
    println(algo2.s)

    println(algo2.f(100))
    */

    val m = algo.get(100)
    println(m)
  }
  
  def test() {
    val edp = new EvaluationDataParams(15, 4)
    //val dp = new DataPreparator()
    val dp = new SimpleDataPreparator()
    //val r = dp.prepareValidation(edp)
    //println(r)
    //DebugWorkflow.dataPrep(dp, "Java DataPrep", edp)
  }

  def main(args: Array[String]) {
    val edp = new regression.DataParams("data/lr_data.txt")
    val dp = new regression.DataPreparator()

    val c = new regression.Cleanser()
    val cp = new regression.CleanserParams(0.8)
    
    val algo = new regression.Algo()
    val ep = EmptyParams()

    val dummyAlgo = new regression.DummyAlgo()

    val algoMap = Map(
      "" -> algo,
      "dummy" -> dummyAlgo)
    val algoParamsList = Seq(
      ("", ep),
      ("dummy", ep))

    val server = new regression.Server()
    val sp = EmptyParams()

    val validator = new regression.Validator()
    val vp = EmptyParams()

    DebugWorkflow.run(
      dataPrep = dp, 
      evalDataParams = edp,
      cleanser = c,
      cleanserParams = cp,
      algoMap = algoMap,
      algoParamsList = algoParamsList,
      server = server,
      serverParams = sp,
      validator = validator,
      validatorParams = vp,
      batch = "Java Workflow"
    )
  }
}











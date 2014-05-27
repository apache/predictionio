package io.prediction.engines.itemrank

import io.prediction.{ PIORunner, BaseEngine, DefaultServer }
import com.github.nscala_time.time.Imports._

object Runner {

  def main(args: Array[String]) {
    val evalParams = new EvalParams(
      appid = 1,
      itypes = None,
      actions = Map(
        "view" -> Some(3),
        "like" -> Some(5),
        "conversion" -> Some(4),
        "rate" -> None
      ),
      conflict = "latest",
      //recommendationTime = 123456,
      seenActions = Some(Set("conversion")),
      //testUsers = Set("u0", "u1", "u2", "u3"),
      //testItems = Set("i0", "i1", "i2"),
      //(int years, int months, int weeks, int days, int hours,
      // int minutes, int seconds, int millis)
      period = new Period(0, 0, 0, 1, 0, 0, 0, 0),
      trainStart = new DateTime("2014-04-01T00:00:00.000"),
      testStart = new DateTime("2014-04-20T00:00:00.000"),
      testUntil = new DateTime("2014-04-30T00:00:00.000"),
      goal = Set("conversion", "view")
    )

    val knnAlgoParams = new KNNAlgoParams
    val randomAlgoParams = new RandomAlgoParams
    val serverParams = null

    val engine = new BaseEngine(
      classOf[ItemRankDataPreparator],
      Map("knn" -> classOf[KNNAlgorithm],
        "rand" -> classOf[RandomAlgorithm]),
      classOf[DefaultServer[Feature, Prediction]]
    )

    val evaluator = new ItemRankEvaluator
    val evalDataPrep = new ItemRankDataPreparator

    PIORunner(
      evalParams,
      ("knn", knnAlgoParams),
      serverParams,
      engine,
      evaluator,
      evalDataPrep)

    PIORunner(
      evalParams,
      ("rand", randomAlgoParams),
      serverParams,
      engine,
      evaluator,
      evalDataPrep)

  }

}

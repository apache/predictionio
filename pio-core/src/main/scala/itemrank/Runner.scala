package io.prediction.itemrank

import io.prediction.{ PIORunner, BaseEngine, DefaultServer }

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
      recommendationTime = 123456,
      seenActions = Some(Set("conversion")),
      testUsers = Set("u0", "u1", "u2", "u3"),
      testItems = Set("i0", "i1", "i2"),
      goal = Set("conversion", "view")
    )

    val algoParams = new AlgoParams
    val engine = new BaseEngine(
      classOf[DataPreparator],
      Map("knn" -> classOf[Algorithm]),
      classOf[DefaultServer[Feature, Target]]
    )

    val evaluator = new Evaluator
    val evalDataPrep = new DataPreparator

    PIORunner.run(
      evalParams,
      ("knn", algoParams),
      engine,
      evaluator,
      evalDataPrep)

  }

}

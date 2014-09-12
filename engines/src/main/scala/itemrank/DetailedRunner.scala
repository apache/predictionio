package io.prediction.engines.itemrank

import io.prediction.controller.EmptyParams
import io.prediction.controller.EngineParams
import io.prediction.controller.Workflow
import io.prediction.controller.WorkflowParams

import com.github.nscala_time.time.Imports._

object DetailedRunner {

  def main(args: Array[String]) {
    /*val dsp = new DataSourceParams(
      appid = 1,
      itypes = None,
      actions = Set("view", "like", "conversion", "rate"),
      //(int years, int months, int weeks, int days, int hours,
      // int minutes, int seconds, int millis)
      // number of hours of each period
      hours = 24,//new Period(0, 0, 0, 1, 0, 0, 0, 0),
      trainStart = new DateTime("2014-04-01T00:00:00.000"),
      testStart = new DateTime("2014-04-20T00:00:00.000"),
      testUntil = new DateTime("2014-04-21T00:00:00.000"),
      goal = Set("conversion", "view"),
      verbose = true
    )*/

    val dsp = EventsDataSoureParams(
      appId = 1,
      itypes = None,
      actions = Set("view", "like", "dislike", "conversion", "rate"),
      startTime = None,
      untilTime = None,
      attributeNames = AttributeNames(
        user = "pio_user",
        item = "pio_item",
        u2iActions = Set("view", "like", "dislike", "conversion", "rate"),
        itypes = "pio_itypes",
        starttime = "pio_starttime",
        endtime = "pio_endtime",
        inactive = "pio_inactive",
        rating = "pio_rate"
      )
    )

    val pp = new PreparatorParams(
      actions = Map(
        "view" -> Some(3),
        "like" -> Some(5),
        "conversion" -> Some(4),
        "rate" -> None
      ),
      conflict = "latest"
    )

    val knnAlgoParams = new KNNAlgoParams(
      similarity = "cosine",
      k = 10)
    val randomAlgoParams = new RandomAlgoParams()
    val mahoutAlgoParams = new MahoutItemBasedAlgoParams(
      booleanData = true,
      itemSimilarity = "LogLikelihoodSimilarity",
      weighted = false,
      nearestN = 10,
      threshold = 5e-324,
      numSimilarItems = 50
    )

    val sp = new EmptyParams()

    val engine = ItemRankEngine()
    val engineParams = new EngineParams(
      dataSourceParams = dsp,
      preparatorParams = pp,
      algorithmParamsList = Seq(("knn", knnAlgoParams)),
      // Seq(("rand", randomAlgoParams))
      // Seq(("mahout", mahoutAlgoParams))
      servingParams = sp
    )

    val mp = new DetailedMetricsParams()
    Workflow.runEngine(
      params = WorkflowParams(
        batch = "Imagine: Local ItemRank Engine",
        verbose = 3),
      engine = engine,
      engineParams = engineParams,
      metricsClassOpt = Some(classOf[ItemRankDetailedMetrics]),
      metricsParams = mp
    )
  }
}

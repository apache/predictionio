package io.prediction.examples.mlc

import io.prediction.engines.itemrank._
import io.prediction.engines.base.AttributeNames
import io.prediction.engines.base.EventsSlidingEvalParams
import io.prediction.controller._

import com.github.nscala_time.time.Imports._

object CommonParams {
  val DataSourceAttributeNames = AttributeNames(
    user = "pio_user",
    item = "pio_item",
    u2iActions = Set("rate"),
    itypes = "pio_itypes",
    starttime = "pio_starttime",
    endtime = "pio_endtime",
    inactive = "pio_inactive",
    rating = "pio_rating")
}

object First {
  def main(args: Array[String]) {
    // Engine Settings
    val engine = ItemRankEngine() 

    val dsp = EventsDataSourceParams(
      appId = 9,
      actions = Set("rate"),
      attributeNames = CommonParams.DataSourceAttributeNames,
      slidingEval = Some(new EventsSlidingEvalParams(
        firstTrainingUntilTime = new DateTime(1998, 2, 1, 0, 0)))
    )

    val pp = new PreparatorParams(
      actions = Map("rate" -> None),
      conflict = "latest"
    )

    val mahoutAlgoParams = new mahout.ItemBasedAlgoParams(
      booleanData = true,
      itemSimilarity = "LogLikelihoodSimilarity",
      weighted = false,
      nearestN = 10,
      threshold = 4.9E-324,
      numSimilarItems = 50,
      numUserActions = 50,
      freshness = 0,
      freshnessTimeUnit = 86400,
      recommendationTime = Some(DateTime.now.millis)
    )

    val engineParams = new EngineParams(
      dataSourceParams = dsp,
      preparatorParams = pp,
      algorithmParamsList = Seq(("mahoutItemBased", mahoutAlgoParams))
    )

    // Metrics Setting
    val metricsParams = new DetailedMetricsParams(
      optOutputPath = None,
      actionsMap = Map("rate" -> None),
      goodThreshold = 3,
      measureType = MeasureType.PrecisionAtK,
      measureK = 10
    ) 

    // Run
    Workflow.runEngine(
      params = WorkflowParams(batch = "MLC: First Evaluation"),
      engine = engine,
      engineParams = engineParams,
      metricsClassOpt = Some(classOf[ItemRankDetailedMetrics]),
      metricsParams = metricsParams
    )
  }
}



package io.prediction.examples.mlc

import io.prediction.engines.itemrec.ItemRecEngine
import io.prediction.engines.itemrec.EventsDataSourceParams
import io.prediction.engines.itemrec.PreparatorParams
import io.prediction.engines.itemrec.NCItemBasedAlgorithmParams
import io.prediction.engines.itemrec.EvalParams
import io.prediction.engines.itemrec.ItemRecMetrics
import io.prediction.engines.itemrec.ItemRecMetricsParams
import io.prediction.engines.itemrec.MeasureType
import io.prediction.engines.base.EventsSlidingEvalParams
import io.prediction.engines.base.BinaryRatingParams

import io.prediction.controller.EngineParams
import io.prediction.controller.Workflow
import io.prediction.controller.WorkflowParams

import com.github.nscala_time.time.Imports._

object ItemRecEvaluation1 {
  def main(args: Array[String]) {
    val engine = ItemRecEngine()
    
    val dsp = EventsDataSourceParams(
      appId = 9,
      actions = Set("rate"),
      attributeNames = CommonParams.DataSourceAttributeNames,
      slidingEval = Some(new EventsSlidingEvalParams(
        firstTrainingUntilTime = new DateTime(1998, 2, 1, 0, 0),
        evalDuration = Duration.standardDays(7),
        evalCount = 12)),
      evalParams = Some(new EvalParams(queryN = 10))
    )
  
    val pp = new PreparatorParams(
      actions = Map("rate" -> None),
      seenActions = Set("rate"),
      conflict = "latest")

    val ncMahoutAlgoParams = new NCItemBasedAlgorithmParams(
      booleanData = true,
      itemSimilarity = "LogLikelihoodSimilarity",
      weighted = false,
      threshold = 4.9E-324,
      nearestN = 10,
      unseenOnly = false,
      freshness = 0,
      freshnessTimeUnit = 86400)
    
    val engineParams = new EngineParams(
      dataSourceParams = dsp,
      preparatorParams = pp,
      algorithmParamsList = Seq(
        ("ncMahoutItemBased", ncMahoutAlgoParams)))

    val metricsParams = new ItemRecMetricsParams(
      ratingParams = new BinaryRatingParams(
        actionsMap = Map("rate" -> None),
        goodThreshold = 3),
      measureType = MeasureType.PrecisionAtK,
      measureK = 10
    ) 
  
    Workflow.runEngine(
      params = WorkflowParams(batch = "MLC: ItemRec Evaluation1", verbose = 0),
      engine = engine,
      engineParams = engineParams,
      metricsClassOpt = Some(classOf[ItemRecMetrics]),
      metricsParams = metricsParams
    )
  }
}

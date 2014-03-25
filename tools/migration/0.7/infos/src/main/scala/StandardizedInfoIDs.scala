package io.prediction.tools.migration

import io.prediction.commons.Config

import grizzled.slf4j.Logger

object StandardizedInfoIDs {
  def main(args: Array[String]) {
    val logger = Logger(StandardizedInfoIDs.getClass)
    val config = new Config()
    val algos = config.getSettingsAlgos()
    val algoOldToNew = Map[String, String](
      "pdio-local-itemrec-random" -> "pio-itemrec-single-random",
      "pdio-local-itemsim-random" -> "pio-itemsim-single-random",
      "pdio-randomrank" -> "pio-itemrec-distributed-random",
      "pdio-latestrank" -> "pio-itemrec-distributed-latest",
      "mahout-itembased" -> "pio-itemrec-distributed-mahout-itembased",
      "mahout-parallelals" -> "pio-itemrec-distributed-mahout-parallelals",
      "mahout-knnitembased" -> "pio-itemrec-single-mahout-knnitembased",
      "mahout-knnuserbased" -> "pio-itemrec-single-mahout-knnuserbased",
      "mahout-thresholduserbased" -> "pio-itemrec-single-mahout-thresholduserbased",
      "mahout-alswr" -> "pio-itemrec-single-mahout-alswr",
      "mahout-svdsgd" -> "pio-itemrec-single-mahout-svdsgd",
      "mahout-svdplusplus" -> "pio-itemrec-single-mahout-svdplusplus",
      "pdio-itemsimrandomrank" -> "pio-itemsim-distributed-random",
      "pdio-itemsimlatestrank" -> "pio-itemsim-distributed-latest",
      "mahout-itemsimcf-single" -> "pio-itemsim-single-mahout-itemsimcf",
      "mahout-itemsimcf" -> "pio-itemsim-distributed-mahout-itemsimcf",
      "graphchi-als" -> "pio-itemrec-single-graphchi-als",
      "graphchi-climf" -> "pio-itemrec-single-graphchi-climf")
    algos.getAll foreach { algo =>
      val newAlgoInfoID = algoOldToNew.get(algo.infoid).getOrElse(algo.infoid)
      logger.info(s"Algo ID ${algo.id}: ${algo.infoid} -> ${newAlgoInfoID}")
      algos.update(algo.copy(infoid = newAlgoInfoID))
    }

    val offlineEvalSplitters = config.getSettingsOfflineEvalSplitters()
    val splittersOldToNew = Map[String, String](
      "trainingtestsplit" -> "pio-distributed-trainingtestsplit",
      "u2isplit" -> "pio-single-trainingtestsplit")
    offlineEvalSplitters.getAll foreach { splitter =>
      val newSplitterInfoID = splittersOldToNew.get(splitter.infoid).getOrElse(splitter.infoid)
      logger.info(s"OfflineEvalSplitter ID ${splitter.id}: ${splitter.infoid} -> ${newSplitterInfoID}")
      offlineEvalSplitters.update(splitter.copy(infoid = newSplitterInfoID))
    }

    val offlineEvalMetrics = config.getSettingsOfflineEvalMetrics()
    val metricsOldToNew = Map[String, String](
      "map_k" -> "pio-itemrec-distributed-map_k",
      "map_k_nd" -> "pio-itemrec-single-map_k",
      "ismap_k" -> "pio-itemsim-distributed-ismap_k",
      "ismap_k_nd" -> "pio-itemsim-single-ismap_k")
    offlineEvalMetrics.getAll foreach { metric =>
      val newMetricInfoID = metricsOldToNew.get(metric.infoid).getOrElse(metric.infoid)
      logger.info(s"OfflineEvalMetric ID ${metric.id}: ${metric.infoid} -> ${newMetricInfoID}")
      offlineEvalMetrics.update(metric.copy(infoid = newMetricInfoID))
    }

    val paramGens = config.getSettingsParamGens()
    val paramGensOldToNew = Map[String, String](
      "random" -> "pio-single-random")
    paramGens.getAll foreach { paramGen =>
      val newParamGenInfoID = paramGensOldToNew.get(paramGen.infoid).getOrElse(paramGen.infoid)
      logger.info(s"ParamGen ID ${paramGen.id}: ${paramGen.infoid} -> ${newParamGenInfoID}")
      paramGens.update(paramGen.copy(infoid = newParamGenInfoID))
    }
  }
}

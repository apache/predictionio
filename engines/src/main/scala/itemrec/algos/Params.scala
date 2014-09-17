package io.prediction.engines.java.itemrec.algos

// This is a temporary hack. The deployment module requires all components to
// use a single language, in order to make it possible to convert json objects
// into parameter classes. Java classes use GSON library; Scala classes use
// json4s library. Hence, we create some fake parameter classes but name them
// under java package to cheat the deploymenet module.

import io.prediction.controller.Params

class GenericItemBasedParams(
  numRecommendations: Int,
  val itemSimilarity: String,
  val weighted: Boolean) extends MahoutParams(numRecommendations) {

  def this(numRecommendations: Int) = 
    this(numRecommendations, GenericItemBased.LOG_LIKELIHOOD, false)
}

class SVDPlusPlusParams(
  numRecommendations: Int,
  val numFeatures: Int,
  val learningRate: Double,
  val preventOverfitting: Double,
  val randomNoise: Double,
  val numIterations: Int,
  val learningRateDecay: Double) extends MahoutParams(numRecommendations) {

  def this(numRecommendations: Int) = 
    this(numRecommendations, 3, 0.01, 0.1, 0.01, 3, 1)
}

package io.prediction.examples.itemrank

import io.prediction.controller.Params

class MahoutItemBasedAlgoParams(
  val booleanData: Boolean,
  val itemSimilarity: String,
  val weighted: Boolean,
  val nearestN: Int,
  val threshold: Double,
  val numSimilarItems: Int
) extends Params {}

class MahoutItemBasedModel(
  val userHistory: Map[String, Set[(String, Int)]],
  val itemSim: Map[String, Seq[(String, Double)]]
) extends Serializable {}

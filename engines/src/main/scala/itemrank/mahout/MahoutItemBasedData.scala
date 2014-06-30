package io.prediction.engines.itemrank

import io.prediction.BaseParams

class MahoutItemBasedCleanserParam(
) extends BaseParams {}
/*
class MoahoutItemBasedCleansedData(
  val ratingsFile: String, // file path
  val users: Map[Int, String], // uindex->uid
  val items: Map[Int, ItemTD] // iindex->itemTD
  val validItems: Set[Int] // valid candidate items index
) extends BaseParams {}
*/

class MahoutItemBasedAlgoParams(
  val booleanData: Boolean,
  val itemSimilarity: String,
  val weighted: Boolean,
  val nearestN: Int,
  val threshold: Double,
  val numSimilarItems: Int
) extends BaseParams {}


class MahoutItemBasedModel(
  val userHistory: Map[String, Set[(String, Int)]],
  val itemSim: Map[String, Seq[(String, Double)]]
) extends Serializable {}

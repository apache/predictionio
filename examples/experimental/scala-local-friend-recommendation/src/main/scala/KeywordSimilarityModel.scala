package io.prediction.examples.friendrecommendation

import scala.collection.immutable.HashMap

case class KeywordSimilarityModel (
  // External-internal id map
  userIdMap: HashMap[Int, Int],
  itemIdMap: HashMap[Int, Int],
  // Keyword array, internal id index, term-weight map item
  userKeyword: Array[HashMap[Int, Double]],
  itemKeyword: Array[HashMap[Int, Double]],
  // Weight and threshold trained
  keywordSimWeight: Double,
  keywordSimThreshold: Double
)

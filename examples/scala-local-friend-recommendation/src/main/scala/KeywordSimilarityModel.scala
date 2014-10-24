package io.prediction.examples.friendrecommendation

import scala.collection.immutable.HashMap

class KeywordSimilarityModel (
  val userKeyword: HashMap[Int, HashMap[Int, Double]],
  val itemKeyword: HashMap[Int, HashMap[Int, Double]]
) extends Serializable

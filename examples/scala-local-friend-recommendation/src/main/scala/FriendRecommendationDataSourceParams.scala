package io.prediction.examples.friendrecommendation

import io.prediction.controller._

class FriendRecommendationDataSourceParams(
  val itemFilePath: String,
  val userKeywordFilePath: String,
  val userActionFilePath: String,
  val trainingRecordFilePath: String
) extends Params

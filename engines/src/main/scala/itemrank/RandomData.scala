package io.prediction.engines.itemrank

import io.prediction.{
  BaseModel,
  BaseAlgoParams
}

class RandomAlgoParams() extends BaseAlgoParams {
  override def toString = s"empty"
}

class RandomModel() extends BaseModel {}

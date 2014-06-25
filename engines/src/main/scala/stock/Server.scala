package io.prediction.engines.stock

import io.prediction.BaseParams
import io.prediction.Server

class StockServerParams(val i: Int = 0) extends BaseParams {
  override def toString = s"StockerServerParams(i=$i)"
}

class StockServer 
    extends Server[Feature, Target, StockServerParams] {
  var _i: Int = -1
  override def init(params: StockServerParams): Unit = { _i = params.i }

  def combine(feature: Feature, targets: Seq[Target]): Target = targets(_i)    
}

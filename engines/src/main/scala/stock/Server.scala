package io.prediction.engines.stock

import io.prediction.BaseServerParams
import io.prediction.Server

class StockServerParams(val i: Int = 0) extends BaseServerParams {}

class StockServer 
    extends Server[Feature, Target, StockServerParams] {
  var _i: Int = -1
  override def init(params: StockServerParams): Unit = { _i = params.i }

  def combine(feature: Feature, targets: Seq[Target]): Target = targets(_i)    
}

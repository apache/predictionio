package io.prediction.commons.settings

trait Info {
  def params: Map[String, Param]
  def paramsections: Seq[ParamSection]
}

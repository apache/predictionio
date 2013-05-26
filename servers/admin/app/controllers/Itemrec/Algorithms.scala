package controllers.Itemrec

import io.prediction.commons.settings.AlgoInfo

import play.api._
import play.api.mvc._

object Algorithms extends Controller {

  def displayParams(algoInfo: AlgoInfo, params: Map[String, Any]): String = {
    // return default value if the param doesn't exist in algo's params field
    // (eg. new param added later).
    algoInfo.name + ": " + (algoInfo.paramorder map { paramName => algoInfo.params(paramName) + " = " +
      params.getOrElse(paramName, algoInfo.params(paramName).defaultvalue) } mkString(", "))
  }

}
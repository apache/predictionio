package controllers.Itemrec

import io.prediction.commons.settings.AlgoInfo

import play.api._
import play.api.mvc._

object Algorithms extends Controller {

  def displayParams(algoInfo: AlgoInfo, params: Map[String, Any]): String = {
    algoInfo.paramorder map { param => algoInfo.paramdescription(param)._1 + " = " + params(param) } mkString(", ")
  }

}
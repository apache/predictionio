package controllers.Itemrec

import io.prediction.commons.settings.AlgoInfo

import play.api._
import play.api.mvc._

object Algorithms extends Controller {

  def getDefaultParams(algoInfo: AlgoInfo): Map[String, Any] = {
    algoInfo.defaultparams map { case ((display, param, visible), value) => (param -> value) } toMap
  }

  def displayVisibleParams(algoInfo: AlgoInfo, params: Map[String, Any]): String = {
    algoInfo.defaultparams filter { case ((display, param, visible), value) => (visible == true) } map {
      case ((display, param, visible), value) =>
        display + " = " + params(param)
    } mkString(", ")
  }

}
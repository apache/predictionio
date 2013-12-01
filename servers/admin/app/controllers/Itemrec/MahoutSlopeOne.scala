package controllers.Itemrec

//import io.prediction.commons.settings.Algo

import play.api._
import play.api.mvc._
import play.api.data._
//import play.api.data.Forms.{tuple, number, text, list, boolean, nonEmptyText}
import play.api.libs.json.Json._
import play.api.libs.json._
// you need this import to have combinators
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError

//import controllers.Application.{algos, withUser, algoInfos}

object MahoutSlopeOne extends GenericAlgoSetting {

  // aggregate all data into one class
  case class AllData(
      info: GenericInfo,
      actionParam: GenericActionParam,
      weighting: String) extends AlgoData {

    override def getParams: Map[String, Any] = {
      paramToMap(actionParam) ++ Map("weighting" -> weighting)
    }

    override def getAlgoid: Int = info.id
  }

  implicit val allDataReads = (
    JsPath.read[GenericInfo] and
    JsPath.read[GenericActionParam] and
    (JsPath \ "weighting").read[String]
  )(AllData)

  def updateSettings(appid: String, engineid: String, algoid: String) = updateGenericSettings[AllData](appid, engineid, algoid)(allDataReads)

}
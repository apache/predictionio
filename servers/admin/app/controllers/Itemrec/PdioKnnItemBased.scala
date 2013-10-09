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

object PdioKnnItemBased extends GenericAlgoSetting {
  
  case class Param(
    measureParam: String,
    priorCountParam: Int,
    priorCorrelParam: Double,
    minNumRatersParam: Int,
    maxNumRatersParam: Int,
    minIntersectionParam: Int,
    minNumRatedSimParam: Int
  )

  implicit val paramReads = (
    (JsPath \ "measureParam").read[String] and
    (JsPath \ "priorCountParam").read[Int](Reads.min(0)) and
    (JsPath \ "priorCorrelParam").read[Double] and
    (JsPath \ "minNumRatersParam").read[Int](Reads.min(1)) and
    (JsPath \ "maxNumRatersParam").read[Int](Reads.min(1)) and
    (JsPath \ "minIntersectionParam").read[Int](Reads.min(1)) and
    (JsPath \ "minNumRatedSimParam").read[Int](Reads.min(1))
  )(Param)

  case class AutoTuneParam (
    priorCountParamMin: Int,
    priorCountParamMax: Int,
    priorCorrelParamMin: Double,
    priorCorrelParamMax: Double,
    minNumRatersParamMin: Int,
    minNumRatersParamMax: Int,
    maxNumRatersParamMin: Int,
    maxNumRatersParamMax: Int,
    minIntersectionParamMin: Int,
    minIntersectionParamMax: Int,
    minNumRatedSimParamMin: Int,
    minNumRatedSimParamMax: Int
  )

  implicit val autoTuneParamReads = (
    (JsPath \ "priorCountParamMin").read[Int](Reads.min(0)) and
    (JsPath \ "priorCountParamMax").read[Int](Reads.min(0)) and
    (JsPath \ "priorCorrelParamMin").read[Double] and
    (JsPath \ "priorCorrelParamMax").read[Double] and
    (JsPath \ "minNumRatersParamMin").read[Int](Reads.min(1)) and
    (JsPath \ "minNumRatersParamMax").read[Int](Reads.min(1)) and
    (JsPath \ "maxNumRatersParamMin").read[Int](Reads.min(1)) and
    (JsPath \ "maxNumRatersParamMax").read[Int](Reads.min(1)) and
    (JsPath \ "minIntersectionParamMin").read[Int](Reads.min(1)) and
    (JsPath \ "minIntersectionParamMax").read[Int](Reads.min(1)) and
    (JsPath \ "minNumRatedSimParamMin").read[Int](Reads.min(1)) and
    (JsPath \ "minNumRatedSimParamMax").read[Int](Reads.min(1))
  )(AutoTuneParam)

  // aggregate all data into one class
  case class AllData(
    info: GenericInfo,
    tune: GenericTune,
    actionParam: GenericActionParam,
    param: Param,
    autoTuneParam: AutoTuneParam
  ) extends AlgoData {

    override def getParams: Map[String, Any] = {
      paramToMap(tune) ++ paramToMap(actionParam) ++ paramToMap(param) ++ paramToMap(autoTuneParam)
    }

    override def getAlgoid: Int = info.id
  }

  implicit val allDataReads = (
    JsPath.read[GenericInfo] and
    JsPath.read[GenericTune] and
    JsPath.read[GenericActionParam] and
    JsPath.read[Param] and
    JsPath.read[AutoTuneParam]
  )(AllData)

  def updateSettings(appid:String, engineid:String, algoid:String) = updateGenericSettings[AllData](appid, engineid, algoid)(allDataReads)

}
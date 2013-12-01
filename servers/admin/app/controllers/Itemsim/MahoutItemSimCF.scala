package controllers.Itemsim

import io.prediction.commons.settings.Algo

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

object MahoutItemSimCF extends GenericAlgoSetting {

  case class Param(
    similarityClassname: String,
    threshold: Double,
    booleanData: Boolean,
    maxPrefsPerUser: Int, // min 1
    minPrefsPerUser: Int // min 1
    )

  implicit val paramReads = (
    (JsPath \ "similarityClassname").read[String] and
    (JsPath \ "threshold").read[Double] and
    (JsPath \ "booleanData").read[Boolean] and
    (JsPath \ "maxPrefsPerUser").read[Int](Reads.min(1)) and
    (JsPath \ "minPrefsPerUser").read[Int](Reads.min(1))
  )(Param)

  case class AutoTuneParam(
    thresholdMin: Double,
    thresholdMax: Double,
    maxPrefsPerUserMin: Int, // min 1
    maxPrefsPerUserMax: Int, // min 1
    minPrefsPerUserMin: Int, // min 1
    minPrefsPerUserMax: Int // min 1
    )

  implicit val autoTuneParamReads = (
    (JsPath \ "thresholdMin").read[Double] and
    (JsPath \ "thresholdMax").read[Double] and
    (JsPath \ "maxPrefsPerUserMin").read[Int](Reads.min(1)) and
    (JsPath \ "maxPrefsPerUserMax").read[Int](Reads.min(1)) and
    (JsPath \ "minPrefsPerUserMin").read[Int](Reads.min(1)) and
    (JsPath \ "minPrefsPerUserMax").read[Int](Reads.min(1))
  )(AutoTuneParam)

  // aggregate all data into one class
  case class AllData(
      info: GenericInfo,
      tune: GenericTune,
      actionParam: GenericActionParam,
      param: Param,
      autoTuneParam: AutoTuneParam) extends AlgoData {

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

  def updateSettings(appid: String, engineid: String, algoid: String) = updateGenericSettings[AllData](appid, engineid, algoid)(allDataReads)

}
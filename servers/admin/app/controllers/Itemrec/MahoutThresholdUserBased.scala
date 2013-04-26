package controllers.Itemrec

///import io.prediction.commons.settings.Algo

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

object MahoutThresholdUserBased extends GenericAlgoSetting {
  
  case class Param(
    userSimilarity: String,
    threshold: Double,
    booleanData: Boolean,
    weighted: Boolean,
    samplingRate: Double
  )

  def validSamplingRate(implicit r: Reads[Double]): Reads[Double] = 
    ( Reads.filter(ValidationError("Must be > 0 and <= 1."))(x => (x > 0.0) && (x <= 1.0)) ) 

  implicit val paramReads = (
    (JsPath \ "userSimilarity").read[String] and
    (JsPath \ "threshold").read[Double] and
    (JsPath \ "booleanData").read[Boolean] and
    (JsPath \ "weighted").read[Boolean] and
    (JsPath \ "samplingRate").read[Double](validSamplingRate)
  )(Param)

  case class AutoTuneParam(
    thresholdMin: Double,
    thresholdMax: Double,
    samplingRateMin: Double,
    samplingRateMax: Double
  )

  implicit val autoTuneParamReads = (
    (JsPath \ "thresholdMin").read[Double] and
    (JsPath \ "thresholdMax").read[Double] and
    (JsPath \ "samplingRateMin").read[Double](validSamplingRate) and
    (JsPath \ "samplingRateMax").read[Double](validSamplingRate)
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

  def updateSettings(app_id:String, engine_id:String, algo_id:String) = updateGenericSettings[AllData](app_id, engine_id, algo_id)(allDataReads)

}
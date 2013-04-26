package controllers.Itemrec

import io.prediction.commons.settings.Algo

import play.api._
import play.api.mvc._
import play.api.data._
//import play.api.data.Forms.{tuple, number, text, list, boolean, nonEmptyText}
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError

//import controllers.Application.{algos, withUser, algoInfos}

object MahoutKnnUserBased extends AlgoSetting {
  
  case class Param(
    userSimilarity: String,
    nearestN: Int,
    booleanData: Boolean,
    minSimilarity: Double,
    weighted: Boolean,
    samplingRate: Double,
    //
    viewParam: String,
    likeParam: String,
    dislikeParam: String,
    conversionParam: String,
    conflictParam: String,
    //
    tune: String, // auto or manual
    tuneMethod: String // random
  )

  def validSamplingRate(implicit r: Reads[Double]): Reads[Double] = 
    ( Reads.filter(ValidationError("Must be > 0 and <= 1."))(x => (x > 0.0) && (x <= 1.0)) ) 

  implicit val paramReads = (
    (JsPath \ "userSimilarity").read[String] and
    (JsPath \ "nearestN").read[Int](Reads.min(1)) and
    (JsPath \ "booleanData").read[Boolean] and
    (JsPath \ "minSimilarity").read[Double] and
    (JsPath \ "weighted").read[Boolean] and
    (JsPath \ "samplingRate").read[Double](validSamplingRate) and
    //
    (JsPath \ "viewParam").read[String](validAction) and
    (JsPath \ "likeParam").read[String](validAction) and
    (JsPath \ "dislikeParam").read[String](validAction) and
    (JsPath \ "conversionParam").read[String](validAction) and
    (JsPath \ "conflictParam").read[String](validConflict) and
    //
    (JsPath \ "tune").read[String](validTune) and
    (JsPath \ "tuneMethod").read[String](validDataIn(List("random"))) 
  )(Param)

  case class AutoTuneParam(
    nearestNMin: Int,
    nearestNMax: Int,
    minSimilarityMin: Double,
    minSimilarityMax: Double,
    samplingRateMin: Double,
    samplingRateMax: Double
  )

  implicit val autoTuneParamReads = (
    (JsPath \ "nearestNMin").read[Int](Reads.min(1)) and
    (JsPath \ "nearestNMax").read[Int](Reads.min(1)) and
    (JsPath \ "minSimilarityMin").read[Double] and
    (JsPath \ "minSimilarityMax").read[Double] and
    (JsPath \ "samplingRateMin").read[Double](validSamplingRate) and
    (JsPath \ "samplingRateMax").read[Double](validSamplingRate)
  )(AutoTuneParam)

  // aggregate all data into one class
  case class AllData(
    info: Info, // from AlgoSetting
    param: Param,
    autoTuneParam: AutoTuneParam
  ) extends AlgoData {

    override def getParams: Map[String, Any] = {
      caseClassToMap(param) ++ caseClassToMap(autoTuneParam)
    }

    override def getAlgoid: Int = info.id
  }

  implicit val allDataReads = (
    JsPath.read[Info] and
    JsPath.read[Param] and
    JsPath.read[AutoTuneParam]
  )(AllData)

  def updateSettings(app_id:String, engine_id:String, algo_id:String) = updateGenericSettings[AllData](app_id, engine_id, algo_id)(allDataReads)

}
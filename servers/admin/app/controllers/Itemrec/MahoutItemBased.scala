package controllers.Itemrec

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

object MahoutItemBased extends AlgoSetting {
   
  case class Param (
    similarityClassname: String,
    threshold: Double,
    booleanData: Boolean,
    maxPrefsPerUser: Int, // min 1
    minPrefsPerUser: Int, // min 1 
    maxSimilaritiesPerItem: Int, // min 1
    maxPrefsPerUserInItemSimilarity: Int, // min 1
    //
    viewParam: String, // 1 - 5 or 'ignore'
    likeParam: String,
    dislikeParam: String,
    conversionParam: String,
    conflictParam: String, // latest, highest or lowest
    //
    tune: String, // auto or manual
    tuneMethod: String // random
  )
  
  implicit val paramReads = (
    (JsPath \ "similarityClassname").read[String] and
    (JsPath \ "threshold").read[Double] and
    (JsPath \ "booleanData").read[Boolean] and
    (JsPath \ "maxPrefsPerUser").read[Int](Reads.min(1)) and
    (JsPath \ "minPrefsPerUser").read[Int](Reads.min(1)) and
    (JsPath \ "maxSimilaritiesPerItem").read[Int](Reads.min(1)) and
    (JsPath \ "maxPrefsPerUserInItemSimilarity").read[Int](Reads.min(1)) and
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
    thresholdMin: Double,
    thresholdMax: Double,
    maxPrefsPerUserMin: Int, // min 1
    maxPrefsPerUserMax: Int, // min 1
    minPrefsPerUserMin: Int, // min 1
    minPrefsPerUserMax: Int, // min 1
    maxSimilaritiesPerItemMin: Int,
    maxSimilaritiesPerItemMax: Int,
    maxPrefsPerUserInItemSimilarityMin: Int,
    maxPrefsPerUserInItemSimilarityMax: Int
  )

  implicit val autoTuneParamReads = (
    (JsPath \ "thresholdMin").read[Double] and
    (JsPath \ "thresholdMax").read[Double] and
    (JsPath \ "maxPrefsPerUserMin").read[Int](Reads.min(1)) and
    (JsPath \ "maxPrefsPerUserMax").read[Int](Reads.min(1)) and
    (JsPath \ "minPrefsPerUserMin").read[Int](Reads.min(1)) and
    (JsPath \ "minPrefsPerUserMax").read[Int](Reads.min(1)) and
    (JsPath \ "maxSimilaritiesPerItemMin").read[Int](Reads.min(1)) and
    (JsPath \ "maxSimilaritiesPerItemMax").read[Int](Reads.min(1)) and
    (JsPath \ "maxPrefsPerUserInItemSimilarityMin").read[Int](Reads.min(1)) and
    (JsPath \ "maxPrefsPerUserInItemSimilarityMax").read[Int](Reads.min(1))
  )(AutoTuneParam)

  // aggregate all data into one class
  case class AllData(
    info: Info,
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
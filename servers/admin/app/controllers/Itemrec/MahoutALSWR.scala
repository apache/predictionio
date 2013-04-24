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

object MahoutALSWR extends AlgoSetting {
   
  case class Param (
    numFeatures: Int, // min 1
    lambda: Double, // min 0
    numIterations: Int, // min 1
    viewParam: String, // 1 - 5 or 'ignore'
    likeParam: String,
    dislikeParam: String,
    conversionParam: String,
    conflictParam: String, // latest, highest or lowest
    tune: String, // auto or manual
    tuneMethod: String // random
  )

  implicit val paramReads = (
    (JsPath \ "numFeatures").read[Int](Reads.min(1)) and
    (JsPath \ "lambda").read[Double](minDouble(0)) and
    (JsPath \ "numIterations").read[Int](Reads.min(1)) and
    (JsPath \ "viewParam").read[String](validAction) and
    (JsPath \ "likeParam").read[String](validAction) and
    (JsPath \ "dislikeParam").read[String](validAction) and
    (JsPath \ "conversionParam").read[String](validAction) and
    (JsPath \ "conflictParam").read[String](validConflict) and
    (JsPath \ "tune").read[String](validTune) and
    (JsPath \ "tuneMethod").read[String](validDataIn(List("random"))) 
  )(Param)

  case class AutoTuneParam(
    numFeaturesMin: Int,
    numFeaturesMax: Int,
    lambdaMin: Double,
    lambdaMax: Double,
    numIterationsMin: Int,
    numIterationsMax: Int
  )

  implicit val autoTuneParamReads = (
    (JsPath \ "numFeaturesMin").read[Int](Reads.min(1)) and
    (JsPath \ "numFeaturesMax").read[Int](Reads.min(1)) and
    (JsPath \ "lambdaMin").read[Double](minDouble(0)) and
    (JsPath \ "lambdaMax").read[Double](minDouble(0)) and
    (JsPath \ "numIterationsMin").read[Int](Reads.min(1)) and
    (JsPath \ "numIterationsMax").read[Int](Reads.min(1))
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
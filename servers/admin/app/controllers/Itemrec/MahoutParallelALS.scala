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

object MahoutParallelALS extends GenericAlgoSetting {
  
  case class Param (
    lambda: Double,
    implicitFeedback: Boolean,
    alpha: Double,
    numFeatures: Int,
    numIterations: Int
  )

  implicit val paramReads = (
    (JsPath \ "lambda").read[Double](minDouble(0)) and
    (JsPath \ "implicitFeedback").read[Boolean] and
    (JsPath \ "alpha").read[Double] and
    (JsPath \ "numFeatures").read[Int](Reads.min(1)) and
    (JsPath \ "numIterations").read[Int](Reads.min(1))
  )(Param)

  case class AutoTuneParam(
    lambdaMin: Double,
    lambdaMax: Double,
    alphaMin: Double,
    alphaMax: Double,
    numFeaturesMin: Int,
    numFeaturesMax: Int,
    numIterationsMin: Int,
    numIterationsMax: Int
  )

  implicit val autoTuneParamReads = (
    (JsPath \ "lambdaMin").read[Double](minDouble(0)) and
    (JsPath \ "lambdaMax").read[Double](minDouble(0)) and
    (JsPath \ "alphaMin").read[Double] and
    (JsPath \ "alphaMax").read[Double] and
    (JsPath \ "numFeaturesMin").read[Int](Reads.min(1)) and
    (JsPath \ "numFeaturesMax").read[Int](Reads.min(1)) and
    (JsPath \ "numIterationsMin").read[Int](Reads.min(1)) and
    (JsPath \ "numIterationsMax").read[Int](Reads.min(1))
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
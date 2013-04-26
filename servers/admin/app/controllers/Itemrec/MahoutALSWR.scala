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

object MahoutALSWR extends GenericAlgoSetting {
   
  case class Param (
    numFeatures: Int, // min 1
    lambda: Double, // min 0
    numIterations: Int // min 1
  )

  implicit val paramReads = (
    (JsPath \ "numFeatures").read[Int](Reads.min(1)) and
    (JsPath \ "lambda").read[Double](minDouble(0)) and
    (JsPath \ "numIterations").read[Int](Reads.min(1))
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
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

object MahoutSVDPlusPlus extends GenericAlgoSetting {

  case class Param(
    numFeatures: Int,
    learningRate: Double,
    preventOverfitting: Double,
    randomNoise: Double,
    numIterations: Int,
    learningRateDecay: Double)

  implicit val paramReads = (
    (JsPath \ "numFeatures").read[Int](Reads.min(1)) and
    (JsPath \ "learningRate").read[Double](minDouble(Double.MinPositiveValue)) and
    (JsPath \ "preventOverfitting").read[Double](minDouble(Double.MinPositiveValue)) and
    (JsPath \ "randomNoise").read[Double](minDouble(0)) and
    (JsPath \ "numIterations").read[Int](Reads.min(1)) and
    (JsPath \ "learningRateDecay").read[Double](minDouble(Double.MinPositiveValue))
  )(Param)

  case class AutoTuneParam(
    numFeaturesMin: Int,
    numFeaturesMax: Int,
    learningRateMin: Double,
    learningRateMax: Double,
    preventOverfittingMin: Double,
    preventOverfittingMax: Double,
    randomNoiseMin: Double,
    randomNoiseMax: Double,
    numIterationsMin: Int,
    numIterationsMax: Int,
    learningRateDecayMin: Double,
    learningRateDecayMax: Double)

  implicit val autoTuneParamReads = (
    (JsPath \ "numFeaturesMin").read[Int](Reads.min(1)) and
    (JsPath \ "numFeaturesMax").read[Int](Reads.min(1)) and
    (JsPath \ "learningRateMin").read[Double](minDouble(Double.MinPositiveValue)) and
    (JsPath \ "learningRateMax").read[Double](minDouble(Double.MinPositiveValue)) and
    (JsPath \ "preventOverfittingMin").read[Double](minDouble(Double.MinPositiveValue)) and
    (JsPath \ "preventOverfittingMax").read[Double](minDouble(Double.MinPositiveValue)) and
    (JsPath \ "randomNoiseMin").read[Double](minDouble(0)) and
    (JsPath \ "randomNoiseMax").read[Double](minDouble(0)) and
    (JsPath \ "numIterationsMin").read[Int](Reads.min(1)) and
    (JsPath \ "numIterationsMax").read[Int](Reads.min(1)) and
    (JsPath \ "learningRateDecayMin").read[Double](minDouble(Double.MinPositiveValue)) and
    (JsPath \ "learningRateDecayMax").read[Double](minDouble(Double.MinPositiveValue))
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
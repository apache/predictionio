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

import controllers.Application.{algos, withUser, algoInfos}

trait GenericAlgoSetting extends Controller {

  // modified from default Reads for allowing conversion from JsString to Int.
  implicit object IntReads extends Reads[Int] {
    def reads(json: JsValue) = json match {
      case JsString(n) => scala.util.control.Exception.catching(classOf[NumberFormatException])
        .opt( JsSuccess(n.toInt) )
        .getOrElse( JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber")))) )
      case JsNumber(n) => JsSuccess(n.toInt)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }
  }

  // modified from default Reads for allowing conversion from JsString to Double.
  implicit object DoubleReads extends Reads[Double] {
    def reads(json: JsValue) = json match {
      case JsString(n) => scala.util.control.Exception.catching(classOf[NumberFormatException])
        .opt( JsSuccess(n.toDouble) )
        .getOrElse( JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber")))) )
      case JsNumber(n) => JsSuccess(n.toDouble)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber"))))
    }
  }

  // modified from default Reads for allowing conversion from JsString to Boolean.
  implicit object BooleanReads extends Reads[Boolean] {
    def reads(json: JsValue) = json match {
      case JsString(b) => scala.util.control.Exception.catching(classOf[IllegalArgumentException])
        .opt( JsSuccess(b.toBoolean) )
        .getOrElse( JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsboolean")))) )
      case JsBoolean(b) => JsSuccess(b)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsboolean"))))
    }
  }

  def validDataIn(list: List[String])(implicit r: Reads[String]): Reads[String] = 
    r.filter(ValidationError("Must be one of these values: " + list.mkString(", ") +".")) (list.contains(_))

  // verify action to score conversion
  def validAction(implicit r: Reads[String]): Reads[String] = validDataIn(List("1", "2", "3", "4", "5", "ignore"))

  // verify action conflict resolution param
  def validConflict(implicit r: Reads[String]): Reads[String] = validDataIn(List("latest", "highest", "lowest"))

  def minDouble(m: Double)(implicit r: Reads[Double]): Reads[Double] =
    r.filter(ValidationError("Must be greater than or equal to 0.")) ( _ >= m )

  /** common info for all algo */
  case class GenericInfo(
    id: Int,
    appid: Int,
    engineid: Int
  )

  implicit val genericInfoReads = (
    (JsPath \ "id").read[Int] and
    (JsPath \ "app_id").read[Int] and
    (JsPath \ "engine_id").read[Int]
  )(GenericInfo)

  /** generic action conversion param for all algo */
  case class GenericActionParam(
    viewParam: String,
    likeParam: String,
    dislikeParam: String,
    conversionParam: String,
    conflictParam: String
  )

  implicit val genericActionParamReads = (
    (JsPath \ "viewParam").read[String](validAction) and
    (JsPath \ "likeParam").read[String](validAction) and
    (JsPath \ "dislikeParam").read[String](validAction) and
    (JsPath \ "conversionParam").read[String](validAction) and
    (JsPath \ "conflictParam").read[String](validConflict)
  )(GenericActionParam)

  /** generic tune settings */
  case class GenericTune(
    tune: String, // auto or manual
    tuneMethod: String // random
  )

  val validTuneMethods: List[String] = List("random") // can be overriden to support different method for particular algo

  implicit val genericTuneReads = (
    (JsPath \ "tune").read[String](validDataIn(List("auto", "manual"))) and
    (JsPath \ "tuneMethod").read[String](validDataIn(validTuneMethods))
  )(GenericTune)

  /** generic updateSettings for all algo */
  def updateGenericSettings[T <: AlgoData](app_id:String, engine_id:String, algo_id:String)(implicit rds: Reads[T]) = withUser { user => implicit request =>

    request.body.asJson.map { json =>
      //println(json)

      json.validate[T].fold(
        invalid = { e => 
          //println(e.toString)
          //val msg = e(0)._2(0).message + " Update Failed." // extract 1st error message only
          BadRequest(toJson(Map("message" -> toJson(e.toString))))
        },
        valid = { data =>

          // get original Algo first
          val optAlgo: Option[Algo] = algos.get(data.getAlgoid)
          
          optAlgo map { algo =>
            // NOTE: read-modify-write the original param
            val updatedParams = algo.params ++ data.getParams

            //println(updatedParams)

            val updatedAlgo = algo.copy(
              params = updatedParams
            )
            
            algos.update(updatedAlgo)
            Ok
          } getOrElse {
            NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id. Update failed."))))
          }
        }
      )
    }.getOrElse{
      val msg = "Invalid Json data."
      BadRequest(toJson(Map("message" -> toJson(msg))))
    }
  }

  /** common getSettings for all algo
  Return default value if nothing has been set */
  def getSettings(app_id:String, engine_id:String, algo_id:String) = withUser { user => implicit request =>
    
    // TODO: check user owns this app + engine + aglo
    
    // TODO: check algo_id is int
    val optAlgo: Option[Algo] = algos.get(algo_id.toInt)
    
    optAlgo map { algo =>

      algoInfos.get(algo.infoid) map { algoInfo =>

        // get default params from algoinfo and combined with existing params
        val params = algoInfo.paramdefaults ++ algo.params

        Ok(toJson(Map(
          "id" -> toJson(algo.id),
          "app_id" -> toJson(app_id),
          "engine_id" -> toJson(engine_id)
          ) ++ (params map { case (k,v) => (k, toJson(v.toString))})
        ))

      } getOrElse {
        NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id."))))
      }
    } getOrElse {
      NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id."))))
    }
  }

}

/** store the received json data of the algo */
trait AlgoData {

  /** convert case class to Map */
  def paramToMap(obj: AnyRef): Map[String, Any] = {
    obj.getClass.getDeclaredFields.filterNot( _.isSynthetic ).map { field => 
      field.setAccessible(true)
      (field.getName -> field.get(obj))
    }.toMap
  }

  /** return the params stored in this AlgoData obj */
  def getParams: Map[String, Any]

  /** return the algo id*/
  def getAlgoid: Int

}


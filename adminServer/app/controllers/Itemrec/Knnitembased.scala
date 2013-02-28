package controllers.Itemrec

import io.prediction.commons.settings.Algo

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms.{tuple, number, text, list, boolean, nonEmptyText}
import play.api.libs.json.Json._
import play.api.libs.json._

import controllers.Application.{algos, withUser, algoInfos}

object Knnitembased extends Controller {
  
  def updateSettings(app_id:String, engine_id:String, algo_id:String) = withUser { user => implicit request =>
    /* request payload
     * {"app_id":"app_id1234","engine_id":"engne_id1234","id":"algo_id2","distanceFunc":"consine","viewmoreAction":"4",
     * "override":"latest","viewAction":"3","buyAction":"4","priorCorrelation":"0","dislikeAction":1,"likeAction":"5","virtualCount":"50","dislike":"1"}
     */
    
    val algoSettingForm = Form(tuple(
      "id" -> number, // algoid
      "app_id" -> number,
      "engine_id" -> number,
      "distanceFunc" -> nonEmptyText, // TODO: verifying
      "virtualCount" -> number,
      "priorCorrelation" -> nonEmptyText, // TODO: verifying double?
      "minNumRaters" -> number,
      "maxNumRaters" -> number,
      "minIntersection" -> number,
      "minNumRatedSim" -> number,
      "viewAction" -> nonEmptyText, // TODO: verifying 1 - 5 or text "ignore"
      "viewmoreAction" -> nonEmptyText,
      "likeAction" -> nonEmptyText,
      "dislikeAction" -> nonEmptyText,
      "buyAction" -> nonEmptyText,
      "override" -> nonEmptyText // TODO: verifying
    ))
    
    algoSettingForm.bindFromRequest.fold(
      formWithError => {
        println(formWithError.errors) // TODO: send back more meaningful message
        val msg = formWithError.errors(0).message + " Update Failed." // extract 1st error message only
        BadRequest(toJson(Map("message" -> toJson(msg))))
      },
      formData => {
        val (id, appId, engineId, distanceFunc, virtualCount, priorCorrelation, 
          minNumRaters, maxNumRaters, minIntersection, minNumRatedSim,
          viewAction, viewmoreAction, likeAction, dislikeAction, buyAction, overrideParam) = formData
        
        // get original Algo first
        val optAlgo: Option[Algo] = algos.get(id)
        
        optAlgo map { algo =>
          val updatedAlgo = algo.copy(
            params = algo.params ++ Map("measureParam" -> distanceFunc, // NOTE: read-modify-write!
                "priorCountParam" -> virtualCount,
                "priorCorrelParam" -> priorCorrelation,
                "minNumRatersParam" -> minNumRaters,
                "maxNumRatersParam" -> maxNumRaters,
                "minIntersectionParam" -> minIntersection,
                "minNumRatedSimParam" -> minNumRatedSim,
                "viewParam" -> viewAction,
                "viewmoreParam" -> viewmoreAction,
                "likeParam" -> likeAction,
                "dislikeParam" -> dislikeAction,
                "conversionParam" -> buyAction,
                "conflictParam" -> overrideParam
                )
          )
          
          algos.update(updatedAlgo)
          Ok
        } getOrElse {
          NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id. Update failed."))))
        }   
      }
    )
    
  }
  
  /* Return default value if nothing has been set */
  def getSettings(app_id:String, engine_id:String, algo_id:String) = withUser { user => implicit request =>
    /*
    Ok(toJson(Map(
      "id" -> toJson("algo_id2"), // engine id
      "app_id" -> toJson("app_id1234"),
      "engine_id" -> toJson("engne_id1234"),
      "distanceFunc" -> toJson("cosine"),
      "virtualCount" -> toJson(50),
      "priorCorrelation" -> toJson(0),
      "viewAction" -> toJson(3),
      "viewmoreAction" -> toJson(4),
      "likeAction" -> toJson(5),
      "dislikeAction" -> toJson(1),
      "buyAction" -> toJson(5),
      "override" -> toJson("latest")
    )))
    */
    
    // TODO: check user owns this app + engine + aglo
    
    // TODO: check algo_id is Int
    val optAlgo: Option[Algo] = algos.get(algo_id.toInt)
    
    optAlgo map { algo =>
      
      def algoParamGetOrElse[T](algoObj: Algo, param: String, defaultValue: Any): T = {
        if (algoObj.params.contains(param)) algoObj.params(param).asInstanceOf[T] else defaultValue.asInstanceOf[T]
      }
      
      val defaultParams = algoInfos.get("pdio-knnitembased").get.paramdefaults
      
      val distanceFunc: String = algoParamGetOrElse[String](algo, "measureParam", defaultParams("measureParam"))
      val virtualCount: Int = algoParamGetOrElse[Int](algo, "priorCountParam", defaultParams("priorCountParam"))
      val priorCorrelation: Int = algoParamGetOrElse[Int](algo, "priorCorrelParam", defaultParams("priorCorrelParam"))

      val minNumRaters: Int = algoParamGetOrElse[Int](algo, "minNumRatersParam", defaultParams("minNumRatersParam"))
      val maxNumRaters: Int = algoParamGetOrElse[Int](algo, "maxNumRatersParam", defaultParams("maxNumRatersParam"))
      val minIntersection: Int = algoParamGetOrElse[Int](algo, "minIntersectionParam", defaultParams("minIntersectionParam"))
      val minNumRatedSim: Int = algoParamGetOrElse[Int](algo, "minNumRatedSimParam", defaultParams("minNumRatedSimParam"))

      val viewAction: Int = algoParamGetOrElse[Int](algo, "viewParam", defaultParams("viewParam"))
      val viewmoreAction: Int = algoParamGetOrElse[Int](algo, "viewmoreParam", defaultParams("viewmoreParam"))
      val likeAction: Int = algoParamGetOrElse[Int](algo, "likeParam", defaultParams("likeParam"))
      val dislikeAction: Int = algoParamGetOrElse[Int](algo, "dislikeParam", defaultParams("dislikeParam"))
      val buyAction: Int = algoParamGetOrElse[Int](algo, "conversionParam", defaultParams("conversionParam"))
      val overrideParam: String = algoParamGetOrElse[String](algo, "conflictParam", defaultParams("conflictParam"))
      
      Ok(toJson(Map(
        "id" -> toJson(algo.id),
        "app_id" -> toJson(app_id),
        "engine_id" -> toJson(engine_id),
        "distanceFunc" -> toJson(distanceFunc),
        "virtualCount" -> toJson(virtualCount),
        "priorCorrelation" -> toJson(priorCorrelation),
        "minNumRaters" -> toJson(minNumRaters),
        "maxNumRaters" -> toJson(maxNumRaters),
        "minIntersection" -> toJson(minIntersection),
        "minNumRatedSim" -> toJson(minNumRatedSim),
        "viewAction" -> toJson(viewAction),
        "viewmoreAction" -> toJson(viewmoreAction),
        "likeAction" -> toJson(likeAction),
        "dislikeAction" -> toJson(dislikeAction),
        "buyAction" -> toJson(buyAction),
        "override" -> toJson(overrideParam)
      )))
    } getOrElse {
      NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id."))))
    }
  }
}
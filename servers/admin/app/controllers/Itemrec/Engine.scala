package controllers.Itemrec

import io.prediction.commons.settings._

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms.{tuple, number, text, list, boolean}
import play.api.libs.json.Json._
import play.api.libs.json._

import controllers.Application.{engines, withUser}

object Engine extends Controller {

  val defaultSettings: Map[String, Any] = Map(
    "serendipity" -> 0, 
    "freshness" -> 0, 
    "unseenonly" -> false, 
    "goal" -> "rate3",
    "numRecommendations" -> 500)

  def updateSettings(appid:String, engineid:String) = withUser { user => implicit request =>
    /*
    {"appid":"appid1234","id":"engineid2","goal":"rate3","serendipity":0,"freshness":2,"allitemtypes":true,"itemtypelist":null}

    */

    val supportedGoals: List[String] = List("view", "viewmore", "buy", "like", "rate3", "rate4", "rate5")

    val engineSettingForm = Form(tuple(
      "appid" -> number,
      "id" -> number,
      "goal" -> (text verifying("Invalid recommendation goal.", goal => supportedGoals.contains(goal))),
      "serendipity" -> number(min=0, max=10),
      "freshness" -> number(min=0, max=10),
      "allitemtypes" -> boolean,
      "itemtypelist" -> list(text), // TODO verifying
      "unseenonly" -> boolean,
      "numRecommendations" -> number
    ))

    // TODO: check this user owns this appid

    // TODO: check appid, engineid is Int

    engineSettingForm.bindFromRequest.fold(
      formWithError => {
        println(formWithError.errors) // TODO: send back more meaningful message
        val msg = formWithError.errors(0).message + " Update Failed." // extract 1st error message only
        BadRequest(toJson(Map("message" -> toJson(msg))))
      },
      formData => {
        val (appid, id, goal, serendipity, freshness, allitemtypes, itemtypelist, unseenonly, numRecommendations) = formData

        // get original engine first
        val engine = engines.get(id.toInt)

        engine map { eng: Engine =>
          val updatedEngine = eng.copy(
            itypes = (if (itemtypelist.isEmpty) None else Option(itemtypelist)),
            settings = eng.settings ++ Map(
              "serendipity" -> serendipity,
              "freshness" -> freshness,
              "goal" -> goal,
              "unseenonly" -> unseenonly,
              "numRecommendations" -> numRecommendations)
          )

          engines.update(updatedEngine)
          Ok

        } getOrElse {
          NotFound(toJson(Map("message" -> toJson("Engine not found. Update failed"))))
        }

      }
    )

  }

  /* Return default value if nothing has been set */
  /**
   * Ok(toJson(Map(
   *   "id" -> toJson("engineid2"), // engine id
   *   "appid" -> toJson("appid1234"),
   *   "allitemtypes" -> toJson(true),
   *   "itemtypelist" ->  JsNull, // toJson(Seq("book", "movie")),
   *   "freshness" -> toJson(0),
   *   "serendipity" -> toJson(0),
   *   "goal" -> toJson("rate3")
   * )))
   */
  def getSettings(appid:String, engineid:String) = withUser { user => implicit request =>
    // TODO: check this user owns this appid

    // TODO: check engineid and appid is int
    val engine = engines.get(engineid.toInt)

    engine map { eng: Engine =>
/*
      val freshness: Int = if (eng.settings.contains("freshness")) eng.settings("freshness").asInstanceOf[Int] else defaultSettings("freshness").asInstanceOf[Int]
      val serendipity: Int = if (eng.settings.contains("serendipity")) eng.settings("serendipity").asInstanceOf[Int] else defaultSettings("serendipity").asInstanceOf[Int]
      val unseenonly: Boolean = if (eng.settings.contains("unseenonly")) eng.settings("unseenonly").asInstanceOf[Boolean] else defaultSettings("unseenonly").asInstanceOf[Boolean]
      val numRecommendations: Int = if (eng.settings.contains("numRecommendations")) eng.settings("numRecommendations").asInstanceOf[Int] else defaultSettings("numRecommendations").asInstanceOf[Int]
      val goal: String = if (eng.settings.contains("goal")) eng.settings("goal").asInstanceOf[String] else defaultSettings("goal").asInstanceOf[String]
*/
      // note: the order matters here, use eng.settings to override defaultSettings
      val settings = defaultSettings ++ eng.settings

      Ok(toJson(Map(
        "id" -> toJson(eng.id), // engine id
        "appid" -> toJson(eng.appid),
        "allitemtypes" -> toJson(eng.itypes == None),
        "itemtypelist" -> eng.itypes.map(x => toJson(x.toIterator.toSeq)).getOrElse(JsNull), // TODO: better way?
        "freshness" -> toJson(settings("freshness").toString),
        "serendipity" -> toJson(settings("serendipity").toString),
        "unseenonly" -> toJson(settings("unseenonly").toString),
        "goal" -> toJson(settings("goal").toString),
        "numRecommendations" -> toJson(settings("numRecommendations").toString)
        )))
    } getOrElse {
      // if No such app id
      NotFound(toJson(Map("message" -> toJson("Invalid engine id or app id."))))
    }

  }
}
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

  val defaultSettings: Map[String, Any] = Map("serendipity" -> 0, "freshness" -> 0, "unseenonly" -> false, "goal" -> "rate3")

  def updateSettings(app_id:String, engine_id:String) = withUser { user => implicit request =>
    /*
    {"app_id":"appid1234","id":"engineid2","goal":"rate3","serendipity":0,"freshness":2,"allitemtypes":true,"itemtypelist":null}

    */

    val supportedGoals: List[String] = List("view", "viewmore", "buy", "like", "rate3", "rate4", "rate5")

    val engineSettingForm = Form(tuple(
      "app_id" -> number,
      "id" -> number,
      "goal" -> (text verifying("Invalid recommendation goal.", goal => supportedGoals.contains(goal))),
      "serendipity" -> number(min=0, max=10),
      "freshness" -> number(min=0, max=10),
      "allitemtypes" -> boolean,
      "itemtypelist" -> list(text), // TODO verifying
      "unseenonly" -> boolean
    ))

    // TODO: check this user owns this appid

    // TODO: check app_id, engine_id is Int

    engineSettingForm.bindFromRequest.fold(
      formWithError => {
        println(formWithError.errors) // TODO: send back more meaningful message
        val msg = formWithError.errors(0).message + " Update Failed." // extract 1st error message only
        BadRequest(toJson(Map("message" -> toJson(msg))))
      },
      formData => {
        val (app_id, id, goal, serendipity, freshness, allitemtypes, itemtypelist, unseenonly) = formData

        // get original engine first
        val engine = engines.get(id.toInt)

        engine map { eng: Engine =>
          val updatedEngine = eng.copy(
            itypes = (if (itemtypelist.isEmpty) None else Option(itemtypelist)),
            settings = Map("serendipity" -> serendipity, "freshness" -> freshness, "goal" -> goal, "unseenonly" -> unseenonly)
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
   *   "app_id" -> toJson("appid1234"),
   *   "allitemtypes" -> toJson(true),
   *   "itemtypelist" ->  JsNull, // toJson(Seq("book", "movie")),
   *   "freshness" -> toJson(0),
   *   "serendipity" -> toJson(0),
   *   "goal" -> toJson("rate3")
   * )))
   */
  def getSettings(app_id:String, engine_id:String) = withUser { user => implicit request =>
    // TODO: check this user owns this appid

    // TODO: check engine_id and app_id is int
    val engine = engines.get(engine_id.toInt)

    engine map { eng: Engine =>

      val freshness: Int = if (eng.settings.contains("freshness")) eng.settings("freshness").asInstanceOf[Int] else defaultSettings("freshness").asInstanceOf[Int]
      val serendipity: Int = if (eng.settings.contains("serendipity")) eng.settings("serendipity").asInstanceOf[Int] else defaultSettings("serendipity").asInstanceOf[Int]
      val unseenonly: Boolean = if (eng.settings.contains("unseenonly")) eng.settings("unseenonly").asInstanceOf[Boolean] else defaultSettings("unseenonly").asInstanceOf[Boolean]
      val goal: String = if (eng.settings.contains("goal")) eng.settings("goal").asInstanceOf[String] else defaultSettings("goal").asInstanceOf[String]

      Ok(toJson(Map(
        "id" -> toJson(eng.id), // engine id
        "app_id" -> toJson(eng.appid),
        "allitemtypes" -> toJson(eng.itypes == None),
        "itemtypelist" -> eng.itypes.map(x => toJson(x.toIterator.toSeq)).getOrElse(JsNull), // TODO: better way?
        "freshness" -> toJson(freshness),
        "serendipity" -> toJson(serendipity),
        "unseenonly" -> toJson(unseenonly),
        "goal" -> toJson(goal)
        )))
    } getOrElse {
      // if No such app id
      NotFound(toJson(Map("message" -> toJson("Invalid engine id or app id."))))
    }

  }
}
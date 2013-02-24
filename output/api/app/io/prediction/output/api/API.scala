package io.prediction.output.api

import io.prediction.commons._
import io.prediction.commons.appdata.{Item, U2IAction, User}
import io.prediction.commons.settings.{App, Engine}
import io.prediction.output.AlgoOutputSelector

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation._
import play.api.i18n._
import play.api.libs.json._

//import com.codahale.jerkson.Json._
import org.joda.time._
import org.joda.time.format._

object API extends Controller {
  /** Set up commons. */
  val appdataConfig = new appdata.Config()
  val settingsConfig = new settings.Config()

  val apps = settingsConfig.getApps()
  val engines = settingsConfig.getEngines()
  val algos = settingsConfig.getAlgos()

  val users = appdataConfig.getUsers()
  val items = appdataConfig.getItems()
  val u2iActions = appdataConfig.getU2IActions()

  /** Set up output. */
  val algoOutputSelector = new AlgoOutputSelector(algos)

  val notFound = NotFound("Your request is not supported.")

  /** Implicits for JSON conversion. */
  trait APIResponse {
    def status: Int
  }
  case class APIMessageResponse(status: Int, body: Map[String, Any]) extends APIResponse
  case class APIUserResponse(status: Int, user: User) extends APIResponse
  case class APIItemResponse(status: Int, item: Item) extends APIResponse
  case class APIErrors(errors: Seq[Map[String, String]])

  implicit object APIResponseToJson extends Writes[APIResponse] {
    def writes(r: APIResponse) = r match {
      case msg: APIMessageResponse => Json.toJson(msg.asInstanceOf[APIMessageResponse].body.mapValues { anyToJsValue(_) })
      case user: APIUserResponse => Json.toJson(user.asInstanceOf[APIUserResponse].user)
      case item: APIItemResponse => Json.toJson(item.asInstanceOf[APIItemResponse].item)
    }
  }

  implicit object APIErrorsToJson extends Writes[APIErrors] {
    def writes(e: APIErrors) = {
      Json.toJson(e.errors)
    }
  }

  implicit object UserToJson extends Writes[User] {
    def writes(user: User) =
      Json.obj(
        "uid" -> user.id,
        "ct" -> user.ct) ++
        (user.latlng map { l => Json.obj("latlng" -> Json.arr(l._1, l._2)) } getOrElse emptyJsonObj) ++
        (user.inactive map { i => Json.obj("inactive" -> i) } getOrElse emptyJsonObj) ++
        (user.attributes.map { a => Json.obj("attributes" -> Json.toJson(a mapValues { anyToJsValue(_) })) } getOrElse emptyJsonObj)
  }

  implicit object ItemToJson extends Writes[Item] {
    def writes(item: Item) =
      Json.obj(
        "iid" -> item.id,
        "ct" -> item.ct,
        "itypes" -> item.itypes) ++
        (item.starttime map { v => Json.obj("startT" -> v) } getOrElse emptyJsonObj) ++
        (item.endtime map { v => Json.obj("endT" -> v) } getOrElse emptyJsonObj) ++
        (item.price map { v => Json.obj("price" -> v) } getOrElse emptyJsonObj) ++
        (item.profit map { v => Json.obj("profit" -> v) } getOrElse emptyJsonObj) ++
        (item.latlng map { v => Json.obj("latlng" -> latlngToList(v)) } getOrElse emptyJsonObj) ++
        (item.inactive map { v => Json.obj("inactive" -> v) } getOrElse emptyJsonObj) ++
        (item.attributes.map { a => Json.obj("attributes" -> Json.toJson(a mapValues { anyToJsValue(_) })) } getOrElse emptyJsonObj)
  }

  def anyToJsValue(v: Any): JsValue = v match {
    case x: Int => Json.toJson(v.asInstanceOf[Int])
    case x: String => Json.toJson(v.asInstanceOf[String])
    case x: APIErrors => Json.toJson(v.asInstanceOf[APIErrors])
    case _ => JsNull
  }

  /** Control structures used by the API. */
  def FormattedResponse(format: String)(r: APIResponse) = {
    format match {
      case "json" => (new Status(r.status)(Json.stringify(Json.toJson(r)))).as(JSON)
      case _ => notFound
    }
  }

  def AuthenticatedApp(appkey: String)(f: App => APIResponse) = {
    apps.getByAppkey(appkey) map { f(_) } getOrElse APIMessageResponse(FORBIDDEN, Map("message" -> "Invalid appkey."))
  }

  def ValidEngine(enginename: String)(f: Engine => APIResponse)(implicit app: App) = {
    engines.getByAppidAndName(app.id, enginename) map { f(_) } getOrElse APIMessageResponse(
      NOT_FOUND,
      Map(
        "message" -> (enginename + " is not a valid engine. Please make sure it is defined in your app's control panel.")
      )
    )
  }

  /** In order to override default error messages, use Lang("en") for
    * Messages() to enforce framwork to use conf/messages.en because
    * default messages cannot be overridden by simply using conf/messages
    * without specifying a language.
    */
  def bindFailed(loe: Seq[FormError]) = APIMessageResponse(
    BAD_REQUEST,
    Map(
      "errors" -> APIErrors(loe.map(e => Map("field" -> e.key, "message" -> Messages(e.message, e.args: _*)(Lang("en")))))
    )
  )

  /** Form validation constraints. */
  val numeric: Mapping[String] = of[String] verifying Constraints.pattern("""-?\d+(\.\d*)?""".r, "numeric", "Must be a number.")
  val gender: Mapping[String] = of[String] verifying Constraints.pattern("""[MmFf]""".r, "gender", "Must be either 'M' or 'F'.")
  val listOfInts: Mapping[String] = of[String] verifying Constraint[String]("listOfInts") {
    o => {
      try {
        o.split(",").map { _.toInt }
        Valid
      } catch {
        case _ => Invalid(ValidationError("Must be a list of integers separated by commas."))
      }
    }
  }
  val latlngRegex = """-?\d+(\.\d*)?,-?\d+(\.\d*)?""".r
  val latlng: Mapping[String] = of[String] verifying Constraint[String]("latlng", () => latlngRegex) {
    o => latlngRegex.unapplySeq(o).map(_ => {
      val coord = o.split(",") map { _.toDouble }
      if (coord(0) >= -90 && coord(0) < 90 && coord(1) >= -180 && coord(1) < 180) Valid
      else Invalid(ValidationError("Cooordinates exceed valid range (-90 <= lat < 90,-180 <= long < 180)."))
    }).getOrElse(Invalid(ValidationError("Must be in the format of '<latitude>,<longitude>'.")))
  }
  val timestamp: Mapping[String] = of[String] verifying Constraint[String]("timestamp") {
    o => {
      try {
        o.toLong
        Valid
      } catch {
        case e: RuntimeException => try {
          ISODateTimeFormat.dateTimeParser().parseDateTime(o)
          Valid
        } catch {
          case e: IllegalArgumentException => Invalid(ValidationError("Must either be a Unix time in milliseconds, or an ISO 8601 date and time."))
        }
      }
    }
  }
  val date: Mapping[String] = of[String] verifying Constraint[String]("date") {
    o => {
      try {
        o.toLong
        Valid
      } catch {
        case _ => try {
          ISODateTimeFormat.localDateParser().parseLocalDate(o)
          Valid
        } catch {
          case _ => Invalid(ValidationError("Must be an ISO 8601 date."))
        }
      }
    }
  }

  /** Utilties. */
  val emptyMap = Map()
  val emptyJsonObj = Json.obj()

  def parseLatlng(latlng: String): Tuple2[Double, Double] = {
    val splitted = latlng.split(",")
    (splitted(0).toDouble, splitted(1).toDouble)
  }

  def latlngToList(latlng: Tuple2[Double, Double]): List[Double] = List(latlng._1, latlng._2)

  /** Accepts UNIX timestamp, ISO 8601 time format, with optional timezone conversion from app settings. */
  def parseDateTimeFromString(timestring: String)(implicit app: App) = {
    try {
      new DateTime(timestring.toLong)
    } catch {
      case e: RuntimeException => try {
        val dt = ISODateTimeFormat.localDateOptionalTimeParser.parseLocalDateTime(timestring)
        dt.toDateTime(DateTimeZone.forID(app.timezone))
      } catch {
        case e: IllegalArgumentException => ISODateTimeFormat.dateTimeParser.parseDateTime(timestring)
      }
    }
  }

  /** API. */
  def status = Action {
    Ok("PredictionIO Output API is online.")
  }

  def createUser(format: String) = Action { implicit request =>
    FormattedResponse(format) {
      Attributes(tuple(
        "appkey" -> nonEmptyText,
        "uid" -> nonEmptyText,
        "latlng" -> optional(latlng),
        "inactive" -> optional(boolean)
      ), Set(
        "appkey",
        "ct",
        "uid",
        "latlng",
        "inactive"
      )).bindFromRequestAndFold(
        f => bindFailed(f.errors),
        (t, attributes) => {
          val (appkey, uid, latlng, inactive) = t
          AuthenticatedApp(t._1) { app =>
            users.insert(User(
              id = uid,
              appid = app.id,
              ct = DateTime.now,
              latlng = latlng map { parseLatlng(_) },
              inactive = inactive,
              attributes = Some(attributes)
            ))
            APIMessageResponse(CREATED, Map("message" -> "User created."))
          }
        }
      )
    }
  }

  def getUser(format: String, uid: String) = Action { implicit request =>
    FormattedResponse(format) {
      Form("appkey" -> nonEmptyText).bindFromRequest.fold(
        f => bindFailed(f.errors),
        t => AuthenticatedApp(t) { app =>
          users.get(app.id, uid) map { user =>
            APIUserResponse(OK, user)
          } getOrElse APIMessageResponse(NOT_FOUND, Map("message" -> "Cannot find user."))
        }
      )
    }
  }

  def deleteUser(format: String, uid: String) = Action { implicit request =>
    FormattedResponse(format) {
      Form("appkey" -> nonEmptyText).bindFromRequest.fold(
        f => bindFailed(f.errors),
        t => AuthenticatedApp(t) { app =>
          users.delete(app.id, uid)
          APIMessageResponse(OK, Map("message" -> "User deleted."))
        }
      )
    }
  }

  def createItem(format: String) = Action { implicit request =>
    FormattedResponse(format) {
      Attributes(tuple(
        "appkey" -> nonEmptyText,
        "iid" -> nonEmptyText,
        "itypes" -> nonEmptyText,
        "price" -> optional(numeric),
        "profit" -> optional(numeric),
        "startT" -> optional(timestamp),
        "endT" -> optional(timestamp),
        "latlng" -> optional(latlng),
        "inactive" -> optional(boolean)
      ), Set(
        "appkey",
        "ct",
        "iid",
        "itypes",
        "price",
        "profit",
        "startT",
        "endT",
        "latlng",
        "inactive"
      )).bindFromRequestAndFold(
        f => bindFailed(f.errors),
        (t, attributes) => {
          val (appkey, iid, itypes, price, profit, startT, endT, latlng, inactive) = t
          AuthenticatedApp(appkey) { implicit app =>
            items.insert(Item(
              id = iid,
              appid = app.id,
              ct = DateTime.now,
              itypes = itypes.split(",").toList,
              starttime = startT map { t => Some(parseDateTimeFromString(t)) } getOrElse Some(DateTime.now),
              endtime = endT map { parseDateTimeFromString(_) },
              price = price map { _.toDouble },
              profit = profit map { _.toDouble },
              latlng = latlng map { parseLatlng(_) },
              inactive = inactive,
              attributes = if (attributes.isEmpty) None else Some(attributes)
            ))
            APIMessageResponse(CREATED, Map("message" -> "Item created."))
          }
        }
      )
    }
  }

  def getItem(format: String, iid: String) = Action { implicit request =>
    FormattedResponse(format) {
      Form("appkey" -> nonEmptyText).bindFromRequest.fold(
        f => bindFailed(f.errors),
        t => AuthenticatedApp(t) { app =>
          items.get(app.id, iid) map { item =>
            APIItemResponse(OK, item)
          } getOrElse APIMessageResponse(NOT_FOUND, Map("message" -> "Cannot find item."))
        }
      )
    }
  }

  def deleteItem(format: String, iid: String) = Action { implicit request =>
    FormattedResponse(format) {
      Form("appkey" -> nonEmptyText).bindFromRequest.fold(
        f => bindFailed(f.errors),
        t => AuthenticatedApp(t) { app =>
          items.delete(app.id, iid)
          APIMessageResponse(OK, Map("message" -> "Item deleted."))
        }
      )
    }
  }

  def userToItemRate(format: String) = Action { implicit request =>
    FormattedResponse(format) {
      Form(tuple(
        "appkey" -> nonEmptyText,
        "uid" -> nonEmptyText,
        "iid" -> nonEmptyText,
        "t" -> optional(timestamp),
        "latlng" -> optional(latlng),
        "rate" -> number(1, 5)
      )).bindFromRequest.fold(
        f => bindFailed(f.errors),
        t => AuthenticatedApp(t._1) { implicit app =>
          u2iActions.insert(U2IAction(
            appid = app.id,
            action = u2iActions.rate,
            uid = t._2,
            iid = t._3,
            t = t._4 map { parseDateTimeFromString(_) } getOrElse DateTime.now,
            latlng = t._5 map { parseLatlng(_) },
            v = Some(t._6),
            price = None,
            evalid = None
          ))
          APIMessageResponse(CREATED, Map("message" -> "Rating recorded."))
        }
      )
    }
  }

  def userToItemLike(format: String) = Action { implicit request =>
    FormattedResponse(format) {
      Form(tuple(
        "appkey" -> nonEmptyText,
        "uid" -> nonEmptyText,
        "iid" -> nonEmptyText,
        "t" -> optional(timestamp),
        "latlng" -> optional(latlng)
      )).bindFromRequest.fold(
        f => bindFailed(f.errors),
        t => AuthenticatedApp(t._1) { implicit app =>
          u2iActions.insert(U2IAction(
            appid = app.id,
            action = u2iActions.likeDislike,
            uid = t._2,
            iid = t._3,
            t = t._4 map { parseDateTimeFromString(_) } getOrElse DateTime.now,
            latlng = t._5 map { parseLatlng(_) },
            v = Some(1),
            price = None,
            evalid = None
          ))
          APIMessageResponse(CREATED, Map("message" -> "Like recorded."))
        }
      )
    }
  }

  def userToItemDislike(format: String) = Action { implicit request =>
    FormattedResponse(format) {
      Form(tuple(
        "appkey" -> nonEmptyText,
        "uid" -> nonEmptyText,
        "iid" -> nonEmptyText,
        "t" -> optional(timestamp),
        "latlng" -> optional(latlng)
      )).bindFromRequest.fold(
        f => bindFailed(f.errors),
        t => AuthenticatedApp(t._1) { implicit app =>
          u2iActions.insert(U2IAction(
            appid = app.id,
            action = u2iActions.likeDislike,
            uid = t._2,
            iid = t._3,
            t = t._4 map { parseDateTimeFromString(_) } getOrElse DateTime.now,
            latlng = t._5 map { parseLatlng(_) },
            v = Some(0),
            price = None,
            evalid = None
          ))
          APIMessageResponse(CREATED, Map("message" -> "Dislike recorded."))
        }
      )
    }
  }

  def userToItemView(format: String) = Action { implicit request =>
    FormattedResponse(format) {
      Form(tuple(
        "appkey" -> nonEmptyText,
        "uid" -> nonEmptyText,
        "iid" -> nonEmptyText,
        "t" -> optional(timestamp),
        "latlng" -> optional(latlng)
      )).bindFromRequest.fold(
        f => bindFailed(f.errors),
        t => AuthenticatedApp(t._1) { implicit app =>
          u2iActions.insert(U2IAction(
            appid = app.id,
            action = u2iActions.view,
            uid = t._2,
            iid = t._3,
            t = t._4 map { parseDateTimeFromString(_) } getOrElse DateTime.now,
            latlng = t._5 map { parseLatlng(_) },
            v = None,
            price = None,
            evalid = None
          ))
          APIMessageResponse(CREATED, Map("message" -> "View recorded."))
        }
      )
    }
  }

  def userToItemConversion(format: String) = Action { implicit request =>
    FormattedResponse(format) {
      Form(tuple(
        "appkey" -> nonEmptyText,
        "uid" -> nonEmptyText,
        "iid" -> nonEmptyText,
        "t" -> optional(timestamp),
        "latlng" -> optional(latlng),
        "price" -> optional(numeric)
      )).bindFromRequest.fold(
        f => bindFailed(f.errors),
        t => AuthenticatedApp(t._1) { implicit app =>
          u2iActions.insert(U2IAction(
            appid = app.id,
            action = u2iActions.conversion,
            uid = t._2,
            iid = t._3,
            t = t._4 map { parseDateTimeFromString(_) } getOrElse DateTime.now,
            latlng = t._5 map { parseLatlng(_) },
            v = None,
            price = t._6 map { _.toDouble },
            evalid = None
          ))
          APIMessageResponse(CREATED, Map("message" -> "Conversion recorded."))
        }
      )
    }
  }

  def itemRecTopN(format: String, enginename: String) = Action { implicit request =>
    FormattedResponse(format) {
      Form(tuple(
        "appkey" -> nonEmptyText,
        "uid" -> nonEmptyText,
        "n" -> number(1, 100),
        "itypes" -> optional(text),
        "latlng" -> optional(latlng),
        "within" -> optional(numeric),
        "unit" -> optional(text)
      )).bindFromRequest.fold(
        f => bindFailed(f.errors),
        t => {
          val (appkey, uid, n, itypes, latlng, within, unit) = t
          AuthenticatedApp(appkey) { implicit app =>
            ValidEngine(enginename) { implicit engine =>
              try {
                val res = algoOutputSelector.itemRecSelection(
                  uid = uid,
                  n = n,
                  itypes = itypes map { _.split(",").toList }
                )
                if (res.length > 0)
                  APIMessageResponse(OK, Map("iids" -> res))
                else
                  APIMessageResponse(NOT_FOUND, Map("message" -> "Cannot find recommendation for user."))
              } catch {
                case e: Exception =>
                  APIMessageResponse(INTERNAL_SERVER_ERROR, Map("message" -> e.getMessage()))
              }
            }
          }
        }
      )
    }
  }
}

package controllers

import io.prediction.commons.Config
import io.prediction.commons.settings._
import io.prediction.commons.modeldata.ItemRecScores
import io.prediction.commons.appdata.{Users, Items, U2IActions}
import io.prediction.output.AlgoOutputSelector

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.{Constraints}
import play.api.i18n.{Messages, Lang}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsNull, JsArray, Json}
import play.api.libs.ws.WS
import play.api.Play.current
import play.api.http

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

import com.github.nscala_time.time.Imports._
import org.apache.commons.codec.digest.DigestUtils

/*
 * TODO:
 * - decodeURIComponent any GET custom param
 */


/*
 * Backend of ControlPanel.
 * Pure REST APIs in JSON.
 */
object Application extends Controller {
  /** PredictionIO Commons settings*/
  val config = new Config()
  val users = config.getSettingsUsers()
  val apps = config.getSettingsApps()
  val engines = config.getSettingsEngines()
  val engineInfos = config.getSettingsEngineInfos()
  val algos = config.getSettingsAlgos()
  val algoInfos = config.getSettingsAlgoInfos()
  val offlineEvalMetricInfos = config.getSettingsOfflineEvalMetricInfos()
  val offlineEvals = config.getSettingsOfflineEvals()
  val offlineEvalMetrics = config.getSettingsOfflineEvalMetrics()
  val offlineEvalResults = config.getSettingsOfflineEvalResults()
  val offlineEvalSplitters = config.getSettingsOfflineEvalSplitters()
  val offlineTunes = config.getSettingsOfflineTunes()
  val paramGens = config.getSettingsParamGens()

  /** PredictionIO Commons modeldata */
  val itemRecScores = config.getModeldataItemRecScores()
  val itemSimScores = config.getModeldataItemSimScores()

  /** PredictionIO Commons modeldata */
  val trainingItemRecScores = config.getModeldataTrainingItemRecScores()
  val trainingItemSimScores = config.getModeldataTrainingItemSimScores()

  /** PredictionIO Commons appdata */
  val appDataUsers = config.getAppdataUsers()
  val appDataItems = config.getAppdataItems()
  val appDataU2IActions = config.getAppdataU2IActions()

  /** PredictionIO Commons training set appdata */
  val trainingSetUsers = config.getAppdataTrainingUsers()
  val trainingSetItems = config.getAppdataTrainingItems()
  val trainingSetU2IActions = config.getAppdataTrainingU2IActions()

  /** PredictionIO Commons validation set appdata */
  val validationSetUsers = config.getAppdataValidationUsers()
  val validationSetItems = config.getAppdataValidationItems()
  val validationSetU2IActions = config.getAppdataValidationU2IActions()

  /** PredictionIO Commons test set appdata */
  val testSetUsers = config.getAppdataTestUsers()
  val testSetItems = config.getAppdataTestItems()
  val testSetU2IActions = config.getAppdataTestU2IActions()

  /** Scheduler setting */
  val settingsSchedulerUrl = config.settingsSchedulerUrl

  /** PredictionIO Output */
  val algoOutputSelector = new AlgoOutputSelector(algos)

  /** misc */
  val timeFormat = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss a z")
  val nameRegex = """\b[a-zA-Z][a-zA-Z0-9_-]*\b""".r

  /** Play Framework security */
  def username(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = Forbidden(toJson(Map("message" -> toJson("Haven't signed in yet."))))

  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  def withUser(f: User => Request[AnyContent] => Result) = withAuth { username => implicit request =>
    users.getByEmail(username).map { user =>
      f(user)(request)
    }.getOrElse(onUnauthorized(request))
  }

  private def md5password(password: String) = DigestUtils.md5Hex(password)

  /** Appkey Generation */
  def randomAlphanumeric(n: Int): String = {
    Random.alphanumeric.take(n).mkString
  }

  def showWeb() = Action {
    Ok(views.html.Web.index())
  }

  /* Serve Engines/Algorithms Static Files (avoid PlayFramework's Assets cache problem during development)*/
  def enginebase(path: String) = Action {
    Ok.sendFile(new java.io.File(Play.application.path, "/enginebase/" + path))
    //TODO: Fix Content-Disposition
  }

  def redirectToWeb = Action {
    Redirect("web/")
  }


  /** Authenticates user
    *
    * {{{
    * POST
    * JSON parameters:
    *   { 
    *     "email" : <string>,
    *     "password" : <string>,
    *     "remember" : <optional string "on">
    *   }
    * JSON response:
    *   {
    *     "username" : <string>,
    *     "email" : <string>
    *   }
    * }}}
    *
    */
  def signin = Action { implicit request =>
    val loginForm = Form(
      tuple(
        "email" -> text,
        "password" -> text,
        "remember" -> optional(text)
      ) verifying ("Invalid email or password", result => result match {
        case (email, password, remember) => users.authenticateByEmail(email, md5password(password)) map { _ => true } getOrElse false
      })
    )

    loginForm.bindFromRequest.fold(
      formWithErrors => Forbidden(toJson(Map("message" -> toJson("Incorrect Email or Password.")))),
      form => {
        val user = users.getByEmail(form._1).get
        Ok(toJson(Map(
          "username" -> "%s %s".format(user.firstName, user.lastName.getOrElse("")),
          "email" -> user.email
        ))).withSession(Security.username -> user.email)
      }
    )
  }

  /** Signs out
    *
    * {{{
    * POST
    * JSON parameters: 
    *   None
    * JSON response:
    *   None
    * }}}
    */
  def signout = Action {
    Ok.withNewSession
  }

  /** Returns authenticated user info
    * (from session cookie)
    *
    * {{{
    * GET
    * JSON parameters: 
    *   None
    * JSON response:
    *   If authenticated
    *   OK
    *   {
    *     "id" : <string>,
    *     "username" : <string>,
    *     "email": <string>
    *   }
    * 
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    * }}}
    */
  def getAuth = withUser { user => implicit request =>
    
    Ok(toJson(Map(
      "id" -> user.id.toString,
      "username" -> (user.firstName + user.lastName.map(" "+_).getOrElse("")),
      "email" -> user.email
    )))
  }

  /** Returns list of apps of the authenticated user
    *
    * {{{
    * GET
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If no app:
    *   No Content
    *
    *   If apps are found:
    *   OK
    *   [ { "id" : <appid int>,  "appname" : <string> }, 
    *     ...
    *   ]
    *
    * }}} 
    */
  def getApplist = withUser { user => implicit request =>

    val userApps = apps.getByUserid(user.id)
    if (!userApps.hasNext) NoContent
    else {
      Ok(toJson(userApps.map { app =>
        Map("id" -> app.id.toString, "appname" -> app.display)
      }.toSeq))
    }
  }

  /** Returns the app details of this appid
    *
    * {{{
    * GET
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If app is not found:
    *   NotFound
    *   {
    *     "message" : "Invalid appid."
    *   }
    *
    *   If app is found:
    *   Ok
    *   {
    *     "id" : <appid int>,
    *     "updatedtime" : <string>,
    *     "userscount": <num of users int>
    *     "itemscount": <num of items int>
    *     "u2icount": <num of u2i Actions int>
    *     "apiurl": <url of API server, string>
    *     "appkey": <string>
    *   }
    * }}}
    *
    * @param id the App ID
    */
  def getAppDetails(id: Int) = withUser { user => implicit request =>

    apps.getByIdAndUserid(id, user.id) map { app =>
      val numUsers = appDataUsers.countByAppid(app.id)
      val numItems = appDataItems.countByAppid(app.id)
      val numU2IActions = appDataU2IActions.countByAppid(app.id)
      Ok(toJson(Map(
        "id" -> toJson(app.id), // app id
        "updatedtime" -> toJson(timeFormat.print(DateTime.now.withZone(DateTimeZone.forID("UTC")))),
        "userscount" -> toJson(numUsers),
        "itemscount" -> toJson(numItems),
        "u2icount" -> toJson(numU2IActions),
        "apiurl" -> toJson("http://yourhost.com:123/appid12"),
        "appkey" -> toJson(app.appkey))))
    } getOrElse {
      NotFound(toJson(Map("message" -> toJson("Invalid appid."))))
    }
  }

  /** Returns list of engines of this appid
    *
    * {{{
    * GET
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If no engine:
    *   NoContent
    *
    *   If engines found:
    *   Ok
    *   {
    *     "id" : <appid int>,
    *     "enginelist" : [ { "id" : <engineid int>, "enginename" : <string>, "engineinfoid" : <string> },
    *                       ....,
    *                      { "id" : <engineid int>, "enginename" : <string>, "engineinfoid" : <string> } ]
    *
    *   }
    * }}}
    *
    * @param id the App ID
    */
  def getAppEnginelist(id: Int) = withUser { user => implicit request =>

    val appEngines = engines.getByAppid(id)

    if (!appEngines.hasNext) NoContent
    else
      Ok(toJson(Map(
        "id" -> toJson(id),
        "enginelist" -> toJson((appEngines map { eng =>
          Map("id" -> eng.id.toString, "enginename" -> eng.name,"engineinfoid" -> eng.infoid)
        }).toSeq)
      )))

  }

  /** Returns an app
    *
    * {{{
    * GET
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If app is found:
    *   Ok
    *   {
    *     "id" : <appid int>
    *     "appname" : <string>
    *   }
    *
    * }}}
    *
    * @param id the App ID
    */
  def getApp(id: Int) = withUser { user => implicit request =>
    val app = apps.getByIdAndUserid(id, user.id).get
    Ok(toJson(Map(
      "id" -> app.id.toString, // app id
      "appname" -> app.display)))
  }

  /** Create an app
    *
    * {{{
    * POST
    * JSON Parameters:
    *   {
    *     "appname" : <the new app name. string>
    *   }
    * JSON Response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If creation failed:
    *   BadRequest
    *   {
    *     "message" : "Invalid character for app name."
    *    }
    *
    *   If app is created:
    *   OK
    *   {
    *     "id" : <the appid of the created app. int>,
    *     "appname" : <string>
    *   }
    * }}}
    *
    */
  def createApp = withUser { user => implicit request =>

    val bad = BadRequest(toJson(Map("message" -> toJson("Invalid character for app name."))))

    request.body.asJson map { js =>
      val appName = (js \ "appname").asOpt[String]
      appName map { an =>
        if (an == "") bad
        else {
          val appid = apps.insert(App(
            id = 0,
            userid = user.id,
            appkey = randomAlphanumeric(64),
            display = an,
            url = None,
            cat = None,
            desc = None,
            timezone = "UTC"
          ))
          Logger.info("Create app ID " + appid)

          Ok(toJson(Map(
            "id" -> appid.toString,
            "appname" -> an
          )))
        }
      } getOrElse bad
    } getOrElse bad
  }

  /** Remove an app
    * 
    * {{{
    * DELETE
    * JSON Parameters:
    *   None
    * JSON Response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If not found:
    *   NotFound
    *   {
    *     "message" : <error message>
    *   }
    *
    *   If error:
    *   InternalServerError
    *   {
    *     "message" : <error message>
    *   }
    *   
    *   If deleted successfully:
    *   Ok
    * }}}
    *
    * @param id the App ID
    */
  def removeApp(id: Int) = withUser { user => implicit request =>

    Async {
      apps.get(id) map { app =>

        // don't delete if there is any sim eval and offline tune pending
        val pendingSimEvals = engines.getByAppid(app.id) flatMap { eng => 
          Helper.getSimEvalsByEngineid(eng.id).filter( x => Helper.isPendingSimEval(x) ).toList
        }
        //Logger.info("Pending sim evals "+ pendingSimEvals.map(x => x.id).mkString(","))

        val pendingOfflineTunes = engines.getByAppid(app.id) flatMap { eng =>
          offlineTunes.getByEngineid(eng.id).filter( x => Helper.isPendingOfflineTune(x) ).toList
        }

        val deployedAlgos = engines.getByAppid(app.id) flatMap { eng =>
          algos.getDeployedByEngineid(eng.id).toList
        }

        if (deployedAlgos.size != 0) {
          concurrent.Future(Forbidden(Json.obj("message" -> s"There are deployed algorithms. Please undeploy them before delete this app.")))
        } else if (pendingSimEvals.size != 0) {
          concurrent.Future(Forbidden(Json.obj("message" -> 
            "There are running simulated evaluations. Please stop and delete them before delete this app.")))
        } else if (pendingOfflineTunes.size != 0) {
          concurrent.Future(Forbidden(Json.obj("message" -> 
            "There are auto-tuning algorithms. Please stop and delete them before delete this app.")))
        } else {

          val timeout = play.api.libs.concurrent.Promise.timeout("Scheduler is unreachable. Giving up.", concurrent.duration.Duration(10, concurrent.duration.MINUTES))

          val delete = Helper.deleteAppScheduler(app.id)

          concurrent.Future.firstCompletedOf(Seq(delete, timeout)).map {
            case r: SimpleResult => {
              if (r.header.status == http.Status.OK) {
                Helper.deleteApp(id, user.id, keepSettings=false)
              }
              r
            }
            case t: String => InternalServerError(Json.obj("message" -> t))

          }
        }

      } getOrElse {
        concurrent.Future(NotFound(toJson(Map("message" -> toJson("Invalid appid.")))))
      }
    }
  }

  /** Erase appdata of this appid
    * 
    * {{{
    * POST
    * JSON Parameters:
    *   None
    * JSON Response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If erased successfully:
    *   Ok
    * }}}
    *
    * @param id the App ID
    */
  def eraseAppData(id: Int) = withUser { user => implicit request =>

    Async {
      apps.get(id) map { app =>

        val timeout = play.api.libs.concurrent.Promise.timeout("Scheduler is unreachable. Giving up.", concurrent.duration.Duration(10, concurrent.duration.MINUTES))

        val delete = Helper.deleteAppScheduler(app.id)

        concurrent.Future.firstCompletedOf(Seq(delete, timeout)).map {
          case r: SimpleResult => {
            if (r.header.status == http.Status.OK) {
              Helper.deleteApp(id, user.id, keepSettings=true)
            }
            r
          }
          case t: String => InternalServerError(Json.obj("message" -> t))

        }
        
      } getOrElse {
        concurrent.Future(NotFound(toJson(Map("message" -> toJson("Invalid appid.")))))
      }
    }
  }

  /** Returns a list of available engine infos in the system
    *
    * {{{
    * GET
    * JSON Parameters:
    *   None
    * JSON Response:
    *   Ok
    *   [ { "id" : <engine info id stirng>,
    *       "engineinfoname" : <name of the engine info>,
    *       "description": <description in html string>
    *     },
    *     ...
    *   ]
    * }}}
    */
  def getEngineInfoList = Action {
    Ok(JsArray(engineInfos.getAll() map {
      eng => Json.obj( 
        "id" -> eng.id,
        "engineinfoname" -> eng.name,
        "description" -> eng.description
        )
    }))
  }

  /** Returns a list of available algo infos of a specific engine info
    *
    * {{{
    * GET
    * JSON Parameters:
    *   None
    * JSON Response:
    *   If the engine info id is not found:
    *   InternalServerError
    *   {
    *     "message" : "Invalid EngineInfo ID."
    *   }
    *
    *   If found:
    *   Ok
    *   { "engineinfoname" : <the name of the engine info>,
    *     "algotypelist" : [ { "id" : <algo info id>, 
    *                          "algoinfoname" : <name of the algo info>, 
    *                          "description" : <string>,
    *                          "req" : <technology requirement string>,
    *                          "datareq" : <data requirement string>
    *                        }, ... 
    *                      ] 
    *   }
    * }}}
    *
    * @param id the engine info id
    */
  def getEngineInfoAlgoList(id: String) = Action {
    engineInfos.get(id) map { engineInfo =>
      Ok(toJson(
        Map(
          "engineinfoname" -> toJson(engineInfo.name),
          "algotypelist" -> toJson(
            (algoInfos.getByEngineInfoId(id) map { algoInfo =>
              Map(
                "id" -> toJson(algoInfo.id),
                "algoinfoname" -> toJson(algoInfo.name),
                "description" -> toJson(algoInfo.description.getOrElse("")),
                "req" -> toJson(algoInfo.techreq),
                "datareq" -> toJson(algoInfo.datareq)
                )

            }).toSeq )
           )

       ))
    } getOrElse InternalServerError(Json.obj("message" -> "Invalid EngineInfo ID."))
  }

  /** Returns a list of available metric infos of a specific engine info
    *
    * {{{
    * GET
    * JSON Parameters:
    *   None
    * JSON Response:
    *   If the engine info id is not found:
    *   InternalServerError
    *   {
    *     "message" : "Invalid EngineInfo ID."
    *   }
    *
    *   If found:
    *   Ok
    *   { "engineinfoname" : <the name of the engine info>,
    *     "metricslist" : [ { "id" : <metric info id>, 
    *                         "metricsname" : <short name of the metric info>,
    *                         "metricslongname" : <long name of the metric info>,
    *                         "settingfields" : { "param1" : <param type in string>, ... }
    *                       }, ... 
    *                     ] 
    *   }
    * }}}
    *
    * @param id the engine info id
    */
  def getEngineInfoMetricsTypeList(id: String) = Action {    
    engineInfos.get(id) map { engInfo =>
      val metrics = offlineEvalMetricInfos.getByEngineinfoid(engInfo.id) map { m =>
        Json.obj(
          "id" -> m.id,
          "metricsname" -> m.name,
          "metricslongname" -> m.description,
          "settingfields" -> Json.toJson(
            m.paramorder.map( p =>
              (m.paramnames(p) -> "int") // TODO: param constraint type, hardcode to int now
            ).toMap
          )
        )
      }
      Ok(Json.obj(
        "engineinfoname" -> engInfo.name,
        "metricslist" -> JsArray(metrics)
      ))
    } getOrElse {
      InternalServerError(Json.obj("message" -> s"Invalid engineinfo ID: ${id}."))
    }
  }

  /** Returns a list of available metric infos of a specific engine info
    *
    * {{{
    * GET
    * JSON Parameters:
    *   None
    * JSON Response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If engine not found:
    *   NotFound
    *   {
    *     "message" : "Invalid app id or engine id."
    *   }
    *
    *   If engine found:
    *   Ok
    *   {
    *     "id" : <engine id>,
    *     "engineinfoid" : <engine info id>,
    *     "enginename" : <engine name>,
    *     "enginestatus" : <engine status>
    *   }
    * }}}
    * @note engine status:
    *   noappdata
    *   nodeployedalgo
    *   firsttraining
    *   nomodeldata
    *   nomodeldatanoscheduler
    *   training
    *   running
    *   runningnoscheduler
    *
    * @param appid the App ID
    * @param id the engine ID
    */
  def getEngine(appid: Int, id: Int) = withUser { user => implicit request =>

    val engine = engines.get(id)

    engine map { eng: Engine =>
      val modelDataExist: Boolean = eng.infoid match {
        case "itemrec" => try { itemRecScores.existByAlgo(algoOutputSelector.itemRecAlgoSelection(eng)) } catch { case e: RuntimeException => false }
        case "itemsim" => try { itemSimScores.existByAlgo(algoOutputSelector.itemSimAlgoSelection(eng)) } catch { case e: RuntimeException => false }
        case _ => false
      }
      val deployedAlgos = algos.getDeployedByEngineid(eng.id)
      val hasDeployedAlgo = deployedAlgos.hasNext
      val algo = if (deployedAlgos.hasNext) Some(deployedAlgos.next()) else None
      val engineStatus: String =
        if (appDataUsers.countByAppid(eng.appid) == 0 && appDataItems.countByAppid(eng.appid) == 0 && appDataU2IActions.countByAppid(eng.appid) == 0)
          "noappdata"
        else if (!hasDeployedAlgo)
          "nodeployedalgo"
        else if (!modelDataExist)
          try {
            (Await.result(WS.url(s"${settingsSchedulerUrl}/apps/${eng.appid}/engines/${eng.id}/algos/${algo.get.id}/status").get(), scala.concurrent.duration.Duration(5, SECONDS)).json \ "status").as[String] match {
              case "jobrunning" => "firsttraining"
              case _ => "nomodeldata"
            }
          } catch {
            case e: java.net.ConnectException => "nomodeldatanoscheduler"
          }
        else
          try {
            (Await.result(WS.url(s"${settingsSchedulerUrl}/apps/${eng.appid}/engines/${eng.id}/algos/${algo.get.id}/status").get(), scala.concurrent.duration.Duration(5, SECONDS)).json \ "status").as[String] match {
              case "jobrunning" => "training"
              case _ => "running"
            }
          } catch {
            case e: java.net.ConnectException => "runningnoscheduler"
          }
      Ok(Json.obj(
        "id" -> eng.id.toString, // engine id
        "engineinfoid" -> eng.infoid,
        "appid" -> eng.appid.toString,
        "enginename" -> eng.name,
        "enginestatus" -> engineStatus))
    } getOrElse {
      // if No such app id
      NotFound(toJson(Map("message" -> toJson("Invalid app id or engine id."))))
    }

  }

  /** Creates an Engine
    * 
    * {{{
    * POST
    * JSON parameters:
    *   {
    *     "appid" : <app id>,
    *     "engineinfoid" : <engine info id>,
    *     "enginename" : <engine name>
    *   }
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If bad param:
    *   BadRequest
    *   {
    *     "message" : <error message>
    *   }
    *
    *   If created:
    *   Ok
    *   {
    *     "id" : <new engine id>,
    *     "engineinfoid" : <engine info id>,
    *     "appid" : <app id>,
    *     "enginename" : <engine name>
    *   }
    *
    * }}}
    *
    * @param appid the App ID
    */
  def createEngine(appid: Int) = withUser { user => implicit request =>
    val supportedEngineTypes: Seq[String] = engineInfos.getAll() map { _.id }
    val enginenameConstraint = Constraints.pattern(nameRegex, "constraint.enginename", "Engine names should only contain alphanumerical characters, underscores, or dashes. The first character must be an alphabet.")

    val engineForm = Form(tuple(
      "appid" -> number,
      "engineinfoid" -> (text verifying("This feature will be available soon.", e => supportedEngineTypes.contains(e))),
      "enginename" -> (text verifying("Please name your engine.", enginename => enginename.length > 0)
                          verifying enginenameConstraint)
    ) verifying("Engine name must be unique.", f => !engines.existsByAppidAndName(f._1, f._3))
    verifying("Engine type is invalid.", f => engineInfos.get(f._2).map(_ => true).getOrElse(false)))

    // TODO: if No such app id
    /*
     *  NotFound(toJson(Map("message" -> toJson("invalid app id"))))
     */

    engineForm.bindFromRequest.fold(
      formWithError => {
        //println(formWithError.errors)
        val msg = formWithError.errors(0).message // extract 1st error message only
        //BadRequest(toJson(Map("message" -> toJson("invalid engine name"))))
        BadRequest(toJson(Map("message" -> toJson(msg))))
      },
      formData => {
        val (fappid, enginetype, enginename) = formData
        val engineInfo = engineInfos.get(enginetype).get
        val engineId = engines.insert(Engine(
          id = -1,
          appid = fappid,
          name = enginename,
          infoid = enginetype,
          itypes = None, // NOTE: default None (means all itypes)
          settings = engineInfo.defaultsettings.map(s => (s._2.id, s._2.defaultvalue)) // TODO: depends on enginetype
        ))
        Logger.info("Create engine ID " + engineId)

        // automatically create default algo
        val defaultAlgoType = engineInfo.defaultalgoinfoid
        val defaultAlgo = Algo(
          id = -1,
          engineid = engineId,
          name = "Default-Algo", // TODO: get it from engineInfo
          infoid = defaultAlgoType,
          command = "",
          params = algoInfos.get(defaultAlgoType).get.params.mapValues(_.defaultvalue),
          settings = Map(), // no use for now
          modelset = false, // init value
          createtime = DateTime.now,
          updatetime = DateTime.now,
          status = "deployed", // this is default deployed algo
          offlineevalid = None,
          loop = None
        )

        val algoId = algos.insert(defaultAlgo)
        Logger.info("Create algo ID " + algoId)

        WS.url(settingsSchedulerUrl+"/users/"+user.id+"/sync").get()

        Ok(toJson(Map(
          "id" -> engineId.toString, // engine id
          "engineinfoid" -> enginetype,
          "appid" -> fappid.toString,
          "enginename" -> enginename)))
      }
    )

  }

  /** Removes an engine
    *
    * {{{
    * DELETE
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If deleted:
    *   Ok
    *
    * }}}
    *
    * @param appid the App ID
    * @param engineid the engine ID
    */
  def removeEngine(appid: Int, engineid: Int) = withUser { user => implicit request =>

    engines.get(engineid) map { eng =>

      // don't delete if there is any sim eval and offline tune pending, or deployed algorithm
      val pendingSimEvals = Helper.getSimEvalsByEngineid(eng.id).filter( x => Helper.isPendingSimEval(x) ).toList
      val pendingOfflineTunes = offlineTunes.getByEngineid(eng.id).filter( x => Helper.isPendingOfflineTune(x) ).toList
      val deployedAlgos = algos.getDeployedByEngineid(eng.id).toList

      if (deployedAlgos.size != 0) {
        val names = deployedAlgos map ( x => x.name ) mkString(",")
        Forbidden(Json.obj("message" -> s"This engine has deployed algorithms (${names}). Please undeploy them before delete this engine."))
      } else if (pendingSimEvals.size != 0) {
        Forbidden(Json.obj("message" -> "There are running simulated evaluations. Please stop and delete them before delete this engine."))
      } else if (pendingOfflineTunes.size != 0) {
        Forbidden(Json.obj("message" -> "There are auto-tuning algorithms. Please stop and delete them before delete this engine."))
      } else {
        /** Deletion could take a while */
        val timeout = play.api.libs.concurrent.Promise.timeout("Scheduler is unreachable. Giving up.", concurrent.duration.Duration(10, concurrent.duration.MINUTES))

        // to scheduler: delete engine
        val delete = Helper.deleteEngineScheduler(appid, engineid)

        Async {
          concurrent.Future.firstCompletedOf(Seq(delete, timeout)).map {
            case r: SimpleResult => {
              if (r.header.status == http.Status.OK) {
                Helper.deleteEngine(engineid, appid, keepSettings=false)
              }
              r
            }
            case t: String => InternalServerError(Json.obj("message" -> t))
          }
        }
      }

    } getOrElse {
      NotFound(Json.obj("message" -> s"Engine ID $engineid does not exist."))    
    }

  }

  /** Returns a list of available (added but not deployed) algorithms of this engine
    * {{{
    * GET
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If no algo:
    *   NoContent
    *
    *   If algos found:
    *   [ { "id" : <algo id>,
    *       "algoname" : <algo name>,
    *       "appid" : <app id>,
    *       "engineid" : <engine id>,
    *       "algoinfoid" : <algo info id>,
    *       "algoinfoname" : <algo info name>,
    *       "status" : <algo status>,
    *       "updatedtime" : <algo last updated time>
    *     }, ...
    *   ]
    * }}}
    * @note algo status
    *   TODO: add more info here...
    *
    * @param appid the App ID
    * @param engineid the engine ID
    */
  def getAvailableAlgoList(appid: Int, engineid: Int) = withUser { user => implicit request =>

    val engineAlgos = algos.getByEngineid(engineid)

    if (!engineAlgos.hasNext) NoContent
    else
      Ok(toJson( // NOTE: only display algos which are not "deployed", nor "simeval"
          (engineAlgos filter { Helper.isAvailableAlgo(_) } map { algo =>
           Map("id" -> algo.id.toString,
               "algoname" -> algo.name,
               "appid" -> appid.toString,
               "engineid" -> algo.engineid.toString,
               "algoinfoid" -> algo.infoid,
               "algoinfoname" -> algoInfos.get(algo.infoid).get.name,
               "status" -> algo.status,
               "updatedtime" -> timeFormat.print(algo.updatetime.withZone(DateTimeZone.forID("UTC")))
               )
         }).toSeq
      ))


  }

  /** Returns an available algorithm
    * {{{
    * GET
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If algo not found:
    *   NotFound
    *   {
    *     "message" : "Invalid app id, engine id or algo id."
    *   }
    *
    *   If found:
    *   Ok
    *   {
    *     "id" : <algo id>,
    *     "algoname" : <algo name>,
    *     "appid" : <app id>,
    *     "engineid" : <engine id>,
    *     "algoinfoid" : <algo info id>,
    *     "algoinfoname" : <algo info name>,
    *     "status" : <algo status>,
    *     "createdtime" : <algo creation time>,
    *     "updatedtime" : <algo last updated time>
    *   }
    * }}} 
    *
    * @param appid the App ID
    * @param engineid the engine ID
    * @param id the algo ID
    */
  def getAvailableAlgo(appid: Int, engineid: Int, id: Int) = withUser { user => implicit request =>

    val optAlgo: Option[Algo] = algos.get(id)

    optAlgo map { algo =>
      Ok(toJson(Map(
          "id" -> algo.id.toString,
          "algoname" -> algo.name,
          "appid" -> appid.toString,
          "engineid" -> algo.engineid.toString,
          "algoinfoid" -> algo.infoid,
          "algoinfoname" -> algoInfos.get(algo.infoid).get.name,
          "status" -> "ready", // default status TODO: what status allowed here?
          "createdtime" -> timeFormat.print(algo.createtime.withZone(DateTimeZone.forID("UTC"))),
          "updatedtime" -> timeFormat.print(algo.updatetime.withZone(DateTimeZone.forID("UTC")))
        )))
    } getOrElse {
      NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id."))))
    }
  }

  /** Creates an new algorithm
    * {{{
    * POST
    * JSON parameters:
    *   {
    *     "algoinfoid" : <algo info id>,
    *     "algoname" : <algo name>,
    *     "appid" : <app id>,
    *     "engineid" : <engine id>
    *   }
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If creation failed:
    *   BadRequest
    *   {
    *     "message" : <error message>
    *   }
    *
    *   If created:
    *   Ok
    *   {
    *     "id" : <algo id>,
    *     "algoname" : <algo name>,
    *     "appid" : <app id>,
    *     "engineid" : <engine id>,
    *     "algoinfoid" : <algo info id>,
    *     "algoinfoname" : <algo info name>,
    *     "status" : <algo status>,
    *     "createdtime" : <algo creation time>,
    *     "updatedtime" : <algo last updated time>
    *   }
    *   
    * }}}
    * 
    * @param appid the App ID
    * @param engineid the engine ID
    */
  def createAvailableAlgo(appid: Int, engineid: Int) = withUser { user => implicit request =>
    // TODO: if No such app id or engine id
    /*
     *  NotFound(toJson(Map("message" -> toJson("invalid app id or engine id"))))
     */
    val supportedAlgoTypes: Seq[String] = algoInfos.getAll map { _.id }
    val algonameConstraint = Constraints.pattern(nameRegex, "constraint.algoname", "Algorithm names should only contain alphanumerical characters, underscores, or dashes. The first character must be an alphabet.")

    val createAlgoForm = Form(tuple(
      "algoinfoid" -> (nonEmptyText verifying("This feature will be available soon.", t => supportedAlgoTypes.contains(t))),
      "algoname" -> (text verifying("Please name your algo.", name => name.length > 0)
                          verifying algonameConstraint), // same name constraint as engine
      "appid" -> number,
      "engineid" -> number
    ) verifying("Algo name must be unique.", f => !algos.existsByEngineidAndName(f._4, f._2)))

    createAlgoForm.bindFromRequest.fold(
      formWithError => {
        //println(formWithError.errors)
        val msg = formWithError.errors(0).message // extract 1st error message only
        BadRequest(toJson(Map("message" -> toJson(msg))))
      },
      formData => {
        val (algoType, algoName, appId, engineId) = formData

        // TODO: store algotype into algos db?
        val algoInfoOpt = algoInfos.get(algoType)

        if (algoInfoOpt == None) {
          BadRequest(toJson(Map("message" -> toJson("Invalid AlgoType."))))
        } else {
          val algoInfo = algoInfoOpt.get

          val newAlgo = Algo(
            id = -1,
            engineid = engineId,
            name = algoName,
            infoid = algoType,
            command = "",
            params = algoInfo.params.mapValues(_.defaultvalue),
            settings = Map(), // no use for now
            modelset = false, // init value
            createtime = DateTime.now,
            updatetime = DateTime.now,
            status = "ready", // default status
            offlineevalid = None,
            loop = None
          )

          val algoId = algos.insert(newAlgo)
          Logger.info("Create algo ID " + algoId)

          Ok(toJson(Map(
            "id" -> algoId.toString, // algo id
            "algoname" -> newAlgo.name,
            "appid" -> appId.toString,
            "engineid" -> newAlgo.engineid.toString,
            "algoinfoid" -> algoType,
            "algoinfoname" -> algoInfos.get(algoType).get.name,
            "status" -> newAlgo.status,
            "createdtime" -> timeFormat.print(newAlgo.createtime.withZone(DateTimeZone.forID("UTC"))),
            "updatedtime" -> timeFormat.print(newAlgo.updatetime.withZone(DateTimeZone.forID("UTC")))
          )))
        }

      }
    )

  }

  /** Deletes an algorithm
    * {{{
    * DELETE
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If deleted:
    *   Ok
    * }}}
    *
    * @param appid the App ID
    * @param engineid the engine ID
    * @param id the algo ID 
    */
  def removeAvailableAlgo(appid: Int, engineid: Int, id: Int) = withUser { user => implicit request =>

    algos.get(id) map { algo =>

      /** Deletion could take a while */
      val timeout = play.api.libs.concurrent.Promise.timeout("Scheduler is unreachable. Giving up.", concurrent.duration.Duration(10, concurrent.duration.MINUTES))

      val deleteTune = algo.offlinetuneid map { tuneid =>
        offlineTunes.get(tuneid) map { tune =>
          /** Make sure to unset offline tune's creation time to prevent scheduler from picking up */
          offlineTunes.update(tune.copy(createtime = None))

          Helper.stopAndDeleteOfflineTuneScheduler(appid, engineid, tuneid)
        } getOrElse {
          concurrent.Future { Ok }
        }
      } getOrElse {
        concurrent.Future { Ok }
      }

      val deleteAlgo: concurrent.Future[SimpleResult] = Helper.deleteAlgoScheduler(appid, engineid, algo.id)

      val complete: concurrent.Future[SimpleResult] = concurrent.Future.reduce(Seq(deleteTune, deleteAlgo)) { (a,b) => 
        if (a.header.status != http.Status.OK) // keep the 1st error
          a
        else
          b
      } 

      Async {
        concurrent.Future.firstCompletedOf(Seq(complete, timeout)).map {
          case r: SimpleResult => {
            if (r.header.status == http.Status.OK) {
              algo.offlinetuneid map { tuneid =>
                Helper.deleteOfflineTune(tuneid, keepSettings=false)
              }
              Helper.deleteAlgo(algo.id, keepSettings=false)
            }
            r
          }
          case t: String => InternalServerError(Json.obj("message" -> t))
        }
      }
    } getOrElse {
      NotFound(Json.obj("message" -> s"Algo ID $id does not exist."))
    }

  }

  /** Returns a list of deployed algorithms
    * {{{
    * GET
    * JSON parameters:
    *   None
    * JSON repseon:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *   
    *   If no deployed algo:
    *   NoContent
    *
    *   If deployed algos found:
    *   {
    *     "updatedtime" : <TODO>,
    *     "status" : <TODO>,
    *     "algolist" : [ { "id" : <algo id>,
    *                      "algoname" : <algo name>,
    *                      "appid" : <app id>,
    *                      "engineid" : <engine id>,
    *                      "algoinfoid" : <algo info id>,
    *                      "algoinfoname" : <algo info name>,
    *                      "status" : <algo status>,
    *                      "updatedtime" : <algo last updated time>
    *                    }, ...
    *                  ]
    *   }
    * }}}
    *
    * @param appid the App ID
    * @param engineid the engine ID
    */
  def getDeployedAlgo(appid: Int, engineid: Int) = withUser { user => implicit request =>

    val deployedAlgos = algos.getDeployedByEngineid(engineid)

    if (!deployedAlgos.hasNext) NoContent
    else
      Ok(toJson(Map(
       "updatedtime" -> toJson("12-03-2012 12:32:12"), // TODO: what's this time for?
       "status" -> toJson("Running"),
       "algolist" -> toJson(deployedAlgos.map { algo =>
         Map("id" -> algo.id.toString,
	         "algoname" -> algo.name,
	         "appid" -> appid.toString,
	         "engineid" -> algo.engineid.toString,
	         "algoinfoid" -> algo.infoid,
	         "algoinfoname" -> algoInfos.get(algo.infoid).get.name,
	         "status" -> algo.status,
	         "updatedtime" -> timeFormat.print(algo.updatetime.withZone(DateTimeZone.forID("UTC"))))
       }.toSeq)
      )))

  }

  /** Returns the algorithm auto tune report
    *
    * {{{
    * GET
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If not found:
    *   NotFound
    *   {
    *     "message" : <error message>
    *   }
    *
    *   If found:
    *   Ok
    *   {
    *     "id" : <algo id>,
    *     "appid" : <app id>,
    *     "engineid" : <engine id>,
    *     "algo" : {
    *                "id" : <original algo id>,
    *                "algoname" : <algo name>,
    *                "appid" : <app id>,
    *                "engineid" : <engine id>,
    *                "algoinfoid" : <algo info id>,
    *                "algoinfoname" : <algo info name>,
    *                "settingsstring" : <algo setting string> 
    *              },
    *     "metric" : { 
    *                  "id" : <metric id>,
    *                  "engineid" : <engine id>,
    *                  "engineinfoid" : <engine info id>,
    *                  "metricsinfoid" : <metric info id>,
    *                  "metricsname" : <metric name>,
    *                  "settingsstring" : <metric setting string> 
    *                },
    *     "metricscorelist" : [
    *                           { 
    *                             "algoautotuneid" : <tuned algo id>,
    *                             "settingsstring" : <algo setting string>,
    *                             "score" : <average score> 
    *                           }, ...
    *                         ],
    *     "metricscoreiterationlist" : [
    *                                    [ { 
    *                                        "algoautotuneid" : <tuneid algo id 1>,
    *                                        "settingsstring" : <algo setting string>,
    *                                        "score" : <score of 1st iteration for this ituned algo id>
    *                                      },
    *                                      { 
    *                                        "algoautotuneid" : <tuneid algo id 2>,
    *                                        "settingsstring" : <algo setting string>,
    *                                        "score" : <score of 1st iteration for this ituned algo id>
    *                                      }, ...
    *                                    ],
    *                                    [ { 
    *                                        "algoautotuneid" : <tuneid algo id 1>,
    *                                        "settingsstring" : <algo setting string>,
    *                                        "score" : <score of 2nd iteration for this ituned algo id>
    *                                      },
    *                                      { 
    *                                        "algoautotuneid" : <tuneid algo id 2>,
    *                                        "settingsstring" : <algo setting string>,
    *                                        "score" : <score of 2nd iteration for this ituned algo id>
    *                                      }, ...
    *                                    ], ...
    *                                  ],
    *     "splittrain" : <training set split percentage 1 to 100>,
    *     "splitvalidation" : <validation set split percentage 1 to 100>,
    *     "splittest" : <test set split percentage 1 to 100>,
    *     "splitmethod" : <split method string: randome, time>
    *     "evaliteration" : <number of iterations>,
    *     "status" : <auto tune status>,
    *     "starttime" : <start time>,
    *     "endtime" : <end time>
    *   }
    * }}}
    *
    * @param appid the App ID
    * @param engineid the engine ID
    * @param algoid the algo ID
    */
  def getAlgoAutotuningReport(appid: Int, engineid: Int, algoid: Int) = withUser { user => implicit request =>

    // get the offlinetuneid of this algo
    algos.get(algoid) map { algo =>
      algo.offlinetuneid map { tuneid =>
        offlineTunes.get(tuneid) map { tune =>

          // get all offlineeval of this offlinetuneid
          val tuneOfflineEvals: Array[OfflineEval] = offlineEvals.getByTuneid(tuneid).toArray.sortBy(_.id)

          val tuneMetrics: Array[OfflineEvalMetric] = tuneOfflineEvals.flatMap{ e => offlineEvalMetrics.getByEvalid(e.id) }

          val tuneSplitters: Array[OfflineEvalSplitter] = tuneOfflineEvals.flatMap{ e => offlineEvalSplitters.getByEvalid(e.id) }

          // get all offlineeavlresults of each offlineevalid
          val tuneOfflineEvalResults: Array[OfflineEvalResult] = tuneOfflineEvals.flatMap{ e => offlineEvalResults.getByEvalid(e.id) }

          // test set score
          val tuneOfflineEvalResultsTestSet: Array[OfflineEvalResult] = tuneOfflineEvalResults.filter( x => (x.splitset == "test") )

          val tuneOfflineEvalResultsTestSetAlgoidMap: Map[Int, Double] = tuneOfflineEvalResultsTestSet.map(x => (x.algoid -> x.score)).toMap

          // get all algos of each offlineevalid
          // note: this happen after getting all available offlineEvalResults,
          // so these retrieved algos may have more algo than those used in offlineEvalResults.
          val tuneAlgos: Array[Algo] = tuneOfflineEvals flatMap { e => algos.getByOfflineEvalid(e.id) }

          val tuneAlgosMap: Map[Int, Algo] = tuneAlgos.map{ a => (a.id -> a) }.toMap

          // group by (loop, paramset)
          type AlgoGroupIndex = (Option[Int], Option[Int])

          val tuneAlgosGroup: Map[AlgoGroupIndex, Array[Algo]] = tuneAlgos.groupBy( a => (a.loop, a.paramset) )

          // get param of each group
          val tuneAlgosGroupParams: Map[AlgoGroupIndex, (Int, String)] = tuneAlgosGroup.map{ case (index, arrayOfAlgos) =>
            val algo = arrayOfAlgos(0) // just take 1, all algos of this group will have same params
            val algoInfo = algoInfos.get(algo.infoid).get
            val settings = Itemrec.Algorithms.displayParams(algoInfo, algo.params)

            (index -> (algo.id, settings))

          }

          // calculate avg
          val avgScores: Map[AlgoGroupIndex, String] = tuneAlgosGroup.mapValues{ arrayOfAlgos =>
            // check if all scores available
            val allAvailable = arrayOfAlgos.map(algo => tuneOfflineEvalResultsTestSetAlgoidMap.contains(algo.id)).reduceLeft( _ && _ )

            if (allAvailable) {
              val scores: Array[Double] = arrayOfAlgos.map(algo => tuneOfflineEvalResultsTestSetAlgoidMap(algo.id))

              (scores.sum / scores.size).toString
            } else {
              "N/A"
            }
          }

          val metricscorelist = avgScores.toSeq.sortBy(_._1).map{ case (k,v) =>
             Map(
              "algoautotuneid" -> toJson(tuneAlgosGroupParams(k)._1),
              "settingsstring"-> toJson(tuneAlgosGroupParams(k)._2),
              "score"-> toJson(v))
          }


          val metricscoreiterationlist = tuneOfflineEvals.map{ e =>
            tuneOfflineEvalResultsTestSet.filter( x => (x.evalid == e.id) ).sortBy(_.algoid).map{ r =>

              val algo = tuneAlgosMap(r.algoid)
              val algoInfo = algoInfos.get(algo.infoid).get

              Map(
                "algoautotuneid" -> toJson(r.algoid.toString),
                "settingsstring"-> toJson(Itemrec.Algorithms.displayParams(algoInfo, algo.params)),
                "score"-> toJson(r.score))

            }.toSeq
          }.toSeq

          val algoInfo = algoInfos.get(algo.infoid).get
          val metric = tuneMetrics(0)
          val splitter = tuneSplitters(0)
          val splitTrain = ((splitter.settings("trainingPercent").asInstanceOf[Double])*100).toInt
          val splitValidation = ((splitter.settings("validationPercent").asInstanceOf[Double])*100).toInt
          val splitTest = ((splitter.settings("testPercent").asInstanceOf[Double])*100).toInt
          val splitMethod = if (splitter.settings("timeorder").asInstanceOf[Boolean]) "time" else "random"
          val evalIteration = tuneOfflineEvals.size // NOTE: for autotune, number of offline eval is the iteration
          val engineinfoid = engines.get(engineid) map { _.infoid } getOrElse { "unkown-engine" }

          val status: String = (tune.starttime, tune.endtime) match {
            case (Some(x), Some(y)) => "completed"
            case (None, None) => "pending"
            case (Some(x), None) => "running"
            case _ => "error"
          }
          val starttime = tune.starttime map (x => timeFormat.print(x.withZone(DateTimeZone.forID("UTC")))) getOrElse ("-")
          val endtime = tune.endtime map (x => timeFormat.print(x.withZone(DateTimeZone.forID("UTC")))) getOrElse ("-")

          Ok(toJson(
            Map(
              "id" -> toJson(algoid.toString),
              "appid" -> toJson(appid.toString),
              "engineid" -> toJson(engineid.toString),
              "algo" -> toJson(Map(
                          "id" -> algo.id.toString,
                          "algoname" -> algo.name.toString,
                          "appid" -> appid.toString,
                          "engineid" -> algo.engineid.toString,
                          "algoinfoid" -> algo.infoid,
                          "algoinfoname" -> algoInfo.name,
                          "settingsstring" -> Itemrec.Algorithms.displayParams(algoInfo, algo.params)
                        )),
              "metric" -> toJson(Map(
                          "id" -> metric.id.toString,
                          "engineid" -> engineid.toString,
                          "engineinfoid" -> engineinfoid,
                          "metricsinfoid" -> metric.infoid,
                          "metricsname" -> (offlineEvalMetricInfos.get(metric.infoid) map { _.name } getOrElse ""),
                          "settingsstring" -> map_k_displayAllParams(metric.params)
                        )),
              "metricscorelist" -> toJson(metricscorelist),
              "metricscoreiterationlist" -> toJson(metricscoreiterationlist),

              "splittrain" -> toJson(splitTrain),
              "splitvalidation" -> toJson(splitValidation),
              "splittest" -> toJson(splitTest),
              "splitmethod" -> toJson(splitMethod),
              "evaliteration" -> toJson(evalIteration),

              "status" -> toJson(status),
              "starttime" -> toJson(starttime),
              "endtime" -> toJson(endtime)
            )
          ))

        } getOrElse {
          NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id."))))
        }
      } getOrElse {
        NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id."))))
      }
    } getOrElse {
      NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id."))))
    }

  }

  /** Applies the selected tuned algo's params to this algo
    * 
    * {{{
    * POST
    * JSON parameters:
    *   {
    *     "tunedalgoid" : <the tuned algo id. This algo's parameters will be used>
    *   }
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *    
    *   If error:
    *   BadRequest
    *   {
    *     "message" : <error message>
    *   }
    *
    *   If algo not found:
    *   NotFound
    *   {
    *     "message" : <error message>
    *   }
    *   If applied:
    *   Ok
    *
    * }}}
    *
    * @param appid the App ID
    * @param engineid the engine ID
    * @param algoid the algo ID to which the tuned parameters are applied
    */
  def algoAutotuningSelect(appid: Int, engineid: Int, algoid: Int) = withUser { user => implicit request =>

    // Apply POST request params of tunedalgoid to algoid
    // update the status of algoid from 'tuning' or 'tuned' to 'ready'

    val form = Form("tunedalgoid" -> number)

    form.bindFromRequest.fold(
      formWithError => {
        val msg = formWithError.errors(0).message // extract 1st error message only
        BadRequest(toJson(Map("message" -> toJson(msg))))
      },
      formData => {
        val tunedAlgoid = formData

        val orgAlgo = algos.get(algoid)
        val tunedAlgo = algos.get(tunedAlgoid)

        if ((orgAlgo == None) || (tunedAlgo == None)) {
          NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id."))))
        } else {
          val tunedAlgoParams = tunedAlgo.get.params ++ Map("tune" -> "manual")
          algos.update(orgAlgo.get.copy(
            params = tunedAlgoParams,
            status = "ready"
          ))

          Ok
        }
      }
    )
  }

  /** Returns a list of simulated evalulation for this engine
    *
    * {{{
    * GET
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If no sim evals:
    *   NoContent
    *
    *   If found:
    *   Ok
    *   [
    *     { "id" : <sim eval id>,
    *       "appid" : <app id>,
    *       "engineid" : <engine id>,
    *       "algolist" : [
    *                      { "id" : <algo id>,
    *                        "algoname" : <algo name>,
    *                        "appid" : <app id>,
    *                        "engineid" : <engine id>,
    *                        "algoinfoid" : <algo info id>,
    *                        "algoinfoname" : <algo info name>,
    *                        "settingsstring" : <algo setting string> 
    *                      }, ...
    *                    ],
    *       "status" : <sim eval status>,
    *       "starttime" : <sim eval create time>,
    *       "endtime" : <sim eval end time>
    *     }, ...
    *   ]
    * }}}
    *
    * @param appid the App ID
    * @param engineid the engine ID
    */
  def getSimEvalList(appid: Int, engineid: Int) = withUser { user => implicit request =>

    // get offlineeval for this engine
    val engineOfflineEvals = offlineEvals.getByEngineid(engineid) filter { e => (e.tuneid == None) }

    if (!engineOfflineEvals.hasNext) NoContent
    else {
      val resp = toJson(

        engineOfflineEvals.map { eval =>

          val status = (eval.starttime, eval.endtime) match {
            case (Some(x), Some(y)) => "completed"
            case (_, _) => "pending"
          }

          val evalAlgos = algos.getByOfflineEvalid(eval.id)

          val algolist = if (!evalAlgos.hasNext) JsNull
          else
            toJson(
              evalAlgos.map { algo =>
                val algoInfo = algoInfos.get(algo.infoid).get // TODO: what if couldn't get the algoInfo here?

                Map("id" -> algo.id.toString,
                     "algoname" -> algo.name,
                     "appid" -> appid.toString,
                     "engineid" -> algo.engineid.toString,
                     "algoinfoid" -> algo.infoid,
                     "algoinfoname" -> algoInfo.name,
                     "settingsstring" -> Itemrec.Algorithms.displayParams(algoInfo, algo.params)
                     )
              }.toSeq
              )

          val createtime = eval.createtime map (x => timeFormat.print(x.withZone(DateTimeZone.forID("UTC")))) getOrElse ("-")
          val starttime = eval.starttime map (x => timeFormat.print(x.withZone(DateTimeZone.forID("UTC")))) getOrElse ("-")
          val endtime = eval.endtime map (x => timeFormat.print(x.withZone(DateTimeZone.forID("UTC")))) getOrElse ("-")

          Map(
           "id" -> toJson(eval.id),
           "appid" -> toJson(appid),
           "engineid" -> toJson(eval.engineid),
           "algolist" -> algolist,
           "status" -> toJson(status),
           "starttime" -> toJson(createtime), // NOTE: use createtime here for test date
           "endtime" -> toJson(endtime)
           )
        }.toSeq

      )

      Ok(resp)

    }

  }


  val supportedMetricTypes = Set("map_k")
  // metrictype -> metrictypename
  val metricTypeNames = Map("map_k" -> "MAP@k")

  def map_k_displayAllParams(params: Map[String, Any]): String = {
    val displayNames: List[String] = List("k")
    val displayToParamNames: Map[String, String] = Map("k" -> "kParam")

    displayNames map (x => x + " = " + params(displayToParamNames(x))) mkString(", ")
  }

  /** Creates a simulated evalution
    *
    * {{{
    * POST
    * JSON parameters:
    *   {
    *     "appid" : <app id>,
    *     "engineid" : <engine id>,
    *     "algo[i]" : <algo id>,
    *     "metrics[i]" : <metric info id>,
    *     "metricsSettings[i]" : <metric setting>,
    *     "splittrain" : <training set split percentage 1 to 100>,
    *     "splittest" : <test set split percentage 1 to 100>,
    *     "splitmethod" : <split method string: randome, time>
    *     "evaliteration" : <number of iterations>
    *   }
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If error:
    *   BadRequest
    *   {
    *     "message" : <error message>
    *   }
    *   
    *   If created:
    *   Ok
    *
    * }}}
    * @param appid the App ID
    * @param engineid the engine ID
    */
  def createSimEval(appid: Int, engineid: Int) = withUser { user => implicit request =>
    // request payload example
    // {"appid":"1","engineid":"17","algo[0]":"12","algo[1]":"13","metrics[0]":"map_k","metricsSettings[0]":"5","metrics[1]":"map_k","metricsSettings[1]":"10"}
    // {"appid":"17","engineid":"22","algo[0]":"146","metrics[0]":"map_k","metricsSettings[0]":"20","splittrain":"71","splittest":"24","splitmethod":"time","evaliteration":"3"}
    val simEvalForm = Form(tuple(
      "appid" -> number,
      "engineid" -> number,
      "algo" -> list(number), // algo id
      "metrics" -> (list(text) verifying ("Invalid metrics types.", x => (x.toSet -- supportedMetricTypes).isEmpty)),
      "metricsSettings" -> list(text),
      "splittrain" -> number(1, 100),
      "splittest" -> number(1, 100),
      "splitmethod" -> text,
      "evaliteration" -> (number verifying ("Number of Iteration must be greater than 0", x => (x > 0)))
    ))

    simEvalForm.bindFromRequest.fold(
      formWithError => {
        //println(formWithError.errors)
        val msg = formWithError.errors(0).message // extract 1st error message only
        BadRequest(toJson(Map("message" -> toJson(msg))))
      },
      formData => {
        val (appId, engineId, algoIds, metricTypes, metricSettings, splitTrain, splitTest, splitMethod, evalIteration) = formData

        // get list of algo obj
        val optAlgos: List[Option[Algo]] = algoIds map {algoId => algos.get(algoId)}

        if (!optAlgos.contains(None)) {
          val listOfAlgos: List[Algo] = optAlgos map (x => x.get)

          SimEval.createSimEval(engineId, listOfAlgos, metricTypes, metricSettings,
          splitTrain, 0, splitTest, splitMethod, evalIteration, None)

          WS.url(settingsSchedulerUrl+"/users/"+user.id+"/sync").get()

          Ok
        } else {
          BadRequest(toJson(Map("message" -> toJson("Invalid algo ids."))))
        }
      }
    )
  }

  /** Requests to stop and delete simulated evalution (including running/pending job)
    * 
    * {{{
    * DELETE
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If not found:
    *   NotFound
    *   {
    *     "message" : <error message>
    *   }
    *
    *   If error:
    *   InternalServerError
    *   {
    *     "message" : <error message>
    *   }
    *
    *   If deleted:
    *   Ok
    *   {
    *      "message" : "Offline evaluation ID $id has been deleted"
    *   }
    * }}}
    *
    * @param appid the App ID
    * @param engineid the engine ID
    * @param id the offline evaluation ID
    *
    */
  def removeSimEval(appid: Int, engineid: Int, id: Int) = withUser { user => implicit request =>

    // remove algo, remove metric, remove offline eval

    offlineEvals.get(id) map { oe =>

      /** Make sure to unset offline eval's creation time to prevent scheduler from picking up */
      offlineEvals.update(oe.copy(createtime = None))

      /** Deletion of app data and model data could take a while */
      val timeout = play.api.libs.concurrent.Promise.timeout("Scheduler is unreachable. Giving up.", concurrent.duration.Duration(10, concurrent.duration.MINUTES))

      val complete = Helper.stopAndDeleteSimEvalScheduler(appid, engineid, oe.id)

      /** Detect timeout (10 minutes by default) */
      Async {
        concurrent.Future.firstCompletedOf(Seq(complete, timeout)).map {
          case r: SimpleResult => {
            if (r.header.status == http.Status.OK) {
              Helper.deleteOfflineEval(oe.id, keepSettings=false)
            }
            r
          }
          case t: String => InternalServerError(Json.obj("message" -> t))
        }
      }
    } getOrElse {
      NotFound(Json.obj("message" -> s"Offline evaluation ID $id does not exist"))
    }
  }

  /** Returns the simulated evaluation report
    *
    * {{{
    * GET
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If not found:
    *   {
    *     "message" : <error message>
    *   }
    *
    *   If found:
    *   Ok
    *   {
    *     "id" : <sim eval id>,
    *     "appid" : <app id>,
    *     "engineid" : <engine id>,
    *     "algolist" : [
    *                    { 
    *                      "id" : <algo id>,
    *                      "algoname" : <algo name>,
    *                      "appid" : <app id>,
    *                      "engineid" : <engine id>,
    *                      "algoinfoid" : <algo info id>,
    *                      "algoinfoname" : <algo info name>,
    *                      "settingsstring" : <algo setting string> 
    *                    }, ...
    *                  ],
    *     "metricslist" : [
    *                       { 
    *                         "id" : <metric id>,
    *                         "engineid" : <engine id>,
    *                         "engineinfoid" : <engine info id>,
    *                         "metricsinfoid" : <metric info id>,
    *                         "metricsname" : <metric name>,
    *                         "settingsstring" : <metric setting string> 
    *                       }, ...
    *                     ],
    *     "metricscorelist" : [
    *                           { 
    *                             "algoid" : <algo id>,
    *                             "metricsid" : <metric id>,
    *                             "score" : <average score> 
    *                           }, ...
    *                         ],
    *     "metricscoreiterationlist" : [
    *                                    { 
    *                                      "algoid" : <algo id>,
    *                                      "metricsid" : <metric id>,
    *                                      "score" : <score of this iteration>
    *                                    }, ...
    *                                  ],
    *     "splittrain" : <training set split percentage 1 to 100>,
    *     "splittest" : <test set split percentage 1 to 100>,
    *     "splitmethod" : <split method string: randome, time>
    *     "evaliteration" : <number of iterations>,
    *     "status" : <eval status>,
    *     "starttime" : <start time>,
    *     "endtime" : <end time>
    *   }
    * }}}
    *
    * @param appid the App ID
    * @param engineid the engine ID
    * @param id the offline evaluation ID
    */
  def getSimEvalReport(appid: Int, engineid: Int, id: Int) = withUser { user => implicit request =>
    
    // get offlineeval for this engine
    val optOffineEval: Option[OfflineEval] = offlineEvals.get(id)

    optOffineEval map { eval =>
      val status = "completed"

      // TODO: add assertion that eval.starttime, eval.endtime can't be None

      val evalAlgos = algos.getByOfflineEvalid(eval.id).toArray

      val algolist =
        if (evalAlgos.isEmpty) // TODO: shouldn't expect this happen
          JsNull
        else {
          toJson(
            evalAlgos.map { algo =>

              val algoInfo = algoInfos.get(algo.infoid).get // TODO: what if couldn't get the algoInfo here?

              Map("id" -> algo.id.toString,
                  "algoname" -> algo.name,
                  "appid" -> appid.toString,
                  "engineid" -> algo.engineid.toString,
                  "algoinfoid" -> algo.infoid,
                  "algoinfoname" -> algoInfo.name,
                  "settingsstring" -> Itemrec.Algorithms.displayParams(algoInfo, algo.params)
                  )
            }.toSeq
          )
        }

      val evalMetrics = offlineEvalMetrics.getByEvalid(eval.id).toArray

      val metricslist =
        if (evalMetrics.isEmpty) // TODO: shouldn't expect this happen
          JsNull
        else {
          toJson(
            evalMetrics.map { metric =>
              Map("id" -> metric.id.toString,
                  "engineid" -> engineid.toString,
                  "engineinfoid" -> "itemrec", // TODO: hardcode now, should get it from engine db
                  "metricsinfoid" -> metric.infoid,
                  "metricsname" -> (offlineEvalMetricInfos.get(metric.infoid) map { _.name } getOrElse ""),
                  "settingsstring" -> map_k_displayAllParams(metric.params)
                  )
            }.toSeq
          )
        }

      val evalResults = offlineEvalResults.getByEvalid(eval.id).toArray

      val metricscorelist = (for (algo <- evalAlgos; metric <- evalMetrics) yield {

        val results = evalResults
          .filter( x => ((x.metricid == metric.id) && (x.algoid == algo.id)) )
          .map(x => x.score)

        val num = results.length
        val avg = if ((results.isEmpty) || (num != eval.iterations)) "N/A" else ((results.reduceLeft( _ + _ ) / num).toString)

        Map("algoid" -> algo.id.toString,
            "metricsid" -> metric.id.toString,
            "score" -> avg.toString
            )
      }).toSeq

      val metricscoreiterationlist = (for (i <- 1 to eval.iterations) yield {
        val defaultScore = (for (algo <- evalAlgos; metric <- evalMetrics) yield {
          ((algo.id, metric.id) -> "N/A") // default score
        }).toMap

        val evalScore = evalResults.filter( x => (x.iteration == i) ).map(r =>
          ((r.algoid, r.metricid) -> r.score.toString)).toMap

        // overwrite defaultScore with evalScore
        (defaultScore ++ evalScore).map{ case ((algoid, metricid), score) =>
          Map("algoid" -> algoid.toString, "metricsid" -> metricid.toString, "score" -> score)
        }
      }).toSeq

      val starttime = eval.starttime map (x => timeFormat.print(x.withZone(DateTimeZone.forID("UTC")))) getOrElse ("-")
      val endtime = eval.endtime map (x => timeFormat.print(x.withZone(DateTimeZone.forID("UTC")))) getOrElse ("-")

      // get splitter data
      val splitters = offlineEvalSplitters.getByEvalid(eval.id)

      if (splitters.hasNext) {
        val splitter = splitters.next

        val splitTrain = ((splitter.settings("trainingPercent").asInstanceOf[Double])*100).toInt
        val splitTest = ((splitter.settings("testPercent").asInstanceOf[Double])*100).toInt
        val splitMethod = if (splitter.settings("timeorder").asInstanceOf[Boolean]) "time" else "random"

        if (splitters.hasNext) {
          val message = "More than one splitter found for this Offline Eval ID:" + eval.id
          NotFound(toJson(Map("message" -> toJson(message))))
        } else {
          Ok(toJson(
            Map(
              "id" -> toJson(eval.id),
               "appid" -> toJson(appid.toString),
               "engineid" -> toJson(eval.engineid),
               "algolist" -> algolist,
               "metricslist" -> metricslist,
               "metricscorelist" -> toJson(metricscorelist),
               "metricscoreiterationlist" -> toJson(metricscoreiterationlist),
               "splittrain" -> toJson(splitTrain),
               "splittest" -> toJson(splitTest),
               "splitmethod" -> toJson(splitMethod),
               "evaliteration" -> toJson(eval.iterations),
               "status" -> toJson(status),
               "starttime" -> toJson(starttime),
               "endtime" -> toJson(endtime)
               )
          ))
        }
      } else {
        val message = "No splitter found for this Offline Eval ID:" + eval.id
        NotFound(toJson(Map("message" -> toJson(message))))
      }
    } getOrElse {
      NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or simeval id."))))
    }

  }

  /** Deploys a list of algorithms (also undeploys any existing deployed algorithms)
    * The status of deployed algorithms change to "deployed"
    * 
    * {{{
    * POST
    * JSON parameters:
    *   {
    *     "algoidlist" : [ array of algo ids ]
    *   }
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If done:
    *   Ok
    * 
    * }}}
    * 
    * @param appid the App ID
    * @param engineid the engine ID
    */
  def algoDeploy(appid: Int, engineid: Int) = withUser { user => implicit request =>
    val deployForm = Form(
      "algoidlist" -> list(number)
    )
    deployForm.bindFromRequest.fold(
      formWithErrors => Ok,
      form => {
        algos.getDeployedByEngineid(engineid) foreach { algo =>
          algos.update(algo.copy(status = "ready"))
        }
        form foreach { id =>
          algos.get(id) foreach { algo =>
            algos.update(algo.copy(status = "deployed"))
          }
        }
        WS.url(settingsSchedulerUrl+"/users/"+user.id+"/sync").get()
        Ok
      }
    )
  }

  /** Undeploys all deployed algorithms
    * {{{
    * POST
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If done:
    *   Ok
    * }}}
    *
    * @param appid the App ID
    * @param engineid the engine ID
    */
  def algoUndeploy(appid: Int, engineid: Int) = withUser { user => implicit request =>

    algos.getDeployedByEngineid(engineid) foreach { algo =>
      algos.update(algo.copy(status = "ready"))
    }
    WS.url(settingsSchedulerUrl+"/users/"+user.id+"/sync").get()
    Ok
  }

  /** Requests to train model now
    * {{{
    * POST
    * JSON parameters:
    *   None
    * JSON response:
    *   If not authenticated:
    *   Forbidden
    *   {
    *     "message" : "Haven't signed in yet."
    *   }
    *
    *   If error:
    *   InternalServerError
    *   {
    *     "message" : <error message>
    *   }
    *
    *   If done:
    *   Ok
    *   {
    *     "message" : <message from scheduler>
    *   }
    * }}}
    *
    * @param appid the App ID
    * @param engineid the engine ID
    */
  def algoTrainNow(appid: Int, engineid: Int) = withUser { user => implicit request =>
    // No extra param required
    val timeout = play.api.libs.concurrent.Promise.timeout("Scheduler is unreachable. Giving up.", concurrent.duration.Duration(10, concurrent.duration.MINUTES))
    val request = WS.url(s"${settingsSchedulerUrl}/apps/${appid}/engines/${engineid}/trainoncenow").get() map { r =>
      Ok(Json.obj("message" -> (r.json \ "message").as[String]))
    } recover {
      case e: Exception => InternalServerError(Json.obj("message" -> e.getMessage()))
    }

    /** Detect timeout (10 minutes by default) */
    Async {
      concurrent.Future.firstCompletedOf(Seq(request, timeout)).map {
        case r: SimpleResult => r
        case t: String => InternalServerError(Json.obj("message" -> t))
      }
    }
  }
}

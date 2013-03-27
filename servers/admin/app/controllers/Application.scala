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
import play.api.libs.json.Json._
import play.api.libs.json.{JsNull}
import play.api.libs.ws.WS
import play.api.Play.current

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

import com.github.nscala_time.time.Imports._

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
  val algos = config.getSettingsAlgos()
  val algoInfos = config.getSettingsAlgoInfos()
  val offlineEvals = config.getSettingsOfflineEvals()
  val offlineEvalMetrics = config.getSettingsOfflineEvalMetrics()
  val offlineEvalResults = config.getSettingsOfflineEvalResults()

  /** PredictionIO Commons modeldata */
  val itemRecScores = config.getModeldataItemRecScores()

  /** PredictionIO Commons appdata */
  val appDataUsers = config.getAppdataUsers()
  val appDataItems = config.getAppdataItems()
  val appDataU2IActions = config.getAppdataU2IActions()

  /** PredictionIO Commons training set appdata */
  val trainingSetUsers = config.getAppdataTrainingUsers()
  val trainingSetItems = config.getAppdataTrainingItems()
  val trainingSetU2IActions = config.getAppdataTrainingU2IActions()

  /** PredictionIO Commons test set appdata */
  val testSetUsers = config.getAppdataTestUsers()
  val testSetItems = config.getAppdataTestItems()
  val testSetU2IActions = config.getAppdataTestU2IActions()

  /** PredictionIO Output */
  val algoOutputSelector = new AlgoOutputSelector(algos)

  /** */
  val timeFormat = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss a z")

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


  /* Authenticate Administrator
   * Method: POST
   * Request JSON Params:
   * 	adminEmail - string
   * 	adminPassword - string
   * 	adminRemember - "on" or not exist
   */
  def signin = Action { implicit request =>
    val loginForm = Form(
      tuple(
        "adminEmail" -> text,
        "adminPassword" -> text,
        "adminRemember" -> optional(text)
      ) verifying ("Invalid email or password", result => result match {
        case (adminEmail, adminPassword, adminRemember) => users.authenticateByEmail(adminEmail, adminPassword) map { _ => true } getOrElse false
      })
    )

    loginForm.bindFromRequest.fold(
      formWithErrors => Forbidden(toJson(Map("message" -> toJson("Incorrect Email or Password.")))),
      form => {
        val user = users.getByEmail(form._1).get
        Ok(toJson(Map(
          "adminName" -> "%s %s".format(user.firstName, user.lastName.getOrElse("")),
          "adminEmail" -> user.email
        ))).withSession(Security.username -> user.email)
      }
    )
  }

  def signout = Action {
    Ok.withNewSession
  }

  /* Get Authenticated Administrator Info
   * Method: GET
   *  Request JSON Params: None (read session cookie for auth)
   */
  def getAuth = withUser { user => implicit request =>
    // If NOT authenticated
    /*
     * Forbidden(toJson(Map("message" -> toJson("Haven't signed in yet."))))
     */

    // If authenticated
    Ok(toJson(Map(
      "id" -> user.id.toString,
      "adminName" -> (user.firstName + user.lastName.map(" "+_).getOrElse("")),
      "adminEmail" -> user.email
    )))
  }

  def getApplist = withUser { user => implicit request =>
    // If NOT authenticated
    /*
     * Forbidden(toJson(Map("message" -> toJson("Haven't signed in yet."))))
     */

    // if No App yet
    /*
     * NoContent
     */
    val userApps = apps.getByUserid(user.id)
    if (!userApps.hasNext) NoContent
    else {
      Ok(toJson(userApps.map { app =>
        Map("id" -> app.id.toString, "appName" -> app.display)
      }.toSeq))
    }
  }

  def getAppDetails(id: String) = withUser { user => implicit request =>
    // If NOT authenticated
    /*
     * Forbidden(toJson(Map("message" -> toJson("Haven't signed in yet."))))
     */

    // if No such app id
    /*
     *  NotFound(toJson(Map("message" -> toJson("invalid app id"))))
     */
    apps.getByIdAndUserid(id.toInt, user.id) map { app =>
      val numUsers = appDataUsers.countByAppid(app.id)
      val numItems = appDataItems.countByAppid(app.id)
      val numU2IActions = appDataU2IActions.countByAppid(app.id)
      Ok(toJson(Map(
        "id" -> toJson(app.id), // app id
        "updatedTime" -> toJson(timeFormat.print(DateTime.now.withZone(DateTimeZone.forID("UTC")))),
        "nUsers" -> toJson(numUsers),
        "nItems" -> toJson(numItems),
        "nU2IActions" -> toJson(numU2IActions),
        "apiEndPoint" -> toJson("http://yourhost.com:123/appid12"),
        "appkey" -> toJson(app.appkey))))
    } getOrElse {
      NotFound(toJson(Map("message" -> toJson("invalid app id"))))
    }
  }

  /**
   * return JSON data in following format:
   *
   * Ok(toJson(Map(
   *   "id" -> toJson("appid2"), // appid
   *   "enginelist" -> toJson(Seq(
   *     Map(
   *       "id" -> "e1234",
   *       "engineName" -> "Engine Name 1",
   *       "enginetype_id" -> "itemrec"),
   *     Map(
   *       "id" -> "e2234",
   *       "engineName" -> "Engine Name 2",
   *       "enginetype_id" -> "itemsim"))))))
   */
  def getAppEnginelist(id: String) = withUser { user => implicit request =>

    // TODO: check this user owns this app

    // TODO: check id is Int
    val appEngines = engines.getByAppid(id.toInt)

    if (!appEngines.hasNext) NoContent
    else
      Ok(toJson(Map(
        "id" -> toJson(id),
        "enginelist" -> toJson((appEngines map { eng =>
          Map("id" -> eng.id.toString, "engineName" -> eng.name,"enginetype_id" -> eng.enginetype)
        }).toSeq)
      )))

  }

  /* Required param: id (app_id) */
  def getApp(id: String) = withUser { user => implicit request =>
    val app = apps.getByIdAndUserid(id.toInt, user.id).get
    Ok(toJson(Map(
      "id" -> app.id.toString, // app id
      "appName" -> app.display)))
  }

  /*
   * createApp
   * JSON Param: appName
   */
  def createApp = withUser { user => implicit request =>
    // If NOT authenticated
    /*
     * Forbidden(toJson(Map("message" -> toJson("Haven't signed in yet."))))
     */

    // if creation failed
    /*
     * BadRequest(toJson(Map("message" -> toJson("invalid character for app name."))))
     */
    val bad = BadRequest(toJson(Map("message" -> toJson("invalid character for app name."))))

    request.body.asJson map { js =>
      val appName = (js \ "appName").asOpt[String]
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
          Ok(toJson(Map(
            "id" -> appid.toString,
            "appName" -> an
          )))
        }
      } getOrElse bad
    } getOrElse bad
  }

  def removeApp(id: String) = withUser { user => implicit request =>
    // If NOT authenticated
    /*
     * Forbidden(toJson(Map("message" -> toJson("Haven't signed in yet."))))
     */

    // if No such app id
    /*
     *  NotFound(toJson(Map("message" -> toJson("invalid app id"))))
     */

    val appid = id.toInt
    deleteApp(appid, keepSettings=false)

    //send deleteAppDir(appid) request to scheduler
    WS.url(config.settingsSchedulerUrl+"/apps/"+id+"/delete").get()

    Logger.info("Delete app ID "+appid)
    apps.deleteByIdAndUserid(appid, user.id)

    Ok

    //BadRequest(toJson(Map("message" -> toJson("This feature will be available soon."))))
  }
  def eraseAppData(id: String) = withUser { user => implicit request =>
    // If NOT authenticated
    /*
     * Forbidden(toJson(Map("message" -> toJson("Haven't signed in yet."))))
     */

    // if No such app id
    /*
     *  NotFound(toJson(Map("message" -> toJson("invalid app id"))))
     */

    val appid = id.toInt
    deleteApp(appid, keepSettings=true)

    //send deleteAppDir(appid) request to scheduler
    WS.url(config.settingsSchedulerUrl+"/apps/"+id+"/delete").get()

    Ok
    //BadRequest(toJson(Map("message" -> toJson("This feature will be available soon."))))
  }

  /* List all available/installable engine types in the system */
  def getEngineTypeList = Action {
    Ok(toJson(Seq(
      Map(
        "id" -> "itemrec",
        "enginetypeName" -> "Item Recommendation Engine",
        "description" -> """
    						<h6>Recommend interesting items to each user personally.</h6>
				            <p>Sample Use Cases</p>
				            <ul>
				                <li>recommend top N items to users personally</li>
				                <li>predict users' future preferences</li>
				                <li>help users to discover new topics they may be interested in</li>
				                <li>personalize content</li>
				                <li>optimize sales</li>
				            </ul>
    						"""),
      Map(
        "id" -> "itemsim",
        "enginetypeName" -> "Items Similarity Prediction Engine",
        "description" -> """
    		            	<h6>Discover similar items.</h6>
				            <p>Sample Use Cases</p>
				            <ul>
				                <li>predict what else would a user like if this user likes a,
				                    i.e. "People who like this also like...."</li>
				                <li>automatic item grouping</li>
				            </ul>
    						"""))))
  }

  /* List all available/installable algorithm type of a specific engine type
   * Required param: id  (i.e. enginetype_id)
   *  */
  def getEngineTypeAlgoList(id: String) = Action {
    Ok(toJson(
      Map(
        "enginetypeName" -> toJson("Item Recommendation Engine"),
       /* "algotypelist" -> toJson(Seq(
          Map(
            "id" -> "pdio-knnitembased",
            "algotypeName" -> algoTypeNames("pdio-knnitembased"), //"Item-based Similarity (kNN) ",
            "description" -> "This item-based k-NearestNeighbor algorithm predicts user preferences based on previous behaviors of users on similar items.",
            "req" -> "Hadoop",
            "datareq" -> "U2I Actions such as Like, Buy and Rate.")
            )) */
        "algotypelist" -> toJson(
          (algoInfos.getByEngineType("itemrec") map { algoInfo =>
            Map(
              "id" -> toJson(algoInfo.id),
              "algotypeName" -> toJson(algoInfo.name),
              "description" -> toJson(algoInfo.description.getOrElse("")),
              "req" -> toJson(algoInfo.techreq),
              "datareq" -> toJson(algoInfo.datareq)
              )

          }).toSeq )
         )

     ))
  }

   /* List all metrics type of a specific engine type
   * Required param: id  (i.e. enginetype_id)
   *  */
  def getEngineTypeMetricsTypeList(id: String) = Action {
   Ok(toJson(
      Map(
        "enginetypeName" -> toJson("Item Recommendation Engine"),
        "metricslist" -> toJson(Seq(
								          toJson(Map(
								            "id" -> toJson("map_k"),
								            "metricsName" -> toJson("MAP@k"),
								            "metricsLongName" -> toJson("Mean Average Precision"),
								            "settingFields" -> toJson(Map(
								            					"k" -> "int"
								            					))
								          ))
								          /*
								          toJson(Map(
								            "id" -> toJson("ndgc_k"),
								            "metricsName" -> toJson("MAP@k"),
								            "metricsLongName" -> toJson("Normalized Discounted Cumulative Gain"),
								            "settingFields" -> toJson(Map(
								            					"k" -> "int"
								            					))
								          ))*/
        						))
      )))
  }

  def getEngine(app_id: String, id: String) = withUser { user => implicit request =>
    // If NOT authenticated
    /*
     * Forbidden(toJson(Map("message" -> toJson("Haven't signed in yet."))))
     */

    // TODO: check this user owns this app

    // TODO: check app_id and id is int
    val engine = engines.get(id.toInt)

    engine map { eng: Engine =>
      val modelDataExist: Boolean = eng.enginetype match {
        case "itemrec" => try { itemRecScores.existByAlgo(algoOutputSelector.itemRecAlgoSelection(eng)) } catch { case e: RuntimeException => false }
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
            (Await.result(WS.url(s"${config.settingsSchedulerUrl}/apps/${eng.appid}/engines/${eng.id}/algos/${algo.get.id}/status").get(), scala.concurrent.duration.Duration(5, SECONDS)).json \ "status").as[String] match {
              case "jobrunning" => "firsttraining"
              case _ => "nomodeldata"
            }
          } catch {
            case e: java.net.ConnectException => "nomodeldatanoscheduler"
          }
        else
          try {
            (Await.result(WS.url(s"${config.settingsSchedulerUrl}/apps/${eng.appid}/engines/${eng.id}/algos/${algo.get.id}/status").get(), scala.concurrent.duration.Duration(5, SECONDS)).json \ "status").as[String] match {
              case "jobrunning" => "training"
              case _ => "running"
            }
          } catch {
            case e: java.net.ConnectException => "runningnoscheduler"
          }
      Ok(obj(
        "id" -> eng.id.toString, // engine id
        "enginetype_id" -> eng.enginetype,
        "app_id" -> eng.appid.toString,
        "engineName" -> eng.name,
        "engineStatus" -> engineStatus))
    } getOrElse {
      // if No such app id
      NotFound(toJson(Map("message" -> toJson("Invalid app id or engine id."))))
    }

  }


  val supportedEngineTypes: List[String] = List("itemrec") // TODO: only itemrec is supported for now...
  val enginenameConstraint = Constraints.pattern("""\b[a-zA-Z][a-zA-Z0-9_-]*\b""".r, "constraint.enginename", "Engine names should only contain alphanumerical characters, underscores, or dashes. The first character must be an alphabet.")
  /*
   * createEngine
   * JSON request params:
   * 	app_id - app id
   * 	enginetype_id - engine type
   * 	engineName - inputted engine name
   */
  def createEngine(app_id: String) = withUser { user => implicit request =>
    val engineForm = Form(tuple(
      "app_id" -> number,
      "enginetype_id" -> (text verifying("This feature will be available soon.", e => supportedEngineTypes.contains(e))),
      "engineName" -> (text verifying("Please name your engine.", enginename => enginename.length > 0)
                          verifying enginenameConstraint)
    ) verifying("Engine name must be unique.", f => !engines.existsByAppidAndName(f._1, f._3)))

    // If NOT authenticated
    /*
     * Forbidden(toJson(Map("message" -> toJson("Haven't signed in yet."))))
     */

    // if No such app id
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

        val engineId = engines.insert(Engine(
          id = -1,
          appid = fappid,
          name = enginename,
          enginetype = enginetype,
          itypes = None, // NOTE: default None (means all itypes)
          settings = Itemrec.Engine.defaultSettings // TODO: depends on enginetype
        ))

        // automatically create default algo
        val defaultAlgoType = "mahout-itembased" // TODO: get it from engineInfo
        val defaultAlgo = Algo(
          id = -1,
          engineid = engineId,
          name = "Default-Algo", // TODO: get it from engineInfo
          infoid = defaultAlgoType,
          deployed = true, // default true
          command = "",
          params = algoInfos.get(defaultAlgoType).get.paramdefaults,
          settings = Map(), // no use for now
          modelset = false, // init value
          createtime = DateTime.now,
          updatetime = DateTime.now,
          offlineevalid = None
        )

        val algoId = algos.insert(defaultAlgo)

        WS.url(config.settingsSchedulerUrl+"/users/"+user.id+"/sync").get()

        Ok(toJson(Map(
          "id" -> engineId.toString, // engine id
          "enginetype_id" -> "itemrec",
          "app_id" -> fappid.toString,
          "engineName" -> enginename)))
      }
    )

  }

  def removeEngine(app_id: String, engine_id: String) = withUser { user => implicit request =>
    // If NOT authenticated
    /*
     * Forbidden(toJson(Map("message" -> toJson("Haven't signed in yet."))))
     */

    // if No such app id
    /*
     *  NotFound(toJson(Map("message" -> toJson("invalid app id"))))
     */

    val appid = app_id.toInt
    val engineid = engine_id.toInt

    deleteEngine(engineid, keepSettings=false)

    //send deleteAppDir(appid) request to scheduler
    WS.url(s"${config.settingsSchedulerUrl}/apps/${app_id}/engines/${engine_id}/delete").get()

    Logger.info("Delete Engine ID "+engine_id)
    engines.deleteByIdAndAppid(engineid, appid)

    Ok    // Ok
  }

  def getAvailableAlgoList(app_id: String, engine_id: String) = withUser { user => implicit request =>
    /* sample output
    Ok(toJson(Seq(
      Map(
        "id" -> "algoid_13213",
        "algoName" -> "algo-test-sim-correl=12",
        "app_id" -> "appid1234",
        "engine_id" -> "engid33333",
        "algotype_id" -> "pdio-knnitembased",
        "algotypeName" -> "Item-based Similarity (kNN) ",
        "status" -> "ready",
        "updatedTime" -> "04-23-2012 12:21:33"),

      Map(
      	"id" -> "algoid_13213",
        "algoName" -> "algo-test-mf-gamma=0.1,sigma=8",
        "app_id" -> "appid1234",
        "engine_id" -> "engid33333",
        "algotype_id" -> "pdio-knnitembased",
        "algotypeName" -> "Non-negative Matrix Factorization",
        "status" -> "autotuning",
        "updatedTime" -> "04-23-2012 12:21:23"),

      Map(
        "id" -> "algoid_3213",
        "algoName" -> "algo-test-mf-gamma=0.5,sigma=4",
        "app_id" -> "appid765",
        "engine_id" -> "engid33333",
        "algotype_id" -> "pdio-knnitembased",
        "algotypeName" -> "Non-negative Matrix Factorization",
        "status" -> "ready",
        "updatedTime" -> "04-23-2012 12:21:23")
     )))
    */

     // TODO: verifying this user owns app_id and engine_id

     // TODO: check engine_id is int
     val engineAlgos = algos.getByEngineid(engine_id.toInt)

     if (!engineAlgos.hasNext) NoContent
     else
       Ok(toJson( // NOTE: only display undeployed algo without offlinevalid
         (engineAlgos filter { algo => (algo.deployed == false) && (algo.offlineevalid == None) } map { algo =>
           Map("id" -> algo.id.toString,
               "algoName" -> algo.name,
               "app_id" -> app_id, // TODO: should algo db store appid and get it from there?
               "engine_id" -> algo.engineid.toString,
               "algotype_id" -> algo.infoid,
               "algotypeName" -> algoInfos.get(algo.infoid).get.name,
               "status" -> "ready", // TODO
               "updatedTime" -> timeFormat.print(algo.updatetime.withZone(DateTimeZone.forID("UTC")))
               )
         }).toSeq
       ))


  }

  def getAvailableAlgo(app_id: String, engine_id: String, id: String) = withUser { user => implicit request =>
    /* sample output
    Ok(toJson(
      Map(
        "id" -> "algoid_13213",
        "algoName" -> "algo-test-sim-correl=12",
        "app_id" -> "appid1234",
        "engine_id" -> "engid33333",
        "algotype_id" -> "pdio-knnitembased",
        "algotypeName" -> "Item-based Similarity (kNN) ",
        "status" -> "ready",
        "createdTime" -> "04-23-2012 12:21:33",
        "updatedTime" -> "04-23-2012 12:21:33"
        )
     ))
     */
    // TODO: check this user owns this appid + engineid + algoid

    val optAlgo: Option[Algo] = algos.get(id.toInt)

    optAlgo map { algo =>
      Ok(toJson(Map(
          "id" -> algo.id.toString, // algo id
          "algoName" -> algo.name,
          "app_id" -> app_id, // TODO
          "engine_id" -> algo.engineid.toString,
          "algotype_id" -> algo.infoid,
          "algotypeName" -> algoInfos.get(algo.infoid).get.name,
          "status" -> "ready", // default status
          "createdTime" -> timeFormat.print(algo.createtime.withZone(DateTimeZone.forID("UTC"))),
          "updatedTime" -> timeFormat.print(algo.updatetime.withZone(DateTimeZone.forID("UTC")))
        )))
    } getOrElse {
      NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id."))))
    }
  }

  val supportedAlgoTypes: List[String] = List(
      "pdio-knnitembased",
      "pdio-latestrank",
      "pdio-randomrank",
      "mahout-itembased",
      "mahout-parallelals",
      "mahout-knnuserbased",
      "mahout-thresholduserbased",
      "mahout-slopeone",
      "mahout-alswr",
      "mahout-svdsgd",
      "mahout-svdplusplus"
  )

  def createAvailableAlgo(app_id: String, engine_id: String) = withUser { user => implicit request =>
    // request payload
    //{"algotype_id":"pdio-knnitembased","algoName":"test","app_id":"1","engine_id":"12"}

    // If NOT authenticated
    /*
     * Forbidden(toJson(Map("message" -> toJson("Haven't signed in yet."))))
     */

    // if No such app id or engine id
    /*
     *  NotFound(toJson(Map("message" -> toJson("invalid app id or engine id"))))
     */

    // if invalid algo name
    /*
     *  BadRequest(toJson(Map("message" -> toJson("invalid algo name"))))
     */

    val createAlgoForm = Form(tuple(
      "algotype_id" -> (nonEmptyText verifying("This feature will be available soon.", t => supportedAlgoTypes.contains(t))),
      "algoName" -> (text verifying("Please name your algo.", name => name.length > 0)
                          verifying enginenameConstraint), // same name constraint as engine
      "app_id" -> number,
      "engine_id" -> number
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
            deployed = false,
            command = "",
            params = algoInfo.paramdefaults,
            settings = Map(), // no use for now
            modelset = false, // init value
            createtime = DateTime.now,
            updatetime = DateTime.now,
            offlineevalid = None
          )

          val algoId = algos.insert(newAlgo)

          Ok(toJson(Map(
            "id" -> algoId.toString, // algo id
            "algoName" -> newAlgo.name,
            "app_id" -> appId.toString,
            "engine_id" -> newAlgo.engineid.toString,
            "algotype_id" -> algoType,
            "algotypeName" -> algoInfos.get(algoType).get.name,
            "status" -> "ready", // default status
            "createdTime" -> timeFormat.print(newAlgo.createtime.withZone(DateTimeZone.forID("UTC"))),
            "updatedTime" -> timeFormat.print(newAlgo.updatetime.withZone(DateTimeZone.forID("UTC")))
          )))
        }

      }
    )

  }

  /**
   * delete appdata DB of this appid
   */
  def deleteAppData(appid: Int) = {
    Logger.info("Delete appdata for app ID "+appid)
    appDataUsers.deleteByAppid(appid)
    appDataItems.deleteByAppid(appid)
    appDataU2IActions.deleteByAppid(appid)
  }

  def deleteTrainingSetData(evalid: Int) = {
    Logger.info("Delete training set for offline eval ID "+evalid)
    trainingSetUsers.deleteByAppid(evalid)
    trainingSetItems.deleteByAppid(evalid)
    trainingSetU2IActions.deleteByAppid(evalid)
  }

  def deleteTestSetData(evalid: Int) = {
    Logger.info("Delete test set for offline eval ID "+evalid)
    testSetUsers.deleteByAppid(evalid)
    testSetItems.deleteByAppid(evalid)
    testSetU2IActions.deleteByAppid(evalid)
  }

  def deleteModelData(algoid: Int) = {
    val algoOpt = algos.get(algoid)
    algoOpt map { algo =>
      algoInfos.get(algo.infoid) map { algoInfo =>
        Logger.info("Delete model data for algo ID "+algoid)
        algoInfo.enginetype match {
          case "itemrec" => itemRecScores.deleteByAlgoid(algoid)
          case _ => throw new RuntimeException("Try to delete algo of unsupported engine type: " + algoInfo.enginetype)
        }
      } getOrElse { throw new RuntimeException("Try to delete algo of non-existing algotype: " + algo.infoid) }
    } getOrElse { throw new RuntimeException("Try to delete non-existing algo: " + algoid) }
  }


  /**
   * delete DB data under this app
   */
  def deleteApp(appid: Int, keepSettings: Boolean) = {

    val appEngines = engines.getByAppid(appid)

    appEngines foreach { eng =>
      deleteEngine(eng.id, keepSettings)
      if (!keepSettings) {
        Logger.info("Delete engine ID "+eng.id)
        engines.deleteByIdAndAppid(eng.id, appid)
      }
    }

    deleteAppData(appid)
  }

  /**
   * delete DB data under this engine
   */
  def deleteEngine(engineid: Int, keepSettings: Boolean) = {

    val engineAlgos = algos.getByEngineid(engineid)

    engineAlgos foreach { algo =>
      deleteModelData(algo.id)
      if (!keepSettings) {
        Logger.info("Delete algo ID "+algo.id)
        algos.delete(algo.id)
      }
    }

    val engineOfflineEvals = offlineEvals.getByEngineid(engineid)

    engineOfflineEvals foreach { eval =>
      deleteOfflineEval(eval.id, keepSettings)
      if (!keepSettings) {
        Logger.info("Delete offline eval ID "+eval.id)
        offlineEvals.delete(eval.id)
      }
    }

  }


  /**
   * delete DB data under this offline eval
   */
  def deleteOfflineEval(evalid: Int, keepSettings: Boolean) = {

    deleteTrainingSetData(evalid)
    deleteTestSetData(evalid)

    val evalAlgos = algos.getByOfflineEvalid(evalid)

    evalAlgos foreach { algo =>
      deleteModelData(algo.id)
      if (!keepSettings) {
        algos.delete(algo.id)
      }
    }

    if (!keepSettings) {
      val evalMetrics = offlineEvalMetrics.getByEvalid(evalid)

      evalMetrics foreach { metric =>
        offlineEvalMetrics.delete(metric.id)
      }

      offlineEvalResults.deleteByEvalid(evalid)
    }

  }


  def removeAvailableAlgo(app_id: String, engine_id: String, id: String) = withUser { user => implicit request =>

    deleteModelData(id.toInt)
    // send the deleteAlgoDir(app_id, engine_id, id) request to scheduler here
    WS.url(config.settingsSchedulerUrl+"/apps/"+app_id+"/engines/"+engine_id+"/algos/"+id+"/delete").get()
    algos.delete(id.toInt)
    Ok

  }

  def getDeployedAlgo(app_id: String, engine_id: String) = withUser { user => implicit request =>
    /* sample output
    Ok(toJson(Map(
       "updatedTime" -> toJson("12-03-2012 12:32:12"),
       "status" -> toJson("Running"),
       "algolist" -> toJson(Seq(
	      Map(
	        "id" -> "algoid1234",
	        "algoName" -> "algo-test-sim1",
	        "app_id" -> "appid1234",
	        "engine_id" -> "engid33333",
	        "algotype_id" -> "pdio-knnitembased",
	        "algotypeName" -> "kNN Item-Based CF",
	        "status" -> "deployed",
	        "updatedTime" -> "04-23-2012 12:21:23"
	      ),
	      Map(
	        "id" -> "algoid531",
	        "algoName" -> "algo-test-sim-correl=12",
	        "app_id" -> "appid765",
	        "engine_id" -> "engid33333",
	        "algotype_id" -> "pdio-knnitembased",
	        "algotypeName" -> "kNN Item-Based CF",
	        "status" -> "deployed",
	        "updatedTime" -> "04-23-2012 12:21:23"
	      )
       ))
     )))
     */

    // TODO: verifying this user owns this app id and engine id

    // TODO: check engine_id is int

    val deployedAlgos = algos.getDeployedByEngineid(engine_id.toInt)

    if (!deployedAlgos.hasNext) NoContent
    else
      Ok(toJson(Map(
       "updatedTime" -> toJson("12-03-2012 12:32:12"), // TODO: what's this time for?
       "status" -> toJson("Running"),
       "algolist" -> toJson(deployedAlgos.map { algo =>
         Map("id" -> algo.id.toString,
	         "algoName" -> algo.name,
	         "app_id" -> app_id, // // TODO: should algo db store appid and get it from there?
	         "engine_id" -> algo.engineid.toString,
	         "algotype_id" -> algo.infoid,
	         "algotypeName" -> algoInfos.get(algo.infoid).get.name,
	         "status" -> "deployed",
	         "updatedTime" -> timeFormat.print(algo.updatetime.withZone(DateTimeZone.forID("UTC"))))
       }.toSeq)
      )))

  }

  def getSimEvalList(app_id: String, engine_id: String) = withUser { user => implicit request =>
    /* sample output */
    /*
    Ok(toJson(Seq(
      Map(
        "id" -> toJson("simeval_id123"),
        "app_id" -> toJson("appid1234"),
        "engine_id" -> toJson("engid33333"),
        "algolist" -> toJson(Seq(
						      Map(
						        "id" -> "algoid1234",
						        "algoName" -> "algo-test-sim1",
						        "app_id" -> "appid1234",
						        "engine_id" -> "engid33333",
						        "algotype_id" -> "knnitembased",
						        "algotypeName" -> "kNN Item-Based CF",
						        "settingsString" -> "distance=cosine, virtualCount=50, priorCorrelation=0, viewScore=3, viewmoreScore=5, likeScore=3, dislikeScore=2, buyScore=3, override=latest"
						      ),
						      Map(
						        "id" -> "algoid56456",
						        "algoName" -> "algo-test-mf-gamma=0.1,sigma=8 ",
						        "app_id" -> "appid765",
						        "engine_id" -> "engid33333",
						        "algotype_id" -> "pdio-knnitembased",
						        "algotypeName" -> "kNN Item-Based CF",
						        "settingsString" -> "gamma=0.1, sigma=8, viewScore=3, viewmoreScore=5, likeScore=3, dislikeScore=2, buyScore=3, override=latest"
						      )
					       )),
        "status" -> toJson("pending"),
        "startTime" -> toJson("04-23-2012 12:21:23")
      ),
      Map(
        "id" -> toJson("simeval_id321"),
        "app_id" -> toJson("appid765"),
        "engine_id" -> toJson("engid33333"),
        "algolist" -> toJson(Seq(
						      Map(
						        "id" -> "algoid1234",
						        "algoName" -> "algo-test-sim2",
						        "app_id" -> "appid1234",
						        "engine_id" -> "engid33333",
						        "algotype_id" -> "pdio-knnitembased",
						        "algotypeName" -> "kNN Item-Based CF",
						        "settingsString" -> "distance=cosine, virtualCount=50, priorCorrelation=0, viewScore=3, viewmoreScore=5, likeScore=3, dislikeScore=2, buyScore=3, override=latest"
						      ),
						      Map(
						        "id" -> "algoid888",
						        "algoName" -> "second_algo-test-mf-gamma=0.1,sigma=8 ",
						        "app_id" -> "appid765",
						        "engine_id" -> "engid33333",
						        "algotype_id" -> "pdio-knnitembased",
						        "algotypeName" -> "kNN Item-Based CF",
						        "settingsString" -> "gamma=0.1, sigma=8, viewScore=3, viewmoreScore=5, likeScore=3, dislikeScore=2, buyScore=3, override=latest"
						      )
					       )),
        "status" -> toJson("completed"),
        "startTime" -> toJson("04-23-2012 12:21:23"),
        "endTime" -> toJson("04-25-2012 13:21:23")
      )
     )))*/

    // TODO: check if the user owns this engine

    // get offlineeval for this engine
    val engineOfflineEvals = offlineEvals.getByEngineid(engine_id.toInt)

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
                     "algoName" -> algo.name,
                     "app_id" -> app_id,
                     "engine_id" -> algo.engineid.toString,
                     "algotype_id" -> algo.infoid,
                     "algotypeName" -> algoInfo.name,
                     "settingsString" -> Itemrec.Algorithms.displayParams(algoInfo, algo.params)
                     )
              }.toSeq
              )

          val createtime = eval.createtime map (x => timeFormat.print(x.withZone(DateTimeZone.forID("UTC")))) getOrElse ("-")
          val starttime = eval.starttime map (x => timeFormat.print(x.withZone(DateTimeZone.forID("UTC")))) getOrElse ("-")
          val endtime = eval.endtime map (x => timeFormat.print(x.withZone(DateTimeZone.forID("UTC")))) getOrElse ("-")

          Map(
           "id" -> toJson(eval.id),
           "app_id" -> toJson(app_id),
           "engine_id" -> toJson(eval.engineid),
           "algolist" -> algolist,
           "status" -> toJson(status),
           "startTime" -> toJson(createtime), // NOTE: use createtime here for test date
           "endTime" -> toJson(endtime)
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

  def createSimEval(app_id: String, engine_id: String) = withUser { user => implicit request =>
    /* request payload
     * {"app_id":"1","engine_id":"17","algo[0]":"12","algo[1]":"13","metrics[0]":"map_k","metricsSettings[0]":"5","metrics[1]":"map_k","metricsSettings[1]":"10"}
     */
    val simEvalForm = Form(tuple(
      "app_id" -> number,
      "engine_id" -> number,
      "algo" -> list(number), // algo id
      "metrics" -> (list(text) verifying ("Invalid metrics types.", x => (x.toSet -- supportedMetricTypes).isEmpty)),
      "metricsSettings" -> list(text)
    )) // TODO: verifying this user owns this app_id and engine_id, and the engine_id owns the algo ids

    simEvalForm.bindFromRequest.fold(
      formWithError => {
        //println(formWithError.errors)
        val msg = formWithError.errors(0).message // extract 1st error message only
        BadRequest(toJson(Map("message" -> toJson(msg))))
      },
      formData => {
        val (appId, engineId, algoIds, metricTypes, metricSettings) = formData

        // insert offlineeval record without create time
        val newOfflineEval = OfflineEval(
          id = -1,
          engineid = engineId,
          name = "sim-eval", // TODO: auto generate name now
          trainingsize = 8, // TODO: default now
          testsize = 2, // TODO: default now
          timeorder = false, // TODO: default now
          createtime = None, // NOTE: no createtime yet
          starttime = None,
          endtime = None
        )

        val evalid = offlineEvals.insert(newOfflineEval)

        val optAlgos: List[Option[Algo]] = algoIds map {algoId => algos.get(algoId)}

        if (!optAlgos.contains(None)) {

          // duplicate algo with evalid
          for ( optAlgo <- optAlgos ) {
            val dupAlgo = optAlgo.get.copy(
              id = -1,
              offlineevalid = Option(evalid)
            )
            val dupAlgoId = algos.insert(dupAlgo)

          }

          // create metric record with evalid
          for ((metricType, metricSetting) <- (metricTypes zip metricSettings)) {
            val metricId = offlineEvalMetrics.insert(OfflineEvalMetric(
              id = -1,
              name = metricTypeNames("map_k"),
              metrictype = metricType,
              jarname = "TODO jarname", // TODO
              evalid = evalid,
              params = Map("kParam" -> metricSetting) // TODO: hardcode param index name for now, should depend on metrictype
            ))
          }

          // after all algo and metric info is stored.
          // update offlineeval record with createtime, so scheduler can know it's ready to be picked up
          offlineEvals.update(newOfflineEval.copy(
            id = evalid,
            name = ("sim-eval-" + evalid), // TODO: auto generate name now
            createtime = Option(DateTime.now)
          ))

          WS.url(config.settingsSchedulerUrl+"/users/"+user.id+"/sync").get()

          Ok

        } else {
          // there is error in getting algos, delete the offline eval record inserted.
          offlineEvals.delete(evalid)

          BadRequest(toJson(Map("message" -> toJson("Invalid algo ids."))))
        }

      }
    )
  }

  /**
   * Stop the simulated evaluation job if it's still running/pending
   * Then delete it
   */
  def removeSimEval(app_id: Int, engine_id: Int, id: Int) = withUser { user => implicit request =>

    // TODO: check if user owns this app + enigne + simeval

    // remove algo, remove metric, remove offline eval

    /** Deletion of app data and model data could take a while */
    val timeout = play.api.libs.concurrent.Promise.timeout("Scheduler is unreachable. Giving up.", concurrent.duration.Duration(10, concurrent.duration.MINUTES))

    val offlineEval = offlineEvals.get(id)

    offlineEval map { oe =>
      /** Make sure to unset offline eval's creation time to prevent scheduler from picking up */
      offlineEvals.update(oe.copy(createtime = None))

      /** Stop any possible running jobs */
      val stop = WS.url(s"${config.settingsSchedulerUrl}/apps/${app_id}/engines/${engine_id}/offlineevals/${id}/stop").get()
      /** Clean up intermediate data files */
      val delete = WS.url(s"${config.settingsSchedulerUrl}/apps/${app_id}/engines/${engine_id}/offlineevals/${id}/delete").get()
      /** Synchronize on both scheduler actions */
      val remove = for {
        s <- stop
        d <- delete
      } yield {
        /** Delete settings from database */
        deleteOfflineEval(id, keepSettings=false)
        offlineEvals.delete(id)
      }

      /** Handle any error that might occur within the Future */
      val complete = remove map { r =>
        Ok(obj("message" -> s"Offline evaluation ID $id has been deleted"))
      } recover {
        case e: Exception => InternalServerError(obj("message" -> e.getMessage()))
      }

      /** Detect timeout (10 minutes by default) */
      Async {
        concurrent.Future.firstCompletedOf(Seq(complete, timeout)).map {
          case r: SimpleResult[_] => r
          case t: String => InternalServerError(obj("message" -> t))
        }
      }
    } getOrElse {
      NotFound(obj("message" -> s"Offline evaluation ID $id does not exist"))
    }
  }

  def getSimEvalReport(app_id: String, engine_id: String, id: String) = withUser { user => implicit request =>
    /* sample output */
    /*
    Ok(toJson(
      Map(
        "id" -> toJson("simeval_id123"),
        "app_id" -> toJson("appid1234"),
        "engine_id" -> toJson("engid33333"),
        "algolist" -> toJson(Seq(
						      Map(
						        "id" -> "algoid1234",
						        "algoName" -> "algo-test-sim1",
						        "app_id" -> "appid1234",
						        "engine_id" -> "engid33333",
						        "algotype_id" -> "knnitembased",
						        "algotypeName" -> "kNN Item-Based CF",
						        "settingsString" -> "distance=cosine, virtualCount=50, priorCorrelation=0, viewScore=3, viewmoreScore=5, likeScore=3, dislikeScore=2, buyScore=3, override=latest"
						      ),
						      Map(
						        "id" -> "algoid876",
						        "algoName" -> "algo-test-mf-gamma=0.1,sigma=8",
						        "app_id" -> "appid765",
						        "engine_id" -> "engid33333",
						        "algotype_id" -> "pdio-knnitembased",
						        "algotypeName" -> "kNN Item-Based CF",
						        "settingsString" -> "gamma=0.1, sigma=8, viewScore=3, viewmoreScore=5, likeScore=3, dislikeScore=2, buyScore=3, override=latest"
						      )
					       )),
		"metricslist" -> toJson(Seq(
						      Map(
						        "id" -> "metricid_123",
						        "engine_id" -> "engid33333",
						        "enginetype_id" -> "itemrec",
						        "metricstype_id" -> "map_k",
						        "metricsName" -> "MAP@k",
						        "settingsString" -> "k=5"
						      ),
						      Map(
						        "id" -> "metricid_888",
						        "engine_id" -> "engid33333",
						        "enginetype_id" -> "itemrec",
						        "metricstype_id" -> "map_k",
						        "metricsName" -> "MAP@k",
						        "settingsString" -> "k=10"
						      ),
						      Map(
						        "id" -> "metricid_811",
						        "engine_id" -> "engid33333",
						        "enginetype_id" -> "itemrec",
						        "metricstype_id" -> "map_k",
						        "metricsName" -> "MAP@k",
						        "settingsString" -> "k=20"
						      )
					       )),
		"metricscorelist" -> toJson(Seq(
		    Map("algo_id" -> toJson("algoid1234"), "metrics_id"-> toJson("metricid_123"), "score"-> toJson(0.12341)),
		    Map("algo_id"-> toJson("algoid1234"), "metrics_id"-> toJson("metricid_888"), "score"-> toJson(0.832)),
		    Map("algo_id"-> toJson("algoid1234"), "metrics_id"-> toJson("metricid_811"), "score"-> toJson(0.341)),
		    Map("algo_id"-> toJson("algoid876"), "metrics_id"-> toJson("metricid_123"), "score"-> toJson(0.2341)),
		    Map("algo_id"-> toJson("algoid876"), "metrics_id"-> toJson("metricid_888"), "score"-> toJson(0.9341)),
		    Map("algo_id"-> toJson("algoid876"), "metrics_id"-> toJson("metricid_811"), "score"-> toJson(0.1241))
		 )),
        "status" -> toJson("completed"),
        "startTime" -> toJson("04-23-2012 12:21:23"),
        "endTime" -> toJson("04-25-2012 13:21:23")
      )
    ))*/

    // TODO: check if id is int

    // get offlineeval for this engine
    val optOffineEval: Option[OfflineEval] = offlineEvals.get(id.toInt)

    optOffineEval map { eval =>
      val status = "completed"

      // TODO: add assertion that eval.starttime, eval.endtime can't be None

      val evalAlgos = algos.getByOfflineEvalid(eval.id)

      val algolist =
        if (!evalAlgos.hasNext) // TODO: shouldn't expect this happen
          JsNull
        else {
          toJson(
            evalAlgos.map { algo =>

              val algoInfo = algoInfos.get(algo.infoid).get // TODO: what if couldn't get the algoInfo here?

              Map("id" -> algo.id.toString,
                  "algoName" -> algo.name,
                  "app_id" -> app_id,
                  "engine_id" -> algo.engineid.toString,
                  "algotype_id" -> algo.infoid,
                  "algotypeName" -> algoInfo.name,
                  "settingsString" -> Itemrec.Algorithms.displayParams(algoInfo, algo.params)
                  )
            }.toSeq
          )
        }

      val evalMetrics = offlineEvalMetrics.getByEvalid(eval.id)

      val metricslist =
        if (!evalMetrics.hasNext) // TODO: shouldn't expect this happen
          JsNull
        else {
          toJson(
            evalMetrics.map { metric =>
              Map("id" -> metric.id.toString,
                  "engine_id" -> engine_id,
                  "enginetype_id" -> "itemrec", // TODO: hardcode now, should get it from engine db
                  "metricstype_id" -> metric.metrictype,
                  "metricsName" -> metric.name,
                  "settingsString" -> map_k_displayAllParams(metric.params)
                  )
            }.toSeq
          )
        }

      val evalResults = offlineEvalResults.getByEvalid(eval.id)

      val metricscorelist =
        if (!evalResults.hasNext) // TODO: shouldn't expect this happen
          JsNull
        else {
          toJson(
            evalResults.map { result =>
              Map("algo_id" -> toJson(result.algoid),
                  "metrics_id" -> toJson(result.metricid),
                  "score" -> toJson(result.score.toString)
                  )
            }.toSeq
          )
        }

      val starttime = eval.starttime map (x => timeFormat.print(x.withZone(DateTimeZone.forID("UTC")))) getOrElse ("-")
      val endtime = eval.endtime map (x => timeFormat.print(x.withZone(DateTimeZone.forID("UTC")))) getOrElse ("-")

      Ok(toJson(
        Map(
          "id" -> toJson(eval.id),
           "app_id" -> toJson(app_id),
           "engine_id" -> toJson(eval.engineid),
           "algolist" -> algolist,
           "metricslist" -> metricslist,
           "metricscorelist" -> metricscorelist,
           "status" -> toJson(status),
           "startTime" -> toJson(starttime),
           "endTime" -> toJson(endtime)
           )

      ))

    } getOrElse {
      NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or simeval id."))))
    }

  }
  // Deploy an array of algo -- set availableAlgo's status to deployed? also, undeploy existing, if any
  def algoDeploy(app_id: String, engine_id: String) = withUser { user => implicit request =>
    // REQUIRED Post Param: algo_id_list (array of availableAlgo ids)
    val deployForm = Form(
      "algo_id_list" -> list(number)
    )
    deployForm.bindFromRequest.fold(
      formWithErrors => Ok,
      form => {
        algos.getDeployedByEngineid(engine_id.toInt) foreach { algo =>
          algos.update(algo.copy(deployed = false))
        }
        form foreach { id =>
          algos.get(id) foreach { algo =>
            algos.update(algo.copy(deployed = true))
          }
        }
        WS.url(config.settingsSchedulerUrl+"/users/"+user.id+"/sync").get()
        Ok
      }
    )
  }

  // Undeploy all deployed algo/algo(s) -- set availableAlgo's status back to undeploy?
  def algoUndeploy(app_id: String, engine_id: String) = withUser { user => implicit request =>
    // No extra param required
    algos.getDeployedByEngineid(engine_id.toInt) foreach { algo =>
      algos.update(algo.copy(deployed = false))
    }
    WS.url(config.settingsSchedulerUrl+"/users/"+user.id+"/sync").get()
    Ok
  }

  // Add model training of the currently deployed algo(s) to queue.
  def algoTrainNow(app_id: String, engine_id: String) = withUser { user => implicit request =>
    // No extra param required
    val timeout = play.api.libs.concurrent.Promise.timeout("Scheduler is unreachable. Giving up.", concurrent.duration.Duration(10, concurrent.duration.MINUTES))
    val request = WS.url(s"${config.settingsSchedulerUrl}/apps/${app_id}/engines/${engine_id}/trainoncenow").get() map { r =>
      Ok(obj("message" -> (r.json \ "message").as[String]))
    } recover {
      case e: Exception => InternalServerError(obj("message" -> e.getMessage()))
    }

    /** Detect timeout (10 minutes by default) */
    Async {
      concurrent.Future.firstCompletedOf(Seq(request, timeout)).map {
        case r: SimpleResult[_] => r
        case t: String => InternalServerError(obj("message" -> t))
      }
    }
  }
}

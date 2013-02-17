package controllers

import io.prediction.commons.settings._
import io.prediction.commons.modeldata.{Config => ModelDataConfig}
import io.prediction.commons.modeldata.ItemRecScores
import io.prediction.commons.appdata.{Config => AppDataConfig, TestSetConfig, TrainingSetConfig}
import io.prediction.commons.appdata.{Users, Items, U2IActions}

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.{Constraints}
import play.api.i18n.{Messages, Lang}
import play.api.libs.json.Json._
import play.api.libs.json.{JsNull}
import play.api.libs.ws.WS
import play.api.Play.current

import scala.util.Random

import org.scala_tools.time.Imports._

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
  val users = config.getUsers()
  val apps = config.getApps()
  val engines = config.getEngines()
  val algos = config.getAlgos()
  val offlineEvals = config.getOfflineEvals()
  val offlineEvalMetrics = config.getOfflineEvalMetrics()
  val offlineEvalResults = config.getOfflineEvalResults()

  /** PredictionIO Commons modeldata */
  val modelDataConfig = new ModelDataConfig()
  val itemRecScores = modelDataConfig.getItemRecScores()
  
  /** PredictionIO Commons appdata */
  val appDataConfig = new AppDataConfig()
  val appDataUsers = appDataConfig.getUsers()
  val appDataItems = appDataConfig.getItems()
  val appDataU2IActions = appDataConfig.getU2IActions()
  
  /** PredictionIO Commons training set appdata */
  val trainingSetConfig = new TrainingSetConfig()
  val trainingSetUsers = trainingSetConfig.getUsers()
  val trainingSetItems = trainingSetConfig.getItems()
  val trainingSetU2IActions = trainingSetConfig.getU2IActions()
  
  /** PredictionIO Commons test set appdata */
  val testSetConfig = new TestSetConfig()
  val testSetUsers = testSetConfig.getUsers()
  val testSetItems = testSetConfig.getItems()
  val testSetU2IActions = testSetConfig.getU2IActions()
  
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
    Random.alphanumeric take n mkString
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
      Ok(toJson(userApps map { app =>
        Map("id" -> app.id.toString, "appName" -> app.display)
      } toSeq))
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
      Ok(toJson(Map(
        "id" -> toJson(app.id), // app id
        "updatedTime" -> toJson("Jan 30, 2013 12:30:12"), // TODO
        "nUsers" -> toJson(1234), // TODO
        "nItems" -> toJson(4567), // TODO
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
        }) toSeq)
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
    /*
    val appid = id.toInt
    deleteApp(appid, keepSettings=false)

    // TODO: add code here: send deleteAppDir(appid) request to scheduler
    
    apps.deleteByIdAndUserid(appid, user.id)
    Ok */

    BadRequest(toJson(Map("message" -> toJson("This feature will be available soon."))))
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
    // TODO: add code here: send deleteAppDir(appid) request to scheduler
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
        "algotypelist" -> toJson(Seq(
          Map(
            "id" -> "knnitembased",
            "algotypeName" -> algoTypeNames("knnitembased"), //"Item-based Similarity (kNN) ",
            "description" -> """
						    						This item-based k-NearestNeighbor algorithm predicts user preferences based on previous behaviors of users on similar items.
						    					""",
            "req" -> """ Hadoop """,
            "datareq" -> """ U2I Actions such as Like, Buy and Rate. """)
            ))
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
      Ok(toJson(Map(
        "id" -> eng.id.toString, // engine id
        "enginetype_id" -> eng.enginetype,
        "app_id" -> eng.appid.toString,
        "engineName" -> eng.name)))
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

        Ok(toJson(Map(
          "id" -> engineId.toString, // engine id
          "enginetype_id" -> "itemrec",
          "app_id" -> fappid.toString,
          "engineName" -> enginename)))
      }
    )

  }

  def removeEngine(app_id: String, engine_id: String) = withUser { user => implicit request =>
    // Ok
    BadRequest(toJson(Map("message" -> "This feature will be available soon."))) // TODO
  }
  
  def getAvailableAlgoList(app_id: String, engine_id: String) = withUser { user => implicit request =>
    /* sample output
    Ok(toJson(Seq(
      Map(
        "id" -> "algoid_13213",
        "algoName" -> "algo-test-sim-correl=12",
        "app_id" -> "appid1234",
        "engine_id" -> "engid33333",
        "algotype_id" -> "knnitembased",
        "algotypeName" -> "Item-based Similarity (kNN) ",
        "status" -> "ready",
        "updatedTime" -> "04-23-2012 12:21:33"),

      Map(
      	"id" -> "algoid_13213",
        "algoName" -> "algo-test-mf-gamma=0.1,sigma=8",
        "app_id" -> "appid1234",
        "engine_id" -> "engid33333",
        "algotype_id" -> "knnitembased",
        "algotypeName" -> "Non-negative Matrix Factorization",
        "status" -> "autotuning",
        "updatedTime" -> "04-23-2012 12:21:23"),

      Map(
        "id" -> "algoid_3213",
        "algoName" -> "algo-test-mf-gamma=0.5,sigma=4",
        "app_id" -> "appid765",
        "engine_id" -> "engid33333",
        "algotype_id" -> "knnitembased",
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
               "algotype_id" -> "knnitembased", // TODO
               "algotypeName" -> algoTypeNames("knnitembased"),
               "status" -> "ready", // TODO
               "updatedTime" -> timeFormat.print(algo.updatetime.withZone(DateTimeZone.forID("UTC")))
               )
         }) toSeq
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
        "algotype_id" -> "knnitembased",
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
          "algotype_id" -> "knnitembased", // TODO
          "algotypeName" -> algoTypeNames("knnitembased"),
          "status" -> "ready", // default status
          "createdTime" -> timeFormat.print(algo.createtime.withZone(DateTimeZone.forID("UTC"))),
          "updatedTime" -> timeFormat.print(algo.updatetime.withZone(DateTimeZone.forID("UTC")))
        )))
    } getOrElse {
      NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id."))))
    }
  }

  val supportedAlgoTypes: List[String] = List("knnitembased")
  val algoTypeNames: Map[String, String] = Map("knnitembased" -> "kNN Item-Based CF") // TODO: or "Item-based Similarity (kNN)"?

  def createAvailableAlgo(app_id: String, engine_id: String) = withUser { user => implicit request =>
    // request payload
    //{"algotype_id":"knnitembased","algoName":"test","app_id":"1","engine_id":"12"}

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
      "algotype_id" -> (nonEmptyText verifying("Unsupported algo type.", t => supportedAlgoTypes.contains(t))),
      "algoName" -> nonEmptyText, // TODO: verifying name convention and unique algo name (similar way as engine name)
      "app_id" -> number,
      "engine_id" -> number
    )) // TODO: verifying thie user owns app_id and engine_id

    createAlgoForm.bindFromRequest.fold(
      formWithError => {
        //println(formWithError.errors)
        val msg = formWithError.errors(0).message // extract 1st error message only
        BadRequest(toJson(Map("message" -> toJson(msg))))
      },
      formData => {
        val (algoType, algoName, appId, engineId) = formData

        // TODO: store algotype into algos db?
        val pkgname = "io.prediction.algorithms.scalding.itemrec.knnitembased"
        val newAlgo = Algo(
          id = -1,
          engineid = engineId,
          name = algoName,
          pkgname = pkgname, // TODO, depends on algoType
          deployed = false,
          command = "",
          params = Itemrec.Knnitembased.defaultParams, // TODO, depends on enginetype and algoType
          settings = Itemrec.Knnitembased.defaultSettings, // TODO, depends on enginetype and algoType
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
          "algotype_id" -> algoType, // TODO
          "algotypeName" -> algoTypeNames(algoType),
          "status" -> "ready", // default status
          "createdTime" -> timeFormat.print(newAlgo.createtime.withZone(DateTimeZone.forID("UTC"))),
          "updatedTime" -> timeFormat.print(newAlgo.updatetime.withZone(DateTimeZone.forID("UTC")))
        )))
      }
    )

  }
  
  /**
   * delete appdata DB of this appid
   */
  def deleteAppData(appid: Int) = {
    appDataUsers.deleteByAppid(appid)
    appDataItems.deleteByAppid(appid)
    appDataU2IActions.deleteByAppid(appid)
  }
  
  def deleteTrainingSetData(evalid: Int) = {
    trainingSetUsers.deleteByAppid(evalid)
    trainingSetItems.deleteByAppid(evalid)
    trainingSetU2IActions.deleteByAppid(evalid)
  }
  
  def deleteTestSetData(evalid: Int) = {
    testSetUsers.deleteByAppid(evalid)
    testSetItems.deleteByAppid(evalid)
    testSetU2IActions.deleteByAppid(evalid)
  }
  
  def deleteModelData(algoid: Int) = {
    itemRecScores.deleteByAlgoid(algoid) // TODO: check engine type and delete corresponding modeldata, now hardcode as itemRecScores
  }
  
  
  /** 
   * delete DB data under this app
   */
  def deleteApp(appid: Int, keepSettings: Boolean) = {
    
    val appEngines = engines.getByAppid(appid)
    
    appEngines foreach { eng =>
      deleteEngine(eng.id, keepSettings)
      if (!keepSettings) {
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
        algos.delete(algo.id)
      }
    }
    
    val engineOfflineEvals = offlineEvals.getByEngineid(engineid)
    
    engineOfflineEvals foreach { eval =>
      deleteOfflineEval(eval.id, keepSettings)
      if (!keepSettings) {
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
    /*
    deleteAlgoData(id.toInt)  
    // TODO: add code here: send the deleteAlgoDir(app_id, engine_id, id) request to scheduler here
    algos.delete(id.toInt)
    Ok
    */
    BadRequest(toJson(Map("message" -> "This feature will be available soon."))) // TODO
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
	        "algotype_id" -> "knnitembased",
	        "algotypeName" -> "kNN Item-Based CF",
	        "status" -> "deployed",
	        "updatedTime" -> "04-23-2012 12:21:23"
	      ),
	      Map(
	        "id" -> "algoid531",
	        "algoName" -> "algo-test-sim-correl=12",
	        "app_id" -> "appid765",
	        "engine_id" -> "engid33333",
	        "algotype_id" -> "knnitembased",
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
       "algolist" -> toJson(deployedAlgos map { algo =>
         Map("id" -> algo.id.toString,
	         "algoName" -> algo.name,
	         "app_id" -> app_id, // // TODO: should algo db store appid and get it from there?
	         "engine_id" -> algo.engineid.toString,
	         "algotype_id" -> "knnitembased", // TODO: get it from algo db
	         "algotypeName" -> algoTypeNames("knnitembased"),
	         "status" -> "deployed",
	         "updatedTime" -> timeFormat.print(algo.updatetime.withZone(DateTimeZone.forID("UTC"))))
       } toSeq)
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
						        "algotype_id" -> "knnitembased",
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
						        "algotype_id" -> "knnitembased",
						        "algotypeName" -> "kNN Item-Based CF",
						        "settingsString" -> "distance=cosine, virtualCount=50, priorCorrelation=0, viewScore=3, viewmoreScore=5, likeScore=3, dislikeScore=2, buyScore=3, override=latest"
						      ),
						      Map(
						        "id" -> "algoid888",
						        "algoName" -> "second_algo-test-mf-gamma=0.1,sigma=8 ",
						        "app_id" -> "appid765",
						        "engine_id" -> "engid33333",
						        "algotype_id" -> "knnitembased",
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
      Ok(toJson(

        engineOfflineEvals map { eval =>

          val status = (eval.starttime, eval.endtime) match {
            case (Some(x), Some(y)) => "completed"
            case (_, _) => "pending"
          }

          val evalAlgos = algos.getByOfflineEvalid(eval.id)

          val algolist = if (!evalAlgos.hasNext) JsNull
          else
            toJson(
               evalAlgos map { algo =>
                 Map("id" -> algo.id.toString,
                     "algoName" -> algo.name,
                     "app_id" -> app_id,
                     "engine_id" -> algo.engineid.toString,
                     "algotype_id" -> "knnitembased", // TODO: get it from algo db
                     "algotypeName" -> algoTypeNames("knnitembased"),
                     "settingsString" -> Itemrec.Knnitembased.displayAllParams(algo.params)
                     )
               } toSeq
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
        } toSeq

      ))

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
  def removeSimEval(app_id: String, engine_id: String, id: String) = withUser { user => implicit request =>

    // TODO: check if user owns this app + enigne + simeval

    // remove algo, remove metric, remove offline eval
    // TODO: check id is Int

    // TODO: need to stop running job in scheduler side first!!
    // TODO: any race condition of this eval job is in the processig of being deleted and get picked up by scheduler again?

    /*
    deleteOfflineEval(id.toInt, keepSettings=false)
    // TODO: add code here: send deleteOfflineEvalDir(app_id.toInt, engine_id.toInt, id.toInt) request to scheduler
    offlineEvals.delete(id.toInt)
    Ok
    */ 
    BadRequest(toJson(Map("message" -> "This feature will be available soon."))) // TODO
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
						        "algotype_id" -> "knnitembased",
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
            evalAlgos map { algo =>
              Map("id" -> algo.id.toString,
                  "algoName" -> algo.name,
                  "app_id" -> app_id,
                  "engine_id" -> algo.engineid.toString,
                  "algotype_id" -> "knnitembased", // TODO: get it from algo db
                  "algotypeName" -> algoTypeNames("knnitembased"),
                  "settingsString" -> Itemrec.Knnitembased.displayAllParams(algo.params)
                  )
            } toSeq
          )
        }

      val evalMetrics = offlineEvalMetrics.getByEvalid(eval.id)

      val metricslist =
        if (!evalMetrics.hasNext) // TODO: shouldn't expect this happen
          JsNull
        else {
          toJson(
            evalMetrics map { metric =>
              Map("id" -> metric.id.toString,
                  "engine_id" -> engine_id,
                  "enginetype_id" -> "itemrec", // TODO: hardcode now, should get it from engine db
                  "metricstype_id" -> metric.metrictype,
                  "metricsName" -> metric.name,
                  "settingsString" -> map_k_displayAllParams(metric.params)
                  )
            } toSeq
          )
        }

      val evalResults = offlineEvalResults.getByEvalid(eval.id)

      val metricscorelist =
        if (!evalResults.hasNext) // TODO: shouldn't expect this happen
          JsNull
        else {
          toJson(
            evalResults map { result =>
              Map("algo_id" -> toJson(result.algoid),
                  "metrics_id" -> toJson(result.metricid),
                  "score" -> toJson(result.score.toString)
                  )
            } toSeq
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
}

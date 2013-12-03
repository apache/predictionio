package controllers

import org.specs2.mutable.Specification
import org.specs2.matcher.JsonMatchers
import org.specs2.execute.Pending
import play.api.test.{ WithServer, Port }
import play.api.test.Helpers.{ OK, FORBIDDEN, BAD_REQUEST, NOT_FOUND, NO_CONTENT }
import play.api.test.Helpers.{ await => HelperAwait, wsUrl, defaultAwaitTimeout }
import play.api.libs.json.{ JsNull, JsArray, Json, JsValue }
import play.api.libs.ws.WS.{ WSRequestHolder }
import org.apache.commons.codec.digest.DigestUtils

import com.mongodb.casbah.Imports._

import java.net.URLEncoder

import io.prediction.commons.Config
import io.prediction.commons.settings.{ App, EngineInfo, AlgoInfo, OfflineEvalMetricInfo, Param, ParamDoubleConstraint, ParamUI }
import io.prediction.commons.settings.{ Engine }

class AdminServerSpec extends Specification with JsonMatchers {
  private def md5password(password: String) = DigestUtils.md5Hex(password)

  val config = new Config
  val users = config.getSettingsUsers()
  val apps = config.getSettingsApps()
  val engineInfos = config.getSettingsEngineInfos()
  val algoInfos = config.getSettingsAlgoInfos()
  val offlineEvalMetricInfos = config.getSettingsOfflineEvalMetricInfos()
  val engines = config.getSettingsEngines()

  /* create test user account */
  def createTestUser(firstname: String, lastname: String, email: String, password: String): (Int, JsValue) = {
    val testUserid = users.insert(
      email = email,
      password = md5password(password),
      firstname = firstname,
      lastname = Some(lastname),
      confirm = email
    )
    users.confirm(email)

    val testUser = Json.obj("id" -> testUserid, "username" -> s"${firstname} ${lastname}", "email" -> email)

    (testUserid, testUser)
  }

  /* signin first, and then add cookie to header of the request */
  def signedinRequest(req: WSRequestHolder, email: String, password: String)(implicit port: Port): WSRequestHolder = {
    val response = HelperAwait(wsUrl("/signin").post(Json.obj("email" -> email, "password" -> password)))
    val signinCookie = response.header("Set-Cookie").get

    req.withHeaders("Cookie" -> signinCookie)
  }

  def setupInfo() = {

    val itemrecEngine = EngineInfo(
      id = "itemrec",
      name = "Item Recommendation Engine",
      description = Some("Description 1"),
      defaultsettings = Map[String, Param]("abc" -> Param(id = "abc", name = "", description = None, defaultvalue = 123.4, constraint = ParamDoubleConstraint(), ui = ParamUI())),
      defaultalgoinfoid = "knn"
    )

    engineInfos.insert(itemrecEngine)

    val knnAlgo = AlgoInfo(
      id = "knn",
      name = "kNN Item Based Collaborative Filtering",
      description = Some("This item-based k-NearestNeighbor algorithm predicts user preferences based on previous behaviors of users on similar items."),
      batchcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ --numRecommendations $numRecommendations$ --unseenOnly $unseenOnly$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$")),
      offlineevalcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ --numRecommendations $numRecommendations$ --unseenOnly $unseenOnly$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet false")),
      params = Map(),
      paramorder = Seq(
        "measureParam",
        "priorCountParam",
        "priorCorrelParam",
        "minNumRatersParam",
        "maxNumRatersParam",
        "minIntersectionParam",
        "minNumRatedSimParam",
        "viewParam",
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      paramsections = Seq(),
      engineinfoid = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate."))

    algoInfos.insert(knnAlgo)

    val mapMetric = OfflineEvalMetricInfo(
      id = "map",
      name = "Mean Average Precision A",
      description = Some("metric description"),
      engineinfoids = Seq("itemrec"),
      commands = Some(Seq(
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataTrainingDbType$ --modeldata_dbName $modeldataTrainingDbName$ --modeldata_dbHost $modeldataTrainingDbHost$ --modeldata_dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -Devalid=$evalid$ -Dalgoid=$algoid$ -Dk=$kParam$ -Dmetricid=$metricid$ -Dhdfsroot=$hdfsRoot$ -jar $topkJar$",
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$")),
      paramdefaults = Map("k" -> 20),
      paramnames = Map("k" -> "k"),
      paramdescription = Map("k" -> "Averaging window size"),
      paramorder = Seq("k"))

    offlineEvalMetricInfos.insert(mapMetric)

  }

  setupInfo()

  /* tests */
  "signin" should {

    val (testUserid, testUser) = createTestUser("Test", "Account", "abc@test.com", "testpassword")

    "accept correct password and return user data" in new WithServer {
      val response = HelperAwait(wsUrl("/signin").post(Json.obj("email" -> "abc@test.com", "password" -> "testpassword")))

      response.status must equalTo(OK) and
        (response.json must equalTo(testUser))
    }

    "reject incorrect email or password" in new WithServer {
      val response1 = HelperAwait(wsUrl("/signin").post(Json.obj("email" -> "abc@test.com", "password" -> "incorrect password")))
      val response2 = HelperAwait(wsUrl("/signin").post(Json.obj("email" -> "other@test.com", "password" -> "testpassword")))

      response1.status must equalTo(FORBIDDEN) and
        (response2.status must equalTo(FORBIDDEN))
    }

  }

  "signout" should {
    "return OK" in new WithServer {
      val response = HelperAwait(wsUrl("/signout").post(Json.obj()))
      // TODO: check session is cleared
      response.status must equalTo(OK)
    }
  }

  "auth" should {

    val email = "auth@test.com"
    val password = "authtestpassword"
    val (testUserid, testUser) = createTestUser("Test", "Account", email, password)

    "return OK and user data if the user has signed in" in new WithServer {
      //val response = HelperAwait(wsUrl("/auth").withHeaders("Cookie" -> signinCookie).get)
      val response = HelperAwait(signedinRequest(wsUrl("/auth"), email, password).get)

      response.status must equalTo(OK) and
        (response.json must equalTo(testUser))
    }

    "return FORBIDDEN if user has not signed in" in new WithServer {
      val response = HelperAwait(wsUrl("/auth").get)

      response.status must equalTo(FORBIDDEN)
    }
  }

  "POST /apps" should {

    val email = "postapps@test.com"
    val password = "postappspassword"
    val (testUserid, testUser) = createTestUser("Test", "Account", email, password)

    "reject empty appname" in new WithServer {
      val r = HelperAwait(signedinRequest(wsUrl("/apps"), email, password).
        post(Json.obj("appname" -> "")))

      r.status must equalTo(BAD_REQUEST)
    }

    "create an app and write to database" in new WithServer {
      val appname = "My Test App"
      val r = HelperAwait(signedinRequest(wsUrl("/apps"), email, password).
        post(Json.obj("appname" -> appname)))

      val appid = (r.json \ "id").asOpt[Int].getOrElse(0)

      val validAppid = (appid != 0)
      val dbWritten = apps.get(appid).map { app =>
        (appname == app.display) && (appid == app.id)
      }.getOrElse(false)

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.obj("id" -> appid, "appname" -> appname))) and
        (dbWritten must beTrue) and
        (validAppid must beTrue)

    }
  }

  "GET /apps" should {

    val email = "getapps@test.com"
    val password = "getappspassword"
    val (testUserid, testUser) = createTestUser("Test", "Account", email, password)

    val email2 = "getapps2@test.com"
    val password2 = "getapps2password"
    val (testUserid2, testUser2) = createTestUser("Test", "Account", email2, password2)

    val email3 = "getapps3@test.com"
    val password3 = "getapps3password"
    val (testUserid3, testUser3) = createTestUser("Test", "Account", email3, password3)

    val testApp = App(
      id = 0,
      userid = testUserid,
      appkey = "appkeystring",
      display = "Get App Name",
      url = None,
      cat = None,
      desc = None,
      timezone = "UTC"
    )

    val testApp2 = testApp.copy(
      userid = testUserid2, // other userid
      display = "Get App Name 2"
    )

    val testApp3 = testApp.copy(display = "Get App Name 3")
    val testApp4 = testApp.copy(display = "Get App Name 4")

    val appid = apps.insert(testApp)
    val appid2 = apps.insert(testApp2)
    val appid3 = apps.insert(testApp3)
    val appid4 = apps.insert(testApp4)

    "return app with this id" in new WithServer {
      val r = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid}"), email, password).get())

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.obj("id" -> appid, "appname" -> "Get App Name")))
    }

    "reject if id not found for this user" in new WithServer {
      val r = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid2}"), email, password).get())

      r.status must equalTo(NOT_FOUND)
    }

    "return NO_CONTENT if 0 app" in new WithServer {
      val r = HelperAwait(signedinRequest(wsUrl(s"/apps"), email3, password3).get())

      r.status must equalTo(NO_CONTENT)
    }

    "return list of 1 app" in new WithServer {
      val r = HelperAwait(signedinRequest(wsUrl(s"/apps"), email2, password2).get())

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.arr(Json.obj("id" -> appid2, "appname" -> "Get App Name 2"))))
    }

    "return list of more than 1 app" in new WithServer {
      val r = HelperAwait(signedinRequest(wsUrl("/apps"), email, password).get())

      val appJson1 = Json.obj("id" -> appid, "appname" -> "Get App Name")
      val appJson3 = Json.obj("id" -> appid3, "appname" -> "Get App Name 3")
      val appJson4 = Json.obj("id" -> appid4, "appname" -> "Get App Name 4")

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.arr(appJson1, appJson3, appJson4)))
    }

    "return app details" in new WithServer {
      val r = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid}/details"), email, password).get())

      r.status must equalTo(OK) and
        (r.body must /("id" -> appid)) and
        (r.body must /("updatedtime" -> """.*""".r)) and
        (r.body must /("userscount" -> 0)) and
        (r.body must /("itemscount" -> 0)) and
        (r.body must /("u2icount" -> 0)) and
        (r.body must /("apiurl" -> """.*""".r)) and
        (r.body must /("appkey" -> "appkeystring"))
    }

  }

  "DELETE /apps" should {
    "delete an app" in new WithServer {
      new Pending("TODO")
    }
  }

  "POST /apps/id/erase_data" should {
    "erase all app data" in new WithServer {
      new Pending("TODO")
    }
  }

  "GET /engineinfos" should {

    "return all engine infos" in new WithServer {
      val r = HelperAwait(wsUrl(s"/engineinfos").get())

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.arr(Json.obj("id" -> "itemrec", "engineinfoname" -> "Item Recommendation Engine", "description" -> "Description 1"))))
    }

    "return all algo infos of a engineinfoid" in new WithServer {
      val r = HelperAwait(wsUrl(s"/engineinfos/itemrec/algoinfos").get())

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.obj(
          "engineinfoname" -> "Item Recommendation Engine",
          "algotypelist" -> Json.arr(Json.obj(
            "id" -> "knn",
            "algoinfoname" -> "kNN Item Based Collaborative Filtering",
            "description" -> "This item-based k-NearestNeighbor algorithm predicts user preferences based on previous behaviors of users on similar items.",
            "req" -> Json.toJson(Seq("Hadoop")),
            "datareq" -> Json.toJson(Seq("Users, Items, and U2I Actions such as Like, Buy and Rate."))
          ))
        )))
    }

    "return all metric infos of a engineinfoid" in new WithServer {
      val r = HelperAwait(wsUrl(s"/engineinfos/itemrec/metricinfos").get())

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.obj(
          "engineinfoname" -> "Item Recommendation Engine",
          "metricslist" -> Json.arr(Json.obj(
            "id" -> "map",
            "metricsname" -> "Mean Average Precision A",
            "metricslongname" -> "metric description",
            "settingfields" -> Json.obj("k" -> "int")
          ))
        )))
    }
  }

  "POST /apps/:appid/engines" should {

    val email = "postengines@test.com"
    val password = "postenginespassword"
    val (testUserid, testUser) = createTestUser("Test", "Account", email, password)

    val testApp = App(
      id = 0,
      userid = testUserid,
      appkey = "appkeystring",
      display = "POST Engines App Name",
      url = None,
      cat = None,
      desc = None,
      timezone = "UTC"
    )
    val appid = apps.insert(testApp)

    "create an engine and write to database" in new WithServer {
      val engineinfoid = "itemrec"
      val enginename = "My-Engine-A"

      val r = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid}/engines"), email, password).
        post(Json.obj("engineinfoid" -> engineinfoid, "enginename" -> enginename)))

      val engineid = (r.json \ "id").asOpt[Int].getOrElse(0)

      val validEngineid = (engineid != 0)
      // check database
      val dbWritten = engines.get(engineid).map { eng =>
        (eng.name == enginename) &&
          (eng.infoid == engineinfoid) &&
          (eng.appid == appid)
      }.getOrElse(false)

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.obj(
          "id" -> engineid,
          "engineinfoid" -> engineinfoid,
          "appid" -> appid,
          "enginename" -> enginename))) and
        (dbWritten must beTrue) and
        (validEngineid must beTrue)
    }

    "reject invalid engine name" in new WithServer {
      val engineinfoid = "itemrec"
      val enginename = "Space is not allowed"

      val r = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid}/engines"), email, password).
        post(Json.obj("engineinfoid" -> engineinfoid, "enginename" -> enginename)))

      r.status must equalTo(BAD_REQUEST)
    }

    "reject duplicated engine name" in new WithServer {
      val engineinfoid = "itemrec"
      val enginename = "myengine"
      val enginename2 = "myengine2"

      val r1 = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid}/engines"), email, password).
        post(Json.obj("engineinfoid" -> engineinfoid, "enginename" -> enginename)))

      val r1dup = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid}/engines"), email, password).
        post(Json.obj("engineinfoid" -> engineinfoid, "enginename" -> enginename)))

      val r2 = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid}/engines"), email, password).
        post(Json.obj("engineinfoid" -> engineinfoid, "enginename" -> enginename2)))

      r1.status must equalTo(OK) and
        (r1dup.status must equalTo(BAD_REQUEST)) and
        (r2.status must equalTo(OK))
    }

    "reject invalid engineinfoid" in new WithServer {
      val engineinfoid = "unknown"
      val enginename = "unknown-engine"

      val r = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid}/engines"), email, password).
        post(Json.obj("engineinfoid" -> engineinfoid, "enginename" -> enginename)))

      r.status must equalTo(BAD_REQUEST)
    }

  }

  "GET /apps/:appid/engines" should {

    val email = "getengines@test.com"
    val password = "getenginespassword"
    val (testUserid, testUser) = createTestUser("Test", "Account", email, password)

    val testApp = App(
      id = 0,
      userid = testUserid,
      appkey = "appkeystring",
      display = "Get Engines App Name",
      url = None,
      cat = None,
      desc = None,
      timezone = "UTC"
    )
    val testApp2 = testApp.copy(
      appkey = "appkeystring2",
      display = "Get Engines App Name2"
    )
    val testApp3 = testApp.copy(
      appkey = "appkeystring3",
      display = "Get Engines App Name3"
    )

    val appid = apps.insert(testApp)
    val appid2 = apps.insert(testApp2)
    val appid3 = apps.insert(testApp3)

    val testEngine = Engine(
      id = 0,
      appid = appid,
      name = "get-engine",
      infoid = "itemrec",
      itypes = None, // NOTE: default None (means all itypes)
      settings = Map("a" -> "b")
    )
    val testEngine2 = testEngine.copy(appid = appid2, name = "get-engine2") // diff app
    val testEngine3 = testEngine.copy(name = "get-engine3") // diff name
    val testEngine4 = testEngine.copy(name = "get-engine4") // diff name

    val engineid = engines.insert(testEngine)
    val engineid2 = engines.insert(testEngine2)
    val engineid3 = engines.insert(testEngine3)
    val engineid4 = engines.insert(testEngine4)

    "return the engine of the specificied /:engineid" in new WithServer {
      val r = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid}/engines/${engineid}"), email, password).get)

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.obj(
          "id" -> engineid,
          "engineinfoid" -> "itemrec",
          "appid" -> appid,
          "enginename" -> "get-engine",
          "enginestatus" -> "noappdata"
        )))
    }

    "return error if invalid engineid" in new WithServer {
      // get engine of diff app
      val r = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid}/engines/${engineid2}"), email, password).get)

      r.status must equalTo(NOT_FOUND)
    }

    "return error if invalid appid" in new WithServer {
      // appid not belong to this user
      val r = HelperAwait(signedinRequest(wsUrl(s"/apps/99999/engines"), email, password).get)

      r.status must equalTo(NOT_FOUND)
    }

    "return NO_CONTENT if 0 engine" in new WithServer {
      val r = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid3}/engines"), email, password).get)

      r.status must equalTo(NO_CONTENT)
    }

    "return list of 1 engine" in new WithServer {
      val r = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid2}/engines"), email, password).get)

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.obj(
          "id" -> appid2,
          "enginelist" -> Json.arr(
            Json.obj(
              "id" -> engineid2,
              "enginename" -> "get-engine2",
              "engineinfoid" -> "itemrec"
            )
          )
        )))
    }

    "retutn list of engines" in new WithServer {
      val r = HelperAwait(signedinRequest(wsUrl(s"/apps/${appid}/engines"), email, password).get)

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.obj(
          "id" -> appid,
          "enginelist" -> Json.arr(
            Json.obj(
              "id" -> engineid,
              "enginename" -> "get-engine",
              "engineinfoid" -> "itemrec"
            ),
            Json.obj(
              "id" -> engineid3,
              "enginename" -> "get-engine3",
              "engineinfoid" -> "itemrec"
            ),
            Json.obj(
              "id" -> engineid4,
              "enginename" -> "get-engine4",
              "engineinfoid" -> "itemrec"
            )
          )
        )))
    }
  }

  "DELETE /apps/:appid/engines/:id" should {
    "delete the engine" in new WithServer {
      new Pending("TODO")
    }

    "return error if invalid appid" in new WithServer {
      new Pending("TODO")
    }

    "return error if invalid engineid" in new WithServer {
      new Pending("TODO")
    }
  }

  step {
    MongoConnection()(config.settingsDbName).dropDatabase()
  }
}
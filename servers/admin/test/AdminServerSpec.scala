package controllers

import org.specs2.mutable.Specification
import org.specs2.matcher.JsonMatchers
import org.specs2.execute.Pending
import play.api.test.{ WithServer, Port }
import play.api.test.Helpers.{ OK, FORBIDDEN, BAD_REQUEST, NOT_FOUND }
import play.api.test.Helpers.{ await => HelperAwait, wsUrl, defaultAwaitTimeout }
import play.api.libs.json.{ JsNull, JsArray, Json }
import org.apache.commons.codec.digest.DigestUtils

import com.mongodb.casbah.Imports._

import io.prediction.commons.Config
import io.prediction.commons.settings.{ App, EngineInfo, AlgoInfo, OfflineEvalMetricInfo, Param, ParamDoubleConstraint, ParamUI }

class AdminServerSpec extends Specification with JsonMatchers {
  private def md5password(password: String) = DigestUtils.md5Hex(password)

  val config = new Config
  val users = config.getSettingsUsers()
  val apps = config.getSettingsApps()
  val engineInfos = config.getSettingsEngineInfos()
  val algoInfos = config.getSettingsAlgoInfos()
  val offlineEvalMetricInfos = config.getSettingsOfflineEvalMetricInfos()

  /* create test user account */
  val testUserid = users.insert(
    email = "abc@test.com",
    password = md5password("testpassword"),
    firstname = "Test",
    lastname = Some("Account"),
    confirm = "abc@test.com"
  )
  users.confirm("abc@test.com")

  val testUser = Json.obj("id" -> testUserid, "username" -> "Test Account", "email" -> "abc@test.com")

  /* user signin cookie */
  val signinCookie = """PLAY_SESSION="d57cb53f04784c64d1f275bc93012b67e93bb6ec-username=abc%40test.com"; Path=/; HTTPOnly"""

  /* tests */
  "signin" should {
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
    "return OK and user data if the user has signed in" in new WithServer {
      val response = HelperAwait(wsUrl("/auth").withHeaders("Cookie" -> signinCookie).get)

      response.status must equalTo(OK) and
        (response.json must equalTo(testUser))
    }

    "return FORBIDDEN if user has not signed in" in new WithServer {
      val response = HelperAwait(wsUrl("/auth").get)

      response.status must equalTo(FORBIDDEN)
    }
  }

  "POST /apps" should {

    "reject empty appname" in new WithServer {
      val r = HelperAwait(wsUrl("/apps").
        withHeaders("Cookie" -> signinCookie).
        post(Json.obj("appname" -> "")))

      r.status must equalTo(BAD_REQUEST)
    }

    "create an app and write to database" in new WithServer {
      val appname = "My Test App"
      val r = HelperAwait(wsUrl("/apps").
        withHeaders("Cookie" -> signinCookie).
        post(Json.obj("appname" -> appname)))

      val appid = (r.json \ "id").as[Int]

      val dbWritten = apps.get(appid).map { app =>
        (appname == app.display) && (appid == app.id)
      }.getOrElse(
        false
      )

      apps.deleteByIdAndUserid(appid, testUserid)

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.obj("id" -> appid, "appname" -> appname))) and
        (dbWritten must beTrue)
    }
  }

  "GET /apps" should {

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
      userid = 12345 // other userid
    )

    val testApp3 = testApp.copy(display = "Get App Name 3")
    val testApp4 = testApp.copy(display = "Get App Name 4")

    val appid = apps.insert(testApp)
    val appid2 = apps.insert(testApp2)
    val appid3 = apps.insert(testApp3)
    val appid4 = apps.insert(testApp4)

    "return app with this id" in new WithServer {
      val r = HelperAwait(wsUrl(s"/apps/${appid}").
        withHeaders("Cookie" -> signinCookie).
        get())

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.obj("id" -> appid, "appname" -> "Get App Name")))
    }

    "reject if id not found for this user" in new WithServer {
      val r = HelperAwait(wsUrl(s"/apps/${appid2}").
        withHeaders("Cookie" -> signinCookie).
        get())

      r.status must equalTo(NOT_FOUND)
    }

    "return list of 0 app" in new WithServer {
      new Pending("TODO")
    }

    "return list of 1 app" in new WithServer {
      new Pending("TODO")
    }

    "return list of more than 1 app" in new WithServer {
      val r = HelperAwait(wsUrl("/apps").
        withHeaders("Cookie" -> signinCookie).
        get())

      val appJson1 = Json.obj("id" -> appid, "appname" -> "Get App Name")
      val appJson3 = Json.obj("id" -> appid3, "appname" -> "Get App Name 3")
      val appJson4 = Json.obj("id" -> appid4, "appname" -> "Get App Name 4")

      r.status must equalTo(OK) and
        (r.json must equalTo(JsArray(Seq(appJson1, appJson3, appJson4))))
    }

    "return app details" in new WithServer {
      val r = HelperAwait(wsUrl(s"/apps/${appid}/details").
        withHeaders("Cookie" -> signinCookie).
        get())

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

    val itemrecEngine = EngineInfo(
      id = "itemrec",
      name = "Item Recommendation Engine",
      description = Some("Description 1"),
      defaultsettings = Map[String, Param]("abc" -> Param(id = "abc", name = "", description = None, defaultvalue = 123.4, constraint = ParamDoubleConstraint(), ui = ParamUI())),
      defaultalgoinfoid = "cf-algo"
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

    "return all engine infos" in new WithServer {
      val r = HelperAwait(wsUrl(s"/engineinfos").get())

      r.status must equalTo(OK) and
        (r.json must equalTo(JsArray(Seq(Json.obj("id" -> "itemrec", "engineinfoname" -> "Item Recommendation Engine", "description" -> "Description 1")))))
    }

    "return all algo infos of a engineinfoid" in new WithServer {
      val r = HelperAwait(wsUrl(s"/engineinfos/itemrec/algoinfos").get())

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.obj(
          "engineinfoname" -> "Item Recommendation Engine",
          "algotypelist" -> JsArray(Seq(Json.obj(
            "id" -> "knn",
            "algoinfoname" -> "kNN Item Based Collaborative Filtering",
            "description" -> "This item-based k-NearestNeighbor algorithm predicts user preferences based on previous behaviors of users on similar items.",
            "req" -> Json.toJson(Seq("Hadoop")),
            "datareq" -> Json.toJson(Seq("Users, Items, and U2I Actions such as Like, Buy and Rate."))
          )))
        )))
    }

    "return all metric infos of a engineinfoid" in new WithServer {
      val r = HelperAwait(wsUrl(s"/engineinfos/itemrec/metricinfos").get())

      r.status must equalTo(OK) and
        (r.json must equalTo(Json.obj(
          "engineinfoname" -> "Item Recommendation Engine",
          "metricslist" -> JsArray(Seq(Json.obj(
            "id" -> "map",
            "metricsname" -> "Mean Average Precision A",
            "metricslongname" -> "metric description",
            "settingfields" -> Json.obj("k" -> "int")
          )))
        )))
    }
  }

  step {
    MongoConnection()(config.settingsDbName).dropDatabase()
  }
}
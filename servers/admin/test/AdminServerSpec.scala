package controllers

import org.specs2.mutable.Specification
import org.specs2.matcher.JsonMatchers
import org.specs2.execute.Pending
import play.api.test.{WithServer, Port}
import play.api.test.Helpers.{OK, FORBIDDEN, BAD_REQUEST, NOT_FOUND}
import play.api.test.Helpers.{await => HelperAwait, wsUrl, defaultAwaitTimeout}
import play.api.libs.json.{JsNull, JsArray, Json}
import org.apache.commons.codec.digest.DigestUtils

import com.mongodb.casbah.Imports._

import io.prediction.commons.Config
import io.prediction.commons.settings.{Users, Apps}
import io.prediction.commons.settings.{App}

class AdminServerSpec extends Specification with JsonMatchers {
  private def md5password(password: String) = DigestUtils.md5Hex(password)

  val config = new Config
  val users: Users = config.getSettingsUsers()
  val apps: Apps = config.getSettingsApps()

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

    "create an app and write to database" in new WithServer  {
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
  

  step {
    MongoConnection()(config.settingsDbName).dropDatabase()
  }
}
package controllers

import org.specs2.mutable.Specification
import org.specs2.execute.Pending
import play.api.test.{WithServer, Port}
import play.api.test.Helpers.{OK, FORBIDDEN}
import play.api.test.Helpers.{await => HelperAwait, wsUrl, defaultAwaitTimeout}
import play.api.libs.json.{JsNull, JsArray, Json}
import org.apache.commons.codec.digest.DigestUtils

import com.mongodb.casbah.Imports._

import io.prediction.commons.Config
import io.prediction.commons.settings.{Users}

class AdminServerSpec extends Specification {
  private def md5password(password: String) = DigestUtils.md5Hex(password)

  val config = new Config
  val users: Users = config.getSettingsUsers()
  
  /* create test user account */
  val userid = users.insert(
    email = "abc@test.com",
    password = md5password("testpassword"),
    firstname = "Test",
    lastname = Some("Account"),
    confirm = "abc@test.com"
  )
  users.confirm("abc@test.com")

  val testUser = Json.obj("id" -> userid, "username" -> "Test Account", "email" -> "abc@test.com")

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

  "apps" should {
    "be created and read correctly" in new WithServer  {
      new Pending("TODO")
    }

    "be deleted" in new WithServer {
      new Pending("TODO")
    }
  }

  step {
    MongoConnection()(config.settingsDbName).dropDatabase()
  }
}
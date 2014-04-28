package io.prediction.scheduler

import io.prediction.commons.Config
import io.prediction.commons.settings._
import io.prediction.commons.appdata._
import io.prediction.commons.modeldata._

import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._

import org.specs2.mutable._

import com.mongodb.casbah.Imports._
import com.github.nscala_time.time.Imports._

class APISpec extends Specification {
  "PredictionIO API Specification".txt

  /** Setup test data. */
  val config = new Config
  val apps = config.getSettingsApps()
  val engines = config.getSettingsEngines()
  val algos = config.getSettingsAlgos()
  val items = config.getAppdataItems()
  val itemRecScores = config.getModeldataItemRecScores()
  val itemSimScores = config.getModeldataItemSimScores()

  val userid = 1

  val appid = apps.insert(App(
    id = 0,
    userid = userid,
    appkey = "appkey",
    display = "",
    url = None,
    cat = None,
    desc = None,
    timezone = "UTC"))

  val dac = Item(
    id = "dac",
    appid = appid,
    ct = DateTime.now,
    itypes = List("fresh", "meat"),
    starttime = Some(DateTime.now),
    endtime = None,
    price = Some(49.394),
    profit = None,
    latlng = Some((37.3197611, -122.0466141)),
    inactive = None,
    attributes = Some(Map("foo" -> "bar", "foo2" -> "bar2")))
  val hsh = Item(
    id = "hsh",
    appid = appid,
    ct = DateTime.now,
    itypes = List("fresh", "meat"),
    starttime = Some(DateTime.now),
    endtime = None,
    price = Some(49.394),
    profit = None,
    latlng = Some((37.3370801, -122.0493201)),
    inactive = None,
    attributes = None)
  val mvh = Item(
    id = "mvh",
    appid = appid,
    ct = DateTime.now,
    itypes = List("fresh", "meat"),
    starttime = Some(DateTime.now),
    endtime = None,
    price = Some(49.394),
    profit = None,
    latlng = Some((37.3154153, -122.0566829)),
    inactive = None,
    attributes = Some(Map("foo3" -> "bar3")))
  val lbh = Item(
    id = "lbh",
    appid = appid,
    ct = DateTime.now,
    itypes = List("fresh", "meat"),
    starttime = Some(DateTime.now),
    endtime = None,
    price = Some(49.394),
    profit = None,
    latlng = Some((37.2997029, -122.0034684)),
    inactive = None,
    attributes = Some(Map("foo4" -> "bar4", "foo5" -> "bar5")))
  val allItems = Seq(dac, hsh, lbh, mvh)
  allItems foreach { items.insert(_) }

  "ItemRec" should {
    val enginename = "itemrec"

    val engineid = engines.insert(Engine(
      id = 0,
      appid = appid,
      name = "itemrec",
      infoid = "itemrec",
      itypes = None,
      params = Map()))

    val algoid = algos.insert(Algo(
      id = 0,
      engineid = engineid,
      name = enginename,
      infoid = "pdio-knnitembased",
      command = "itemr",
      params = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None))

    itemRecScores.insert(ItemRecScore(
      uid = "user1",
      iids = Seq("hsh", "mvh", "lbh", "dac"),
      scores = Seq(4, 3, 2, 1),
      itypes = Seq(Seq("fresh", "meat"), Seq("fresh", "meat"), Seq("fresh", "meat"), Seq("fresh", "meat")),
      appid = appid,
      algoid = algoid,
      modelset = true))

    "get top N" in new WithServer {
      val response = Helpers.await(wsUrl(s"/engines/itemrec/${enginename}/topn.json")
        .withQueryString(
          "pio_appkey" -> "appkey",
          "pio_uid" -> "user1",
          "pio_n" -> "10")
        .get())
      response.status must beEqualTo(OK) and
        (response.body must beEqualTo("""{"pio_iids":["hsh","mvh","lbh","dac"]}"""))
    }

    "get top N with geo" in new WithServer {
      val response = Helpers.await(wsUrl(s"/engines/itemrec/${enginename}/topn.json")
        .withQueryString(
          "pio_appkey" -> "appkey",
          "pio_uid" -> "user1",
          "pio_n" -> "10",
          "pio_latlng" -> "37.3229978,-122.0321823",
          "pio_within" -> "2.2")
        .get())
      response.status must beEqualTo(OK) and
        (response.body must beEqualTo("""{"pio_iids":["hsh","dac"]}"""))
    }
  }

  "ItemSim" should {
    val enginename = "itemsim"

    val engineid = engines.insert(Engine(
      id = 0,
      appid = appid,
      name = "itemsim",
      infoid = "itemsim",
      itypes = None,
      params = Map()))

    val algoid = algos.insert(Algo(
      id = 0,
      engineid = engineid,
      name = enginename,
      infoid = "pdio-itembasedcf",
      command = "items",
      params = Map("foo" -> "bar"),
      settings = Map("dead" -> "beef"),
      modelset = true,
      createtime = DateTime.now,
      updatetime = DateTime.now,
      status = "deployed",
      offlineevalid = None))

    itemSimScores.insert(ItemSimScore(
      iid = "user1",
      simiids = Seq("hsh", "mvh", "lbh", "dac"),
      scores = Seq(4, 3, 2, 1),
      itypes = Seq(Seq("fresh", "meat"), Seq("fresh", "meat"), Seq("fresh", "meat"), Seq("fresh", "meat")),
      appid = appid,
      algoid = algoid,
      modelset = true))

    "get top N" in new WithServer {
      val response = Helpers.await(wsUrl(s"/engines/itemsim/${enginename}/topn.json")
        .withQueryString(
          "pio_appkey" -> "appkey",
          "pio_iid" -> "user1",
          "pio_n" -> "10")
        .get())
      response.status must beEqualTo(OK) and
        (response.body must beEqualTo("""{"pio_iids":["hsh","mvh","lbh","dac"]}"""))
    }

    "get top N with geo" in new WithServer {
      val response = Helpers.await(wsUrl(s"/engines/itemsim/${enginename}/topn.json")
        .withQueryString(
          "pio_appkey" -> "appkey",
          "pio_iid" -> "user1",
          "pio_n" -> "10",
          "pio_latlng" -> "37.3229978,-122.0321823",
          "pio_within" -> "2.2")
        .get())
      response.status must beEqualTo(OK) and
        (response.body must beEqualTo("""{"pio_iids":["hsh","dac"]}"""))
    }
  }

  "Items" should {
    "fail creation with tabs in itypes" in new WithServer {
      val response = Helpers.await(wsUrl(s"/items.json").post(Map(
        "pio_appkey" -> Seq("appkey"),
        "pio_iid" -> Seq("fooitem"),
        "pio_itypes" -> Seq("footype\tbartype"))))
      response.status must beEqualTo(BAD_REQUEST)
    }
  }

  "CORSFilter" should {
    import play.api.mvc.WithFilters
    import io.prediction.api.CORSFilter

    "send correct CORS headers for OPTIONS request" in new WithServer {
      val response = Helpers.await(wsUrl(s"/apiurl").withHeaders(
        "Access-Control-Request-Method" -> "POST").options())

      def allowsCorrectMethods(allowedMethodHeader: String): Boolean = {
        val allowedMethods = allowedMethodHeader.split(", ")
        return allowedMethods.sameElements(Array("GET", "POST", "DELETE"))
      }

      response.header("Access-Control-Allow-Origin").nonEmpty must beTrue and
        (response.header("Access-Control-Allow-Methods").exists(allowsCorrectMethods) must beTrue)
    }

    "not allow any origin if setting is empty" in new WithServer() {
      val origin = "http://www.test-domain.com"
      val response = Helpers.await(wsUrl(s"/apiurl").withHeaders("Origin" -> origin).get())
      response.header("Access-Control-Allow-Origin").getOrElse("") mustNotEqual origin
    }

    val appWithOneAllowedDomain = FakeApplication(withGlobal = Some(new WithFilters(CORSFilter(Some("http://www.test-domain.com")))))
    val appWithSeveralAllowedDomains = FakeApplication(
      withGlobal = Some(new WithFilters(CORSFilter(Some("http://www.test-domain.com,http://www.other-domain.com,http://www.abcd.efgh")))))

    "allow specified origin if setting contains one domain" in new WithServer(app = appWithOneAllowedDomain) {
      val origin = "http://www.test-domain.com"
      val response = Helpers.await(wsUrl(s"/apiurl").withHeaders("Origin" -> origin).get())
      response.header("Access-Control-Allow-Origin").getOrElse("") mustEqual origin
    }

    "not allow unspecified origin if setting contains one domain" in new WithServer(app = appWithOneAllowedDomain) {
      val origin = "http://www.other-domain.com"
      val response = Helpers.await(wsUrl(s"/apiurl").withHeaders("Origin" -> origin).get())
      response.header("Access-Control-Allow-Origin").getOrElse("") mustNotEqual origin
    }

    "allow specified origin 1 if setting contains several domains" in new WithServer(app = appWithSeveralAllowedDomains) {
      val origin = "http://www.test-domain.com"
      val response = Helpers.await(wsUrl(s"/apiurl").withHeaders("Origin" -> origin).get())
      response.header("Access-Control-Allow-Origin").getOrElse("") mustEqual origin
    }

    "allow specified origin 2 if setting contains several domains" in new WithServer(app = appWithSeveralAllowedDomains) {
      val origin = "http://www.other-domain.com"
      val response = Helpers.await(wsUrl(s"/apiurl").withHeaders("Origin" -> origin).get())
      response.header("Access-Control-Allow-Origin").getOrElse("") mustEqual origin
    }

    "not allow unspecified origin if setting contains several domains" in new WithServer(app = appWithSeveralAllowedDomains) {
      val origin = "http://www.unallowed-domain.com"
      val response = Helpers.await(wsUrl(s"/apiurl").withHeaders("Origin" -> origin).get())
      response.header("Access-Control-Allow-Origin").getOrElse("") mustNotEqual origin
    }

  }

  step {
    MongoConnection()(config.settingsDbName).dropDatabase()
    MongoConnection()(config.appdataDbName).dropDatabase()
    MongoConnection()(config.modeldataDbName).dropDatabase()
  }
}
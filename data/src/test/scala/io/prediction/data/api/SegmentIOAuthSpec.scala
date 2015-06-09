package io.prediction.data.api

import sun.misc.BASE64Encoder

import akka.actor.ActorRefFactory
import io.prediction.data.storage._
import org.specs2.mutable.Specification
import org.specs2.mutable.After
import spray.http.HttpCharsets._
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

class SegmentIOAuthSpec
  extends Specification
  with Specs2RouteTest
  with HttpService
  with After {

  override def after: Any = system.shutdown()

  override def actorRefFactory: ActorRefFactory = system

  val appId = 0
  val eventClient = Storage.getLEvents()
  val accessKeysClient = new AccessKeys{
    override def insert(k: AccessKey): Option[String] = null
    override def getByAppid(appid: Int): Seq[AccessKey] = null
    override def update(k: AccessKey): Unit = {}
    override def delete(k: String): Unit = {}
    override def getAll(): Seq[AccessKey] = null

    override def get(k: String): Option[AccessKey] =
      Some(AccessKey(k, appId, Seq.empty))
  }

  val dbName = "test_pio_storage_events_" + hashCode
  val channelsClient = Storage.getDataObject[Channels](
    StorageTestUtils.jdbcSourceName,
    dbName
  )

  val base64Encoder = new BASE64Encoder

  "Event Service" should {
    "process SegmentIO identity request properly" in {
      val jsonReq =
        """
          |{
          |  "anonymous_id": "507f191e810c19729de860ea",
          |  "channel": "browser",
          |  "context": {
          |    "ip": "8.8.8.8",
          |    "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36"
          |  },
          |  "message_id": "022bb90c-bbac-11e4-8dfc-aa07a5b093db",
          |  "received_at": "2015-02-23T22:28:55.387Z",
          |  "sent_at": "2015-02-23T22:28:55.111Z",
          |  "traits": {
          |    "name": "Peter Gibbons",
          |    "email": "peter@initech.com",
          |    "plan": "premium",
          |    "logins": 5
          |  },
          |  "type": "identify",
          |  "user_id": "97980cfea0067",
          |  "version": "1.1"Create
          |}
        """.stripMargin

      val accessKey = "abc:"
      val accessKeyEncoded = base64Encoder.encodeBuffer(accessKey.getBytes)
      val route = new EventServiceActor(
        eventClient,
        accessKeysClient,
        channelsClient,
        EventServerConfig()).route

      Post("/webhooks/segmentio.json", jsonReq) ~>
        addHeader("Authorization", accessKeyEncoded) ~>
        addHeader("ContentType", "application/json") ~>
        route ~>
        check {
          println(responseAs[String])
          handled ==== true
        }
    }
  }
}

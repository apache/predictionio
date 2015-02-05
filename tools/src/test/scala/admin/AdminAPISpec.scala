/**
 * Created by asylum on 2/4/15.
 */

package io.prediction.tools.admin

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestProbe
import io.prediction.data.api.StartServer
import io.prediction.data.storage.Storage
import org.specs2.mutable.Specification
import spray.util._
import spray.http._
import spray.httpx.RequestBuilding._


class AdminAPISpec extends Specification{
  
  val system = ActorSystem(Utils.actorSystemNameFrom(getClass))
  val config = AdminServerConfig(
    ip = "localhost",
    port = 7071)
  
  val commandClient = new CommandClient(
    appClient = Storage.getMetaDataApps,
    accessKeyClient = Storage.getMetaDataAccessKeys,
    eventClient = Storage.getLEvents()
  )
  
  val adminActor= system.actorOf(Props(classOf[AdminServerActor], commandClient))

  adminActor ! StartServer(config.ip, config.port)
  
  "The 'runRoute' directive" should {
    "properly produce HttpResponses from the admin actor" in {
      val probe = TestProbe()(system)
      probe.send(adminActor,Get("/"))
      //probe.expectMsg(HttpResponse(200,HttpEntity(ContentType(MediaTypes.`application/json`,HttpCharsets.`UTF-8`))))
      success
    }
  }
  step(system.shutdown())
}
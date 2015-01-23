/** Copyright 2014 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.data.api

import io.prediction.data.storage.Storage
import io.prediction.data.storage.CommandClient

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.io.IO
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout

import org.json4s.DefaultFormats
//import org.json4s.ext.JodaTimeSerializers

import spray.can.Http
import spray.http.HttpCharsets
import spray.http.HttpEntity
import spray.http.HttpResponse
import spray.http.MediaTypes
import spray.http.StatusCodes
import spray.httpx.Json4sSupport
import spray.httpx.unmarshalling.Unmarshaller
import spray.routing._
import spray.routing.authentication.Authentication
import spray.routing.Directives._

import scala.concurrent.Future

import java.util.concurrent.TimeUnit

// see EventAPI.scala for reference
class AdminServiceActor(val commandClient: CommandClient)
  extends HttpServiceActor {

  object Json4sProtocol extends Json4sSupport {
    implicit def json4sFormats = DefaultFormats
    //implicit def json4sFormats: Formats = DefaultFormats.lossless ++
    //  JodaTimeSerializers.all
  }

  import Json4sProtocol._

  val log = Logging(context.system, this)

  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our
  // Futures
  implicit def executionContext = actorRefFactory.dispatcher
  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  // for better message response
  val rejectionHandler = RejectionHandler {
    case MalformedRequestContentRejection(msg, _) :: _ =>
      complete(StatusCodes.BadRequest, Map("message" -> msg))
    case MissingQueryParamRejection(msg) :: _ =>
      complete(StatusCodes.NotFound,
        Map("message" -> s"missing required query parameter ${msg}."))
    case AuthenticationFailedRejection(cause, challengeHeaders) :: _ =>
      complete(StatusCodes.Unauthorized, challengeHeaders,
        Map("message" -> s"Invalid accessKey."))
  }

  val jsonPath = """(.+)\.json$""".r

  val route: Route =
    pathSingleSlash {
      get {
        respondWithMediaType(MediaTypes.`application/json`) {
          complete(Map("status" -> "alive"))
        }
      }
    }

  /*
  val route: Route = ...
    path(“cmd” / “app” ) {
      post {
        entity(as[AppNewRequest]) { req =>
           complete { ...
             commandClient.futureAppNew(req)
           }
      }
    }
  */

  def receive = runRoute(route)

}

class AdminServerActor(val commandClient: CommandClient) extends Actor {
  val log = Logging(context.system, this)
  val child = context.actorOf(
    Props(classOf[AdminServiceActor], commandClient),
    "AdminServiceActor")

  implicit val system = context.system

  def receive = {
    case StartServer(host, portNum) => {
      IO(Http) ! Http.Bind(child, interface = host, port = portNum)
    }
    case m: Http.Bound => log.info("Bound received. AdminServer is ready.")
    case m: Http.CommandFailed => log.error("Command failed.")
    case _ => log.error("Unknown message.")
  }
}

case class AdminServerConfig(
  ip: String = "localhost",
  port: Int = 7071
)

object AdminServer {
  def createAdminServer(config: AdminServerConfig) = {
    implicit val system = ActorSystem("AdminServerSystem")

    val commandClient = new CommandClient(
      appClient = Storage.getMetaDataApps,
      accessKeyClient = Storage.getMetaDataAccessKeys,
      eventClient = Storage.getLEvents()
    )

    val serverActor = system.actorOf(
      Props(classOf[AdminServerActor], commandClient),
      "AdminServerActor")
    serverActor ! StartServer(config.ip, config.port)
    system.awaitTermination
  }
}

object AdminRun {
  def main (args: Array[String]) {
    AdminServer.createAdminServer(AdminServerConfig(
      ip = "localhost",
      port = 7071))
  }
}

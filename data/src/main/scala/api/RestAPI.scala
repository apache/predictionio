package io.prediction.data.api

import akka.actor.{Actor, ActorSystem, Props}
import akka.event.Logging
import akka.io.IO
import io.prediction.data.storage.{EventJson4sSupport, Storage}
import org.json4s.DefaultFormats
//import org.json4s.ext.JodaTimeSerializers

import spray.can.Http
import spray.http.{MediaTypes, StatusCodes}
import spray.httpx.Json4sSupport
import spray.routing._


class RestServiceActor() extends HttpServiceActor {

  object Json4sProtocol extends Json4sSupport {
    implicit def json4sFormats = DefaultFormats +
      new EventJson4sSupport.APISerializer
    //implicit def json4sFormats: Formats = DefaultFormats.lossless ++
    //  JodaTimeSerializers.all
  }

  import Json4sProtocol._

  val log = Logging(context.system, this)

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
    } ~
      path("cmd" / "list") {
        get {
          respondWithMediaType(MediaTypes.`application/json`){
            complete(List("build","train","deploy"))
          }
        }
    } ~
      path("cmd" / "app") {
        get {
          respondWithMediaType(MediaTypes.`application/json`){
            val apps = Storage.getMetaDataApps.getAll().sortBy(_.name)
            val accessKeys = Storage.getMetaDataAccessKeys
            var appsRes = List[AppResponse]()
            apps.map {
              app => {
                AppResponse(app.id,app.name,accessKeys.getByAppid(app.id))
              }
            }
            complete(apps)
          }
        } ~
        delete {
          entity(as[AppRequest]) { appArgs =>
            complete("App deleted successfully")
          }
        } ~
        post {
          entity(as[AppRequest]) { appArgs =>
            complete("App added successfully")
          }
        }
    }
  def receive = runRoute(route)
}

class RestServerActor() extends Actor {
  val log = Logging(context.system, this)
  val child = context.actorOf(
    Props(classOf[RestServiceActor]),
    "RestServiceActor")
  implicit val system = context.system

  def receive = {
    case StartServer(host, portNum) => {
      IO(Http) ! Http.Bind(child, interface = host, port = portNum)
    }
    case m: Http.Bound => log.info("Bound received. RestServer is ready.")
    case m: Http.CommandFailed => log.error("Command failed.")
    case _ => log.error("Unknown message.")
  }
}

case class RestServerConfig(
                             ip: String = "localhost",
                             port: Int = 7071
                             )

object RestServer {
  def createRestServer(config: RestServerConfig) = {
    implicit val system = ActorSystem("RestServerSystem")

    val serverActor = system.actorOf(
      Props(classOf[RestServerActor]),
      "RestServerActor")
    serverActor ! StartServer(config.ip, config.port)
    system.awaitTermination

  }
}

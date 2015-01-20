package io.prediction.data.api

import akka.actor.{Actor, ActorSystem, Props}
import akka.event.Logging
import akka.io.IO
import io.prediction.data.storage.{AccessKey, App, EventJson4sSupport, Storage}

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
      path("cmd" / "app" / Segment / "data") {
        appName => {
          delete {
            respondWithMediaType(MediaTypes.`application/json`) {

              val apps = Storage.getMetaDataApps
              val events = Storage.getLEvents()

              val response = apps.getByName(appName) map { app =>
                val data = if (events.remove(app.id)) {
                  GeneralResponse(1, s"Removed Event Store for this app ID: ${app.id}")
                } else {
                  GeneralResponse(0, s"Error removing Event Store for this app.")
                }

                val dbInit = events.init(app.id)
                val data2 = if (dbInit) {
                  GeneralResponse(1, s"Initialized Event Store for this app ID: ${app.id}.")
                } else {
                  GeneralResponse(0, s"Unable to initialize Event Store for this appId:" +
                    s" ${app.id}.")
                }
                events.close()
                GeneralResponse(data.status + data2.status, data.message + data2.message)
              } getOrElse {
                GeneralResponse(0, s"App ${appName} does not exist.")
              }
              complete(response)
            }
          }
        }
      } ~
      path("cmd" / "app" / Segment) {
        appName => {
          delete {
            respondWithMediaType(MediaTypes.`application/json`) {
              val apps = Storage.getMetaDataApps
              val events = Storage.getLEvents()

              val response = apps.getByName(appName) map { app =>
                val data = if (events.remove(app.id)) {
                  if (Storage.getMetaDataApps.delete(app.id)) {
                    GeneralResponse(1, s"App successfully deleted")
                  } else {
                    GeneralResponse(0, s"Error deleting app ${app.name}.")
                  }
                } else {
                  GeneralResponse(0, s"Error removing Event Store for app ${app.name}.");
                }
                events.close()
                data
              } getOrElse {
                GeneralResponse(0, s"App ${appName} does not exist.")
              }
              complete(response)
            }
          }
        }
      } ~
      path("cmd" / "app") {
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            complete {
              val apps = Storage.getMetaDataApps.getAll().sortBy(_.name)
              val accessKeys = Storage.getMetaDataAccessKeys

              val appsRes = apps.map {
                app => {
                  AppResponse(app.id, app.name, accessKeys.getByAppid(app.id))
                }
              }
              appsRes
            }
          }
        } ~
          post {
            entity(as[AppRequest]) {
              appArgs => respondWithMediaType(MediaTypes.`application/json`) {
                val apps = Storage.getMetaDataApps
                val response = apps.getByName(appArgs.name) map { app =>
                  GeneralResponse(0, s"App ${appArgs.name} already exists. Aborting.")
                } getOrElse {
                    apps.get(appArgs.id) map {
                      app2 =>
                        GeneralResponse(0, s"App ID ${app2.id} already exists and maps to the app '${app2.name}'. " +
                          "Aborting.")
                    } getOrElse{
                      val appid = apps.insert(App(
                        id = Option(appArgs.id).getOrElse(0),
                        name = appArgs.name,
                        description = Option(appArgs.description)))
                      appid map { id =>
                        val events = Storage.getLEvents()
                        val dbInit = events.init(id)
                        val r = if (dbInit) {

                          val accessKeys = Storage.getMetaDataAccessKeys
                          val accessKey = AccessKey(
                            key = "",
                            appid = id,
                            events = Seq())

                          val accessKey2 = accessKeys.insert(AccessKey(
                            key = "",
                            appid = id,
                            events = Seq()))

                          accessKey2 map { k =>
                            AppResponse(id,appArgs.name,Seq[AccessKey](accessKey))
                          } getOrElse {
                            GeneralResponse(0,s"Unable to create new access key.")
                          }
                        } else {
                          GeneralResponse(0,s"Unable to initialize Event Store for this app ID: ${id}.")
                        }
                        events.close()
                        r
                      } getOrElse {
                        GeneralResponse(0,s"Unable to create new app.")
                      }
                    }
                }
                complete(response)
              }
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

object RunRestServer {

  def main (args: Array[String]) {
    RestServer.createRestServer(RestServerConfig(
      ip = "localhost",
      port = 7071))
  }

}

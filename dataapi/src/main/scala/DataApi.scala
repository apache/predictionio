package io.prediction.dataapi

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.io.IO
import akka.event.Logging

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.{ read, write }
import org.json4s.ext.JodaTimeSerializers

import spray.httpx.Json4sSupport
import spray.can.Http
import spray.routing._
import Directives._
import spray.http.MediaTypes

class DataServiceActor extends HttpServiceActor {

  object Json4sProtocol extends Json4sSupport {
    implicit def json4sFormats: Formats = DefaultFormats.lossless ++
      JodaTimeSerializers.all
  }

  import Json4sProtocol._

  val log = Logging(context.system, this)

  val eventClient = StorageClient.eventClient

  val route: Route =
    pathSingleSlash {
      get {
        respondWithMediaType(MediaTypes.`application/json`) {
          complete("status" -> "alive")
        }
      }
    } ~
    path("events" / Segment) { eventId =>
      get {
        respondWithMediaType(MediaTypes.`application/json`) {
          complete {
            log.info("get called.")
            val e = eventClient.get(eventId).get
            e
          }
        }
      } ~
      delete {
        respondWithMediaType(MediaTypes.`application/json`) {
          complete {
            log.info("delete called.")
            val r = eventClient.delete(eventId)
            s"""{"found" : ${r}}"""
          }
        }
      }
    } ~
    path("events") {
      post {
        // TODO: non blocking
        entity(as[Event]) { event =>
          //val event = jsonObj.extract[Event]
          val id = eventClient.insert(event).get
          complete("eventId" -> s"${id}")
        }

      }
    }

  def receive = runRoute(route)

}

/* message */
case class StartServer(
  val host: String,
  val port: Int
)

class DataServerActor extends Actor {
  val log = Logging(context.system, this)
  val child = context.actorOf(Props[DataServiceActor], "DataServiceActor")
  implicit val system = context.system

  def receive = {
    case StartServer(host, portNum) => {
      IO(Http) ! Http.Bind(child, interface = host, port = portNum)
    }
    case m: Http.Bound => log.info("Bound received.")
    case m: Http.CommandFailed => log.error("Command failed.")
    case _ => log.error("Unknown message.")
  }
}


object Run {

  def main (args: Array[String]) {
    implicit val system = ActorSystem("DataAPISystem")
    val serverActor = system.actorOf(Props[DataServerActor], "DataServerActor")
    serverActor ! StartServer("localhost", 8081)

    println("[ Hit any key to exit. ]")
    val result = readLine()
    system.shutdown()
  }

}
